package ch.sbb.matsim.contrib.railsim.qsimengine;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import jakarta.annotation.Nullable;

/**
 * Simple train state that can be used without simulation and exact positions on track.
 */
final class SimpleTrainState implements TrainPosition {

	private final DriverAgent driver = new DriverAgent();
	private final Vehicle vehicle;

	/**
	 * The transit route.
	 */
	private final TransitRoute route;

	/**
	 * All links of this transit route.
	 */
	private final List<RailLink> links;

	/**
	 * Current index of the route.
	 */
	private int routeIdx = 0;

	/**
	 * Current stop index.
	 */
	private int stopIdx = 0;

	public SimpleTrainState(Vehicle vehicle, TransitRoute route, List<RailLink> links) {
		this.vehicle = vehicle;
		this.route = route;
		this.links = links;
	}

	void nextLink() {
		routeIdx++;
	}

	void nextStop() {
		stopIdx++;
	}

	@Override
	public MobsimDriverAgent getDriver() {
		return driver;
	}

	@Nullable
	@Override
	public RailsimTransitDriverAgent getPt() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public TrainInfo getTrain() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Nullable
	@Override
	public Id<Link> getHeadLink() {
		return null;
	}

	@Nullable
	@Override
	public Id<Link> getTailLink() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public double getHeadPosition() {
		return 0;
	}

	@Override
	public double getTailPosition() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public double getDelay() {
		return 0;
	}

	@Override
	public int getRouteIndex() {
		return routeIdx;
	}

	@Override
	public int getRouteSize() {
		return links.size();
	}

	@Override
	public RailLink getRoute(int idx) {
		return links.get(idx);
	}

	@Override
	public List<RailLink> getRoute(int from, int to) {
		return links.subList(from, to);
	}

	@Override
	public List<RailLink> getRouteUntilNextStop() {
		int from = routeIdx;

		for (int i = 0; i < links.size(); i++) {
			if (isStop(links.get(i).getLinkId()) && from <= i + 1) {
				return links.subList(from, i + 1);
			}
		}
		return links.subList(from, links.size());
	}

	@Override
	public boolean isStop(Id<Link> link) {
		TransitStopFacility nextStop = driver.getNextTransitStop();
		return nextStop != null && nextStop.getLinkId().equals(link);
	}

	@Nullable
	@Override
	public TransitStopFacility getNextStop() {
		return stopIdx < route.getStops().size() ? route.getStops().get(stopIdx).getStopFacility() : null;
	}

	private final class DriverAgent implements TransitDriverAgent {

		@Override
		public TransitStopFacility getNextTransitStop() {
			return getNextStop();
		}

		@Override
		public double handleTransitStop(TransitStopFacility stop, double now) {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public Id<Link> chooseNextLinkId() {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public void notifyMoveOverNode(Id<Link> newLinkId) {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public boolean isWantingToArriveOnCurrentLink() {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public State getState() {
			return State.LEG;
		}

		@Override
		public double getActivityEndTime() {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public void endActivityAndComputeNextState(double now) {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public void endLegAndComputeNextState(double now) {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public void setStateToAbort(double now) {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public OptionalTime getExpectedTravelTime() {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public Double getExpectedTravelDistance() {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public Facility getCurrentFacility() {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public Facility getDestinationFacility() {
			throw new UnsupportedOperationException("Not supported.");
		}

		@Override
		public Id<Person> getId() {
			return Id.createPersonId(0);
		}

		@Override
		public Id<Link> getCurrentLinkId() {
			return getHeadLink();
		}

		@Override
		public Id<Link> getDestinationLinkId() {
			return links.getLast().getLinkId();
		}

		@Override
		public String getMode() {
			return "rail";
		}


		@Override
		public void setVehicle(MobsimVehicle veh) {
			// no-op for simple offline calculations
		}

		@Override
		public MobsimVehicle getVehicle() {
			return new SimpleMobsimVehicle(vehicle, this);
		}

		@Override
		public Id<Vehicle> getPlannedVehicleId() {
			return vehicle.getId();
		}
	}

	private static final class SimpleMobsimVehicle implements MobsimVehicle {

		private final Vehicle vehicle;
		private final MobsimDriverAgent driver;

		SimpleMobsimVehicle(Vehicle vehicle, MobsimDriverAgent driver) {
			this.vehicle = vehicle;
			this.driver = driver;
		}

		@Override
		public Id<Link> getCurrentLinkId() {
			return null;
		}

		@Override
		public double getSizeInEquivalents() {
			return 1.0;
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
		public java.util.Collection<? extends PassengerAgent> getPassengers() {
			return java.util.List.of();
		}

		@Override
		public int getPassengerCapacity() {
			return 0;
		}

		@Override
		public org.matsim.vehicles.Vehicle getVehicle() {
			return vehicle;
		}

		@Override
		public MobsimDriverAgent getDriver() {
			return driver;
		}

		@Override
		public org.matsim.api.core.v01.Id<org.matsim.vehicles.Vehicle> getId() {
			return vehicle.getId();
		}
	}

}
