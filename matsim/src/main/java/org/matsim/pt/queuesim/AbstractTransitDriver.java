package org.matsim.pt.queuesim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.PersonEntersVehicleEventImpl;
import org.matsim.core.events.PersonLeavesVehicleEventImpl;
import org.matsim.core.events.VehicleArrivesAtFacilityEventImpl;
import org.matsim.core.events.VehicleDepartsAtFacilityEventImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.PersonAgent;
import org.matsim.ptproject.qsim.QueueSimulation;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;

public abstract class AbstractTransitDriver implements TransitDriverAgent {
	
	private static final Logger log = Logger.getLogger(AbstractTransitDriver.class);

	private TransitVehicle vehicle = null;
	private int nextLinkIndex = 0;
	private final TransitStopAgentTracker agentTracker;
	private final Person dummyPerson;
	private final QueueSimulation sim;
	private TransitRouteStop nextStop;
	private TransitStopFacility lastHandledStop = null;
	private ListIterator<TransitRouteStop> stopIterator;

	public abstract void legEnds(final double now);
	public abstract NetworkRouteWRefs getCarRoute();
	public abstract TransitLine getTransitLine();
	public abstract TransitRoute getTransitRoute();
	public abstract double getDepartureTime();

	public AbstractTransitDriver(Person personImpl, QueueSimulation sim, TransitStopAgentTracker agentTracker2) {
		super();
		this.dummyPerson = personImpl;
		this.sim = sim;
		this.agentTracker = agentTracker2;
	}

	protected void init() {
		if (getTransitRoute() != null) {
			this.stopIterator = getTransitRoute().getStops().listIterator();
			this.nextStop = (stopIterator.hasNext() ? stopIterator.next() : null);
		} else {
			this.nextStop = null;
		}
		this.nextLinkIndex = 0;
	}
	
	@Override
	public Id chooseNextLinkId() {
		if (this.nextLinkIndex < getCarRoute().getLinkIds().size()) {
			return getCarRoute().getLinkIds().get(this.nextLinkIndex);
		}
		if (this.nextLinkIndex == getCarRoute().getLinkIds().size()) {
			return getCarRoute().getEndLinkId();
		}
		return null;
	}

	@Override
	public void moveOverNode() {
		this.nextLinkIndex++;
	}

	@Override
	public TransitStopFacility getNextTransitStop() {
		if (this.nextStop == null) {
			return null;
		}
		return this.nextStop.getStopFacility();
	}

	@Override
	public double handleTransitStop(final TransitStopFacility stop, final double now) {
		assertExpectedStop(stop);
		processEventVehicleArrives(stop, now);
		ArrayList<PassengerAgent> passengersLeaving = findPassengersLeaving(stop);	
		int freeCapacity = this.vehicle.getPassengerCapacity() - this.vehicle.getPassengers().size() + passengersLeaving.size();
		List<PassengerAgent> passengersEntering = findPassengersEntering(stop, freeCapacity);
		double stopTime = handlePassengersAndCalculateStopTime(stop, now, passengersLeaving, passengersEntering);
		stopTime = longerStopTimeIfWeAreAheadOfSchedule(now, stopTime);
		if (stopTime == 0.0) {
			depart(now);
		}
		return stopTime;
	}

	@Override
	public void activityEnds(final double now) {
		this.sim.agentDeparts(now, this, this.getCurrentLeg().getRoute().getStartLinkId());
	}
	
	@Override
	public void teleportToLink(final Id linkId) {
	}
	
	QueueSimulation getSimulation(){
		return this.sim;
	}
	
	@Override
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
		EventsManager events = QueueSimulation.getEvents();
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
		EventsManager events = QueueSimulation.getEvents();
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
			List<PassengerAgent> passengersEntering) {
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
			final double now, List<PassengerAgent> passengersEntering) {
		EventsManager events = QueueSimulation.getEvents();
		for (PassengerAgent passenger : passengersEntering) {
			this.agentTracker.removeAgentFromStop(passenger, stop);
			this.vehicle.addPassenger(passenger);
			DriverAgent agent = (DriverAgent) passenger;
			events.processEvent(new PersonEntersVehicleEventImpl(now, agent.getPerson().getId(), this.vehicle.getBasicVehicle(), this.getTransitRoute().getId()));
		}
	}

	private void handlePassengersLeaving(final TransitStopFacility stop,
			final double now, ArrayList<PassengerAgent> passengersLeaving) {
		EventsManager events = QueueSimulation.getEvents();
		for (PassengerAgent passenger : passengersLeaving) {
			this.vehicle.removePassenger(passenger);
			DriverAgent agent = (DriverAgent) passenger;
			events.processEvent(new PersonLeavesVehicleEventImpl(now, agent.getPerson().getId(), this.vehicle.getBasicVehicle().getId(), this.getTransitRoute().getId()));
			agent.teleportToLink(stop.getLinkId());
//			events.processEvent(new AgentArrivalEventImpl(now, agent.getPerson(),
//					stop.getLink(), agent.getCurrentLeg()));
			agent.legEnds(now);
		}
	}

	private List<PassengerAgent> findPassengersEntering(
			final TransitStopFacility stop, int freeCapacity) {
		ArrayList<PassengerAgent> passengersEntering = new ArrayList<PassengerAgent>();
		for (PassengerAgent agent : this.agentTracker.getAgentsAtStop(stop)) {
			if (freeCapacity == 0) {
				break;
			}
			List<TransitRouteStop> stops = getTransitRoute().getStops();
			List<TransitRouteStop> stopsToCome = stops.subList(stopIterator.nextIndex(), stops.size());
			if (agent.getEnterTransitRoute(getTransitLine(), getTransitRoute(), stopsToCome)) {
				passengersEntering.add(agent);
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

		@Override
		public List<Id> getLinkIds() {
			return this.delegate.getLinkIds();
		}

		@Deprecated
		@Override
		public List<Link> getLinks() {
			return this.delegate.getLinks();
		}

		@Deprecated
		@Override
		public List<Node> getNodes() {
			return this.delegate.getNodes();
		}

		@Deprecated
		@Override
		public Link getStartLink() {
			return this.delegate.getStartLink();
		}

		@Override
		public NetworkRouteWRefs getSubRoute(final Node fromNode, final Node toNode) {
			return this.delegate.getSubRoute(fromNode, toNode);
		}

		@Override
		public double getTravelCost() {
			return this.delegate.getTravelCost();
		}

		@Override
		public Id getVehicleId() {
			return AbstractTransitDriver.this.vehicle.getBasicVehicle().getId();
		}

		@Override
		public void setLinks(final Link startLink, final List<Link> srcRoute, final Link endLink) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Deprecated
		@Override
		public void setNodes(final List<Node> srcRoute) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setNodes(final Link startLink, final List<Node> srcRoute, final Link endLink) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setTravelCost(final double travelCost) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setVehicleId(final Id vehicleId) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Deprecated
		@Override
		public Link getEndLink() {
			return this.delegate.getEndLink();
		}

		@Override
		public void setEndLink(final Link link) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public void setStartLink(final Link link) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
		public double getDistance() {
			return this.delegate.getDistance();
		}

		@Override
		public Id getEndLinkId() {
			return this.delegate.getEndLinkId();
		}

		@Override
		public Id getStartLinkId() {
			return this.delegate.getStartLinkId();
		}

		@Deprecated
		@Override
		public double getTravelTime() {
			return this.delegate.getTravelTime();
		}

		@Override
		public void setDistance(final double distance) {
			throw new UnsupportedOperationException("read only route.");
		}

		@Override
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