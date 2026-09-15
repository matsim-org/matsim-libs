package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;
import java.util.List;

final class SimpleRailsimAgent implements RailsimTransitDriverAgent {

	private final Id<Person> id;
	private final TransitLine line;
	private final TransitRoute route;
	private final Departure departure;
	private final NetworkRoute networkRoute;
	private final Vehicle vehicle;

	private int stopIndex;
	private int stopped = 0;

	public SimpleRailsimAgent(TransitLine line, TransitRoute route, Departure departure,
							  int stopIndex, NetworkRoute networkRoute, Vehicle vehicle) {
		this.line = line;
		this.route = route;
		this.departure = departure;
		this.stopIndex = stopIndex;
		this.networkRoute = networkRoute;
		this.vehicle = vehicle;
		this.id = Id.createPersonId(vehicle.getId().toString());
	}

	@Override
	public TransitStopFacility getNextTransitStop() {
		// The agents only drives between two stops, after that it can return null
		if (stopped > 2)
			return null;

		return stopIndex < route.getStops().size() ? route.getStops().get(stopIndex).getStopFacility() : null;
	}

	@Override
	public double handleTransitStop(TransitStopFacility stop, double now) {
		stopIndex++;
		stopped++;
		return 0;
	}

	@Override
	public TransitStopFacility addDetour(List<RailLink> original, List<RailLink> detour) {
		return null;
	}

	@Override
	public TransitLine getTransitLine() {
		return line;
	}

	@Override
	public TransitRoute getTransitRoute() {
		return route;
	}

	@Override
	public Departure getDeparture() {
		return departure;
	}

	@Override
	public int getCurrentStopIndex() {
		return stopIndex;
	}

	@Override
	public PlanElement getCurrentPlanElement() {
		return new SimpleLeg();
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		return networkRoute.getStartLinkId();
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {

	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		return false;
	}

	@Override
	public State getState() {
		return State.LEG;
	}

	@Override
	public double getActivityEndTime() {
		// Needs to be NaN to indicate that there is no follow-up activity
		return Double.NaN;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {

	}

	@Override
	public void endLegAndComputeNextState(double now) {

	}

	@Override
	public void setStateToAbort(double now) {

	}

	@Override
	public OptionalTime getExpectedTravelTime() {
		return null;
	}

	@Override
	public Double getExpectedTravelDistance() {
		return 0.0;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {

	}

	@Override
	public Facility getCurrentFacility() {
		return null;
	}

	@Override
	public Facility getDestinationFacility() {
		return null;
	}

	@Override
	public Id<Person> getId() {
		return id;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return null;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return null;
	}

	@Override
	public String getMode() {
		return "";
	}

	@Override
	public MobsimVehicle getVehicle() {
		return new SimpleVehicle();
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return vehicle.getId();
	}

	private final class SimpleVehicle implements MobsimVehicle {

		@Override
		public Id<Link> getCurrentLinkId() {
			return null;
		}

		@Override
		public Vehicle getVehicle() {
			return vehicle;
		}

		@Override
		public MobsimDriverAgent getDriver() {
			return SimpleRailsimAgent.this;
		}

		@Override
		public double getSizeInEquivalents() {
			return 0;
		}

		@Override
		public boolean addPassenger(PassengerAgent passenger) {
			return false;
		}

		@Override
		public boolean removePassenger(PassengerAgent passenger) {
			return false;
		}

		@Override
		public Collection<? extends PassengerAgent> getPassengers() {
			return List.of();
		}

		@Override
		public int getPassengerCapacity() {
			return 0;
		}

		@Override
		public Id<Vehicle> getId() {
			return vehicle.getId();
		}
	}

	private final class SimpleLeg implements Leg {

		@Override
		public String getMode() {
			return "";
		}

		@Override
		public void setMode(String mode) {

		}

		@Override
		public String getRoutingMode() {
			return "";
		}

		@Override
		public void setRoutingMode(String routingMode) {

		}

		@Override
		public Route getRoute() {
			return networkRoute;
		}

		@Override
		public void setRoute(Route route) {

		}

		@Override
		public OptionalTime getDepartureTime() {
			return null;
		}

		@Override
		public void setDepartureTime(double seconds) {

		}

		@Override
		public void setDepartureTimeUndefined() {

		}

		@Override
		public OptionalTime getTravelTime() {
			return null;
		}

		@Override
		public void setTravelTime(double seconds) {

		}

		@Override
		public void setTravelTimeUndefined() {

		}

		@Override
		public Attributes getAttributes() {
			return null;
		}
	}
}
