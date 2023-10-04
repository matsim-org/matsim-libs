package org.matsim.contrib.parking.parkingsearch.DynAgent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.parking.parkingsearch.events.RemoveParkingActivityEvent;
import org.matsim.contrib.parking.parkingsearch.events.ReserveParkingLocationEvent;
import org.matsim.contrib.parking.parkingsearch.events.SelectNewParkingLocationEvent;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEvent;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.search.NearestParkingSpotSearchLogic;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import java.util.List;

/**
 * @author Ricardo Ewert
 */
public class NearestParkingDynLeg extends ParkingDynLeg {
	private boolean parkingAtEndOfLeg = true;
	private boolean reachedDestinationWithoutParking = false;
	private boolean alreadyReservedParking = false;
	private boolean driveToBaseWithoutParking = false;
	private final Activity followingActivity;
	private final Leg currentPlannedLeg;
	private final int planIndexNextActivity;
	private Plan plan;
	private Id<Link> nextSelectedParkingLink = null;

	public NearestParkingDynLeg(Leg currentPlannedLeg, NetworkRoute route, Plan plan, int planIndexNextActivity, ParkingSearchLogic logic,
								ParkingSearchManager parkingManager, Id<Vehicle> vehicleId, MobsimTimer timer, EventsManager events) {
		super(currentPlannedLeg.getMode(), route, logic, parkingManager, vehicleId, timer, events);
		this.followingActivity = (Activity) plan.getPlanElements().get(planIndexNextActivity);
		followingActivity.setStartTime(timer.getTimeOfDay());
		this.currentPlannedLeg = currentPlannedLeg;
		this.plan = plan;
		this.planIndexNextActivity = planIndexNextActivity;
		if (followingActivity.getAttributes().getAsMap().containsKey("parking") && followingActivity.getAttributes().getAttribute("parking").equals(
			"noParking"))
			parkingAtEndOfLeg = false;
	}

	@Override
	public void movedOverNode(Id<Link> newLinkId) {
		currentLinkIdx++;
		currentLinkId = newLinkId;
		if (!parkingMode) {
			if (currentLinkId.equals(this.getDestinationLinkId())) {
				if (!parkingAtEndOfLeg) {
					reachedDestinationWithoutParking = true;
				} else {
					this.parkingMode = true;
					this.events
						.processEvent(new StartParkingSearchEvent(timer.getTimeOfDay(), vehicleId, currentLinkId));
					hasFoundParking = parkingManager.reserveSpaceIfVehicleCanParkHere(vehicleId, currentLinkId);
					if (hasFoundParking) {
						this.events.processEvent(new ReserveParkingLocationEvent(timer.getTimeOfDay(), vehicleId, currentLinkId, currentLinkId));
						nextSelectedParkingLink = currentLinkId;
					} else
						((FacilityBasedParkingManager) parkingManager).registerRejectedReservation(timer.getTimeOfDay());
				}
			}
		} else if (followingActivity.getLinkId().equals(newLinkId)) {
			if (alreadyReservedParking)
				hasFoundParking = true;
			else {
				hasFoundParking = parkingManager.reserveSpaceIfVehicleCanParkHere(vehicleId, currentLinkId);
				if (hasFoundParking) {
					this.events.processEvent(new ReserveParkingLocationEvent(timer.getTimeOfDay(), vehicleId, currentLinkId, currentLinkId));
					nextSelectedParkingLink = currentLinkId;
				} else
					((FacilityBasedParkingManager) parkingManager).registerRejectedReservation(timer.getTimeOfDay());
			}
		}
	}

	@Override
	public Id<Link> getNextLinkId() {

		if (!parkingMode && parkingAtEndOfLeg) {
			parkingMode = true;
			this.events.processEvent(new StartParkingSearchEvent(timer.getTimeOfDay(), vehicleId, currentLinkId));
		}
		if (!parkingMode && !reachedDestinationWithoutParking && !driveToBaseWithoutParking) {
			List<Id<Link>> linkIds = route.getLinkIds();

			if (currentLinkIdx == linkIds.size() - 1) {
				return route.getEndLinkId();
			}
			return linkIds.get(currentLinkIdx + 1);

		} else {
			if (hasFoundParking || reachedDestinationWithoutParking) {
				// easy, we can just park where at our destination link
				if (hasFoundParking) {
					double parkingDuration;
					double expectedDrivingDurationToPickup;
					double drivingDurationFromDropOff = timer.getTimeOfDay() - currentPlannedLeg.getDepartureTime().seconds();

					if (nextSelectedParkingLink.equals(currentLinkId)) {
						expectedDrivingDurationToPickup = ((NearestParkingSpotSearchLogic) this.logic).getExpectedTravelTime(
							followingActivity.getLinkId(), timer.getTimeOfDay(), currentLinkId);
					} else {
						expectedDrivingDurationToPickup = ((NearestParkingSpotSearchLogic) this.logic).getExpectedTravelTime(
							currentPlannedLeg.getRoute().getStartLinkId(), timer.getTimeOfDay(), currentLinkId);
					}
					parkingDuration = followingActivity.getMaximumDuration().seconds() - drivingDurationFromDropOff - expectedDrivingDurationToPickup;
					followingActivity.setMaximumDuration(parkingDuration);
				}
				this.logic.reset();
				return null;
			} else {
				if (this.currentAndNextParkLink != null) {
					if (currentAndNextParkLink.getFirst().equals(currentLinkId)) {
						// we already calculated this
						return currentAndNextParkLink.getSecond();
					}
				}
				// need to find the next link
				double nextPickupTime = followingActivity.getStartTime().seconds() + followingActivity.getMaximumDuration().seconds();
				double maxParkingDuration = followingActivity.getMaximumDuration().seconds() - (followingActivity.getStartTime().seconds() - timer.getTimeOfDay());
				Id<Link> nextLinkId = ((NearestParkingSpotSearchLogic) this.logic).getNextLink(currentLinkId, route.getEndLinkId(), vehicleId, mode,
					timer.getTimeOfDay(), maxParkingDuration, nextPickupTime);
				if (((NearestParkingSpotSearchLogic) this.logic).isNextParkingActivitySkipped() && parkingAtEndOfLeg) {
					removeNextActivityAndFollowingLeg();
					parkingAtEndOfLeg = false;
					parkingMode = false;
					driveToBaseWithoutParking = true;
					this.events.processEvent(
						new RemoveParkingActivityEvent(timer.getTimeOfDay(), vehicleId, currentLinkId));
				}
				if (!driveToBaseWithoutParking && !((NearestParkingSpotSearchLogic) this.logic).isUseRandomLinkChoice()) {
					Id<Link> nextPlanedParkingLink = ((NearestParkingSpotSearchLogic) this.logic).getNextParkingLocation();
					if (nextSelectedParkingLink == null || !nextSelectedParkingLink.equals(nextPlanedParkingLink)) {
						nextSelectedParkingLink = nextPlanedParkingLink;
						if (((NearestParkingSpotSearchLogic) this.logic).canReserveParkingSlot()) {
							alreadyReservedParking = parkingManager.reserveSpaceIfVehicleCanParkHere(vehicleId, nextSelectedParkingLink);
							if (alreadyReservedParking)
								this.events.processEvent(
									new ReserveParkingLocationEvent(timer.getTimeOfDay(), vehicleId, currentLinkId, nextSelectedParkingLink));
							else
								((FacilityBasedParkingManager) parkingManager).registerRejectedReservation(timer.getTimeOfDay());
						} else {
							this.events.processEvent(
								new SelectNewParkingLocationEvent(timer.getTimeOfDay(), vehicleId, currentLinkId, nextSelectedParkingLink));
						}
						followingActivity.setLinkId(nextPlanedParkingLink);
					}
				}
				currentAndNextParkLink = new Tuple<>(currentLinkId, nextLinkId);
				if (((NearestParkingSpotSearchLogic) this.logic).getNextRoute() != null)
					currentPlannedLeg.setRoute(((NearestParkingSpotSearchLogic) this.logic).getNextRoute());
				return nextLinkId;
			}
		}
	}

	private void removeNextActivityAndFollowingLeg() {
		plan.getPlanElements().remove(planIndexNextActivity);
		plan.getPlanElements().remove(planIndexNextActivity);
	}

	public boolean driveToBaseWithoutParking() {
		return driveToBaseWithoutParking;
	}
}
