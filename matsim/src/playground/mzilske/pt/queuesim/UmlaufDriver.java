package playground.mzilske.pt.queuesim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.PersonEntersVehicleEventImpl;
import org.matsim.core.events.PersonLeavesVehicleEventImpl;
import org.matsim.core.events.VehicleArrivesAtFacilityEventImpl;
import org.matsim.core.events.VehicleDepartsAtFacilityEventImpl;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.Simulation;
import org.matsim.core.mobsim.queuesim.TransitDriverAgent;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.queuesim.PassengerAgent;
import org.matsim.pt.queuesim.TransitDriver;
import org.matsim.pt.queuesim.TransitStopAgentTracker;
import org.matsim.pt.queuesim.TransitVehicle;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;


public class UmlaufDriver implements TransitDriverAgent {

	private static final Logger log = Logger.getLogger(TransitDriver.class);
	
	private final Umlauf umlauf;
	private Iterator<UmlaufStueckI> iUmlaufStueck;
	
	private List<Link> linkRoute;
	private NetworkRouteWRefs carRoute;
	private double departureTime;

	/*package*/ TransitVehicle vehicle = null;

	private int nextLinkIndex = 0;
	private final TransitQueueSimulation sim;
	private final TransitStopAgentTracker agentTracker;

	private final LegImpl currentLeg = new LegImpl(TransportMode.car);
	private final PersonImpl dummyPerson;

	private Iterator<TransitRouteStop> stopIterator;
	private TransitRouteStop nextStop;
	private TransitStopFacility lastHandledStop = null;

	private TransitLine transitLine;
	private TransitRoute transitRoute;

	public UmlaufDriver(Umlauf umlauf,
			TransitStopAgentTracker thisAgentTracker,
			TransitQueueSimulation transitQueueSimulation) {
		this.umlauf = umlauf;
		this.agentTracker = thisAgentTracker;
		this.sim = transitQueueSimulation;
		this.dummyPerson = new PersonImpl(new IdImpl("ptDrvr_Uml_" + umlauf.getId()));
		this.iUmlaufStueck = this.umlauf.getUmlaufStuecke().iterator();
		setNextLeg();
	}

	private void setNextLeg() {
		UmlaufStueckI umlaufStueck = this.iUmlaufStueck.next();
		if (umlaufStueck.isFahrt()) {
			setLeg(umlaufStueck.getLine(), umlaufStueck.getRoute(), umlaufStueck.getDeparture());
		} else {
			setWenden(umlaufStueck.getCarRoute());
		}
	}
	
	private void setWenden(NetworkRouteWRefs carRoute) {
		this.transitLine = null;
		this.transitRoute = null;
		this.stopIterator = null;
		this.nextStop = null;
		setCarRoute(carRoute);
	}

	private void setLeg(final TransitLine line, final TransitRoute route,
			final Departure departure) {
		this.transitLine = line;
		this.transitRoute = route;
		this.stopIterator = route.getStops().iterator();
		this.nextStop = (this.stopIterator.hasNext() ? this.stopIterator.next() : null);
		this.departureTime = departure.getDepartureTime();
		setCarRoute(route.getRoute());
	}

	private void setCarRoute(NetworkRouteWRefs carRoute) {
		this.carRoute = carRoute;
		this.linkRoute = this.carRoute.getLinks();
		this.currentLeg.setRoute(new NetworkRouteWrapper(this.carRoute)); // we use the non-wrapped route for efficiency, but the leg has to return the wrapped one.
		this.nextLinkIndex = 0;
	}

	public void setVehicle(final TransitVehicle vehicle) {
		this.vehicle = vehicle;
	}

	public Link chooseNextLink() {
		if (this.nextLinkIndex < this.linkRoute.size()) {
			return this.linkRoute.get(this.nextLinkIndex);
		}
		if (this.nextLinkIndex == this.linkRoute.size()) {
			return this.carRoute.getEndLink();
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
		if (stop != this.nextStop.getStopFacility()) {
			throw new RuntimeException("Expected different stop.");
		}

		EventsManager events = TransitQueueSimulation.getEvents();
		if (this.lastHandledStop != stop) {
			events.processEvent(new VehicleArrivesAtFacilityEventImpl(now, this.vehicle.getBasicVehicle().getId(), stop.getId()));
		}

		int freeCapacity = this.vehicle.getPassengerCapacity() - this.vehicle.getPassengers().size();
		// find out who wants to get out
		ArrayList<PassengerAgent> passengersLeaving = new ArrayList<PassengerAgent>();
		for (PassengerAgent passenger : this.vehicle.getPassengers()) {
			if (passenger.getExitAtStop(stop)) {
				passengersLeaving.add(passenger);
			}
		}
		freeCapacity += passengersLeaving.size();
		// find out who wants to get in
		ArrayList<PassengerAgent> passengersEntering = new ArrayList<PassengerAgent>();
		for (PassengerAgent agent : this.agentTracker.getAgentsAtStop(stop)) {
			if (freeCapacity == 0) {
				break;
			}
			PassengerAgent passenger = agent;
			if (passenger.getEnterTransitRoute(this.transitLine, this.transitRoute)) {
				passengersEntering.add(passenger);
				freeCapacity--;
			}
		}

		// do the handling
		double stopTime = 0.0;
		int cntEgress = passengersLeaving.size();
		int cntAccess = passengersEntering.size();
		if ((cntAccess > 0) || (cntEgress > 0)) {
			stopTime = cntAccess * 4 + cntEgress * 2;
			if (this.lastHandledStop != stop) {
				stopTime += 15.0; // add fixed amount of time for door-operations and similar stuff
			}

			for (PassengerAgent passenger : passengersLeaving) {
				this.vehicle.removePassenger(passenger);
				DriverAgent agent = (DriverAgent) passenger;
				events.processEvent(new PersonLeavesVehicleEventImpl(now, agent.getPerson(), this.vehicle.getBasicVehicle()));
				agent.teleportToLink(stop.getLink());
				events.processEvent(new AgentArrivalEventImpl(now, agent.getPerson(),
						stop.getLink(), agent.getCurrentLeg()));
				agent.legEnds(now);
			}

			for (PassengerAgent passenger : passengersEntering) {
				this.agentTracker.removeAgentFromStop(passenger, stop);
				this.vehicle.addPassenger(passenger);
				DriverAgent agent = (DriverAgent) passenger;
				events.processEvent(new PersonEntersVehicleEventImpl(now, agent.getPerson(), this.vehicle.getBasicVehicle()));
			}

		}
		this.lastHandledStop = stop;

		if ((this.nextStop.isAwaitDepartureTime()) && (this.nextStop.getDepartureOffset() != Time.UNDEFINED_TIME)) {
			double earliestDepTime = this.departureTime + this.nextStop.getDepartureOffset();
			if (now + stopTime < earliestDepTime) {
				stopTime = earliestDepTime - now;
			}
		}

		if (stopTime == 0.0) {
			events.processEvent(new VehicleDepartsAtFacilityEventImpl(now, this.vehicle.getBasicVehicle().getId(), stop.getId()));
			if (this.stopIterator.hasNext()) {
				this.nextStop = this.stopIterator.next();
			} else {
				this.nextStop = null;
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
		}
		return stopTime;
	}

	public double getDepartureTime() {
		return this.departureTime;
	}

	public void activityEnds(final double now) {
		this.sim.agentDeparts(this, this.currentLeg.getRoute().getStartLink());
	}

	public LegImpl getCurrentLeg() {
		return this.currentLeg;
	}

	public Link getDestinationLink() {
		return this.currentLeg.getRoute().getEndLink();
	}

	public PersonImpl getPerson() {
		return this.dummyPerson;
	}

	public void legEnds(final double now) {
		if (this.iUmlaufStueck.hasNext()) {
			this.setNextLeg();
			if (this.departureTime < now) {
				this.departureTime = now;
			}
			this.sim.scheduleActivityEnd(this);
		} else {
			Simulation.decLiving();	
		}
	}

	public void teleportToLink(final Link link) {
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
			return UmlaufDriver.this.vehicle.getBasicVehicle().getId();
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
