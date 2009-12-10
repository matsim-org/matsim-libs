package org.matsim.pt.queuesim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.PersonEntersVehicleEventImpl;
import org.matsim.core.events.PersonLeavesVehicleEventImpl;
import org.matsim.core.events.VehicleArrivesAtFacilityEventImpl;
import org.matsim.core.events.VehicleDepartsAtFacilityEventImpl;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.TransitDriverAgent;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;

public abstract class AbstractTransitDriver implements TransitDriverAgent {
	
	private static final Logger log = Logger.getLogger(TransitDriver.class);

	private TransitVehicle vehicle = null;
	private int nextLinkIndex = 0;
	private final TransitStopAgentTracker agentTracker;
	private final Person dummyPerson;
	private final TransitQueueSimulation sim;
	private TransitRouteStop nextStop;
	private TransitStopFacility lastHandledStop = null;
	private Iterator<TransitRouteStop> stopIterator;

	public abstract void legEnds(final double now);
	public abstract NetworkRouteWRefs getCarRoute();
	public abstract TransitLine getTransitLine();
	public abstract TransitRoute getTransitRoute();
	public abstract double getDepartureTime();

	public AbstractTransitDriver(Person personImpl, TransitQueueSimulation sim, TransitStopAgentTracker agentTracker2) {
		super();
		this.dummyPerson = personImpl;
		this.sim = sim;
		this.agentTracker = agentTracker2;
	}

	protected void init() {
		if (getTransitRoute() != null) {
			this.stopIterator = getTransitRoute().getStops().iterator();
			this.nextStop = (stopIterator.hasNext() ? stopIterator.next() : null);
		} else {
			this.nextStop = null;
		}
		this.nextLinkIndex = 0;
	}
	
	public Link chooseNextLink() {
		if (this.nextLinkIndex < getCarRoute().getLinks().size()) {
			return getCarRoute().getLinks().get(this.nextLinkIndex);
		}
		if (this.nextLinkIndex == getCarRoute().getLinks().size()) {
			return getCarRoute().getEndLink();
		}
		return null;
	}

	public void moveOverNode() {
		this.nextLinkIndex++;
	}

	public TransitStopFacility getNextTransitStop() {
		if (this.nextStop == null) {
			return null;
		}
		return this.nextStop.getStopFacility();
	}

	public double handleTransitStop(final TransitStopFacility stop, final double now) {
		assertExpectedStop(stop);
		processEventVehicleArrives(stop, now);
		ArrayList<PassengerAgent> passengersLeaving = findPassengersLeaving(stop);	
		int freeCapacity = this.vehicle.getPassengerCapacity() - this.vehicle.getPassengers().size() + passengersLeaving.size();
		ArrayList<PassengerAgent> passengersEntering = findPassengersEntering(stop, freeCapacity);
		double stopTime = handlePassengersAndCalculateStopTime(stop, now, passengersLeaving, passengersEntering);
		stopTime = longerStopTimeIfWeAreAheadOfSchedule(now, stopTime);
		if (stopTime == 0.0) {
			depart(now);
		}
		return stopTime;
	}

	public void activityEnds(final double now) {
		this.sim.agentDeparts(now, this, ((LegImpl) this.getCurrentLeg()).getRoute().getStartLink());
	}
	
	public void teleportToLink(final Link link) {
	}
	
	TransitQueueSimulation getSimulation(){
		return this.sim;
	}
	
	public Person getPerson() {
		return this.dummyPerson;
	}
	
	public TransitVehicle getVehicle() {
		return this.vehicle;
	}
	
	public void setVehicle(final TransitVehicle vehicle) {
		this.vehicle = vehicle;
	}
	
	private void processEventVehicleArrives(final TransitStopFacility stop,
			final double now) {
		EventsManager events = TransitQueueSimulation.getEvents();
		if (this.lastHandledStop != stop) {
			events.processEvent(new VehicleArrivesAtFacilityEventImpl(now, this.vehicle.getBasicVehicle().getId(), stop.getId()));
		}
	}

	private void assertExpectedStop(final TransitStopFacility stop) {
		if (stop != this.nextStop.getStopFacility()) {
			throw new RuntimeException("Expected different stop.");
		}
	}

	private double longerStopTimeIfWeAreAheadOfSchedule(final double now,
			double stopTime) {
		if ((this.nextStop.isAwaitDepartureTime()) && (this.nextStop.getDepartureOffset() != Time.UNDEFINED_TIME)) {
			double earliestDepTime = getDepartureTime() + this.nextStop.getDepartureOffset();
			if (now + stopTime < earliestDepTime) {
				stopTime = earliestDepTime - now;
			}
		}
		return stopTime;
	}

	private void depart(final double now) {
		EventsManager events = TransitQueueSimulation.getEvents();
		events.processEvent(new VehicleDepartsAtFacilityEventImpl(now, this.vehicle.getBasicVehicle().getId(), this.lastHandledStop.getId()));
		this.nextStop = (stopIterator.hasNext() ? stopIterator.next() : null);
		if(this.nextStop == null) {
			assertVehicleIsEmpty();
		}
	}

	private void assertVehicleIsEmpty() {
		if (this.vehicle.getPassengers().size() > 0) {
			RuntimeException e = new RuntimeException("Transit vehicle is at last stop but still contains passengers that did not leave the vehicle!");
			log.error("Transit vehicle must be empty after last stop! vehicle-id = " + this.vehicle.getBasicVehicle().getId(), e);
			for (PassengerAgent agent : this.vehicle.getPassengers()) {
				if (agent instanceof PersonAgent) {
				log.error("Agent is still in transit vehicle: agent-id = " + ((PersonAgent) agent).getPerson().getId());
				}
			}
			throw e;
		}
	}

	private double handlePassengersAndCalculateStopTime(
			final TransitStopFacility stop, final double now,
			ArrayList<PassengerAgent> passengersLeaving,
			ArrayList<PassengerAgent> passengersEntering) {
		double stopTime = 0.0;
		int cntEgress = passengersLeaving.size();
		int cntAccess = passengersEntering.size();
		if ((cntAccess > 0) || (cntEgress > 0)) {
			stopTime = cntAccess * 4 + cntEgress * 2;
			if (this.lastHandledStop != stop) {
				stopTime += 15.0; // add fixed amount of time for door-operations and similar stuff
			}
			handlePassengersLeaving(stop, now, passengersLeaving);
			handlePassengersEntering(stop, now, passengersEntering);
		}
		this.lastHandledStop = stop;
		return stopTime;
	}

	private void handlePassengersEntering(final TransitStopFacility stop,
			final double now, ArrayList<PassengerAgent> passengersEntering) {
		EventsManager events = TransitQueueSimulation.getEvents();
		for (PassengerAgent passenger : passengersEntering) {
			this.agentTracker.removeAgentFromStop(passenger, stop);
			this.vehicle.addPassenger(passenger);
			DriverAgent agent = (DriverAgent) passenger;
			events.processEvent(new PersonEntersVehicleEventImpl(now, agent.getPerson(), this.vehicle.getBasicVehicle(), this.getTransitRoute().getId()));
		}
	}

	private void handlePassengersLeaving(final TransitStopFacility stop,
			final double now, ArrayList<PassengerAgent> passengersLeaving) {
		EventsManager events = TransitQueueSimulation.getEvents();
		for (PassengerAgent passenger : passengersLeaving) {
			this.vehicle.removePassenger(passenger);
			DriverAgent agent = (DriverAgent) passenger;
			events.processEvent(new PersonLeavesVehicleEventImpl(now, agent.getPerson(), this.vehicle.getBasicVehicle(), this.getTransitRoute().getId()));
			agent.teleportToLink(stop.getLink());
//			events.processEvent(new AgentArrivalEventImpl(now, agent.getPerson(),
//					stop.getLink(), agent.getCurrentLeg()));
			agent.legEnds(now);
		}
	}

	private ArrayList<PassengerAgent> findPassengersEntering(
			final TransitStopFacility stop, int freeCapacity) {
		ArrayList<PassengerAgent> passengersEntering = new ArrayList<PassengerAgent>();
		
		for (PassengerAgent agent : this.agentTracker.getAgentsAtStop(stop)) {
			if (freeCapacity == 0) {
				break;
			}
			PassengerAgent passenger = agent;
			if (passenger.getEnterTransitRoute(getTransitLine(), getTransitRoute())) {
				passengersEntering.add(passenger);
				freeCapacity--;
			}
		}
		return passengersEntering;
	}

	private ArrayList<PassengerAgent> findPassengersLeaving(
			final TransitStopFacility stop) {
		ArrayList<PassengerAgent> passengersLeaving = new ArrayList<PassengerAgent>();
		for (PassengerAgent passenger : this.vehicle.getPassengers()) {
			if (passenger.getExitAtStop(stop)) {
				passengersLeaving.add(passenger);
			}
		}
		return passengersLeaving;
	}

	protected NetworkRouteWrapper getWrappedCarRoute() {
		return new NetworkRouteWrapper(getCarRoute());
	}
	
	/**
	 * A simple wrapper that delegates all get-Methods to another instance, blocks set-methods
	 * so this will be read-only, and returns in getVehicleId() a vehicle-Id specific to this driver,
	 * and not to the NetworkRoute. This allows to share one NetworkRoute from a TransitRoute with
	 * multiple transit drivers, thus saving memory.
	 *
	 * @author mrieser
	 */
	protected class NetworkRouteWrapper implements NetworkRouteWRefs, Cloneable {

		private static final long serialVersionUID = 1L;
		private final NetworkRouteWRefs delegate;

		public NetworkRouteWrapper(final NetworkRouteWRefs route) {
			this.delegate = route;
		}

		public List<Id> getLinkIds() {
			return this.delegate.getLinkIds();
		}

		public List<Link> getLinks() {
			return this.delegate.getLinks();
		}

		public List<Node> getNodes() {
			return this.delegate.getNodes();
		}

		public Link getStartLink() {
			return this.delegate.getStartLink();
		}

		public NetworkRouteWRefs getSubRoute(final Node fromNode, final Node toNode) {
			return this.delegate.getSubRoute(fromNode, toNode);
		}

		public double getTravelCost() {
			return this.delegate.getTravelCost();
		}

		public Id getVehicleId() {
			return AbstractTransitDriver.this.vehicle.getBasicVehicle().getId();
		}

		public void setLinks(final Link startLink, final List<Link> srcRoute, final Link endLink) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Deprecated
		public void setNodes(final List<Node> srcRoute) {
			throw new UnsupportedOperationException("read only route.");
		}

		public void setNodes(final Link startLink, final List<Node> srcRoute, final Link endLink) {
			throw new UnsupportedOperationException("read only route.");
		}

		public void setTravelCost(final double travelCost) {
			throw new UnsupportedOperationException("read only route.");
		}

		public void setVehicleId(final Id vehicleId) {
			throw new UnsupportedOperationException("read only route.");
		}

		public Link getEndLink() {
			return this.delegate.getEndLink();
		}

		public void setEndLink(final Link link) {
			throw new UnsupportedOperationException("read only route.");
		}

		public void setStartLink(final Link link) {
			throw new UnsupportedOperationException("read only route.");
		}

		public double getDistance() {
			return this.delegate.getDistance();
		}

		public Id getEndLinkId() {
			return this.delegate.getEndLinkId();
		}

		public Id getStartLinkId() {
			return this.delegate.getStartLinkId();
		}

		public double getTravelTime() {
			return this.delegate.getTravelTime();
		}

		public void setDistance(final double distance) {
			throw new UnsupportedOperationException("read only route.");
		}

		public void setTravelTime(final double travelTime) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public NetworkRouteWrapper clone() {
			try {
				return (NetworkRouteWrapper) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new AssertionError(e);
			}
		}
	}
	

}