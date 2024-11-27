package org.matsim.contrib.parking.parkingsearch.DynAgent.agentLogic;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.IdleDynActivity;
import org.matsim.contrib.dynagent.StaticPassengerDynLeg;
import org.matsim.contrib.parking.parkingsearch.DynAgent.NearestParkingDynLeg;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.vehicles.Vehicle;

import java.util.List;

/**
 * @author Ricardo Ewert
 */
public class NearestParkingSpotAgentLogic extends ParkingAgentLogic {
	public NearestParkingSpotAgentLogic(Plan plan, ParkingSearchManager parkingManager, RoutingModule walkRouter, Network network,
										ParkingRouter parkingRouter, EventsManager events, ParkingSearchLogic parkingLogic, MobsimTimer timer,
										VehicleTeleportationLogic teleportationLogic, ParkingSearchConfigGroup configGroup) {
		super(plan, parkingManager, walkRouter, network, parkingRouter, events, parkingLogic, timer, teleportationLogic, configGroup);
	}

	@Override
	public DynAction computeNextAction(DynAction oldAction, double now) {
		// we have the following cases of ending dynacts:
		// non-car trip arrival: start Activity
		// car-trip arrival: add park-car activity
		// park-car activity: get next PlanElement & add walk leg to activity location
		// walk-leg to act: start next PlanElement Activity
		// ordinary activity: get next Leg, if car: go to car, otherwise add ordinary leg by other mode
		// walk-leg to car: add unpark activity
		// unpark activity: find the way to the next route & start leg

		//if a parking activity was skipped we need no change the nexParkActionState
		if (lastParkActionState.equals(LastParkActionState.CARTRIP) && ((NearestParkingDynLeg) oldAction).driveToBaseWithoutParking()) {
			this.lastParkActionState = LastParkActionState.WALKFROMPARK;
		}

		return switch (lastParkActionState) {
			case ACTIVITY -> nextStateAfterActivity(oldAction, now);
			case CARTRIP -> nextStateAfterCarTrip(oldAction, now);
			case NONCARTRIP -> nextStateAfterNonCarTrip(oldAction, now);
			case PARKACTIVITY -> nextStateAfterParkActivity(oldAction, now);
			case UNPARKACTIVITY -> nextStateAfterUnParkActivity(oldAction, now);
			case WALKFROMPARK -> nextStateAfterWalkFromPark(oldAction, now);
			case WALKTOPARK -> nextStateAfterWalkToPark(oldAction, now);
		};
	}

	@Override
	protected DynAction nextStateAfterUnParkActivity(DynAction oldAction, double now) {
		// we have unparked, now we need to get going by car again.

		Leg currentPlannedLeg = (Leg) currentPlanElement;
		currentPlannedLeg.setDepartureTime(timer.getTimeOfDay());
		Route plannedRoute = currentPlannedLeg.getRoute();
		NetworkRoute actualRoute = this.parkingRouter.getRouteFromParkingToDestination(plannedRoute.getEndLinkId(), now, agent.getCurrentLinkId());
		actualRoute.setVehicleId(currentlyAssignedVehicleId);
		if (!plannedRoute.getStartLinkId().equals(actualRoute.getStartLinkId())) {
			currentPlannedLeg.setRoute(actualRoute);
		}
		if ((this.parkingManager.unParkVehicleHere(currentlyAssignedVehicleId, agent.getCurrentLinkId(), now)) || (isInitialLocation)) {
			this.lastParkActionState = LastParkActionState.CARTRIP;
			isInitialLocation = false;
//			Leg currentLeg = (Leg) this.currentPlanElement;
			int planIndexNextActivity = planIndex + 1;
			Activity nextPlanElement = (Activity) plan.getPlanElements().get(planIndexNextActivity);
			if (ParkingUtils.checkIfActivityHasNoParking(nextPlanElement)) {
				this.lastParkActionState = LastParkActionState.WALKFROMPARK;
			}
			//this could be Car, Carsharing, Motorcylce, or whatever else mode we have, so we want our leg to reflect this.
			return new NearestParkingDynLeg(currentPlannedLeg, actualRoute, plan, planIndexNextActivity, parkingLogic, parkingManager,
				currentlyAssignedVehicleId, timer, events);
		} else {
			throw new RuntimeException("parking location mismatch");
		}

	}

	@Override
	protected DynAction nextStateAfterParkActivity(DynAction oldAction, double now) {
		// add a walk leg after parking
		Leg currentPlannedLeg = (Leg) currentPlanElement;
		Facility fromFacility = new LinkWrapperFacility(network.getLinks().get(agent.getCurrentLinkId()));
		Facility toFacility = new LinkWrapperFacility(network.getLinks().get(currentPlannedLeg.getRoute().getEndLinkId()));
		List<? extends PlanElement> walkTrip = walkRouter.calcRoute(
			DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, now, plan.getPerson()));
		if (walkTrip.size() != 1 || !(walkTrip.get(0) instanceof Leg)) {
			String message = "walkRouter returned something else than a single Leg, e.g. it routes walk on the network with non_network_walk to " +
				"access the network. Not implemented in parking yet!";
			log.error(message);
			throw new RuntimeException(message);
		}
		Leg walkLeg = (Leg) walkTrip.get(0);
		this.lastParkActionState = LastParkActionState.WALKFROMPARK;
		this.stageInteractionType = null;
		if (!walkLeg.getTravelTime().equals(OptionalTime.defined(0.))) {
			return new StaticPassengerDynLeg(walkLeg.getRoute(), walkLeg.getMode());
		} else {
			return nextStateAfterWalkFromPark(oldAction, now);
		}
	}

	@Override
	protected DynAction nextStateAfterActivity(DynAction oldAction, double now) {
		// we could either depart by car or not next

		if (plan.getPlanElements().size() >= planIndex + 1) {
			if (plan.getPlanElements().get(planIndex) instanceof Activity && ((Activity) plan.getPlanElements().get(planIndex)).getType().equals(
				ParkingUtils.WaitingForParkingActivityType)) {
				//now the waiting activity has finished and we can park now
				this.parkingManager.parkVehicleHere(Id.create(this.agent.getId(), Vehicle.class), agent.getCurrentLinkId(), now);
				return nextStateAfterNonCarTrip(oldAction, now);
			}
			if (plan.getPlanElements().get(planIndex + 1) instanceof Activity) {
				return nextStateAfterNonCarTrip(oldAction, now);
			}
			if (plan.getPlanElements().get(planIndex) instanceof Activity && ((Activity) plan.getPlanElements().get(planIndex)).getType()
																															   .contains("_GetOff")) {
				((Activity) plan.getPlanElements().get(planIndex)).setEndTime(now);
				((Activity) plan.getPlanElements().get(planIndex + 4)).setStartTime(now + ((Activity) plan.getPlanElements()
																										  .get(planIndex + 2)).getMaximumDuration()
																															  .seconds());
				// checks if it is possible to stay from getOff until getIn
				boolean possibleToStay = checkIfParkingIsPossibleUntilNextActivities(this.planIndex, this.planIndex + 2);
				if (possibleToStay) {
					return nextStateAfterNonCarTrip(oldAction, now);
				}
			}
			planIndex++;
			this.currentPlanElement = plan.getPlanElements().get(planIndex);
			Leg currentLeg = (Leg) currentPlanElement;
			if (currentLeg.getMode().equals(TransportMode.car)) {
				Id<Vehicle> vehicleId = Id.create(this.agent.getId(), Vehicle.class);
				Id<Link> parkLink = this.parkingManager.getVehicleParkingLocation(vehicleId);

				if (parkLink == null) {
					//this is the first activity of a day and our parking manager does not provide information about initial stages. We suppose the
					// car is parked where we are
					parkLink = agent.getCurrentLinkId();
				}

				Facility fromFacility = new LinkWrapperFacility(network.getLinks().get(agent.getCurrentLinkId()));
				Id<Link> teleportedParkLink = this.teleportationLogic.getVehicleLocation(agent.getCurrentLinkId(), vehicleId, parkLink, now,
					currentLeg.getMode());
				Facility toFacility = new LinkWrapperFacility(network.getLinks().get(teleportedParkLink));
				List<? extends PlanElement> walkTrip = walkRouter.calcRoute(
					DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, now, plan.getPerson()));
				if (walkTrip.size() != 1 || !(walkTrip.get(0) instanceof Leg walkLeg)) {
					String message = "walkRouter returned something else than a single Leg, e.g. it routes walk on the network with non_network_walk" +
						" to access the network. Not implemented in parking yet!";
					log.error(message);
					throw new RuntimeException(message);
				}
				this.currentlyAssignedVehicleId = vehicleId;
				this.stageInteractionType = ParkingUtils.ParkingStageInteractionType;
				if (!walkLeg.getTravelTime().equals(OptionalTime.defined(0.))) {
					this.lastParkActionState = LastParkActionState.WALKTOPARK;
					return new StaticPassengerDynLeg(walkLeg.getRoute(), walkLeg.getMode());
				} else {
					return nextStateAfterWalkToPark(oldAction, now);
				}
			} else if (currentLeg.getMode().equals(TransportMode.pt)) {
				if (currentLeg.getRoute() instanceof TransitPassengerRoute) {
					throw new IllegalStateException("not yet implemented");
				} else {
					this.lastParkActionState = LastParkActionState.NONCARTRIP;
					return new StaticPassengerDynLeg(currentLeg.getRoute(), currentLeg.getMode());
				}
				//teleport or pt route
			} else {
				//teleport
				this.lastParkActionState = LastParkActionState.NONCARTRIP;
				return new StaticPassengerDynLeg(currentLeg.getRoute(), currentLeg.getMode());
			}

		} else {
			throw new RuntimeException(
				"no more leg to follow but activity is ending\nLastPlanElement: " + currentPlanElement.toString() + "\n Agent " + this.agent.getId() +
					"\nTime: " + Time.writeTime(
					now));
		}
	}

	@Override
	protected DynAction nextStateAfterWalkToPark(DynAction oldAction, double now) {
		//walk2park is complete, we can unpark.
		this.lastParkActionState = LastParkActionState.UNPARKACTIVITY;
		Activity beforePlanElement = (Activity) plan.getPlanElements().get(planIndex - 1);
		if (ParkingUtils.checkIfActivityHasNoParking(beforePlanElement)) {
			return nextStateAfterUnParkActivity(oldAction, now); // wenn kein Parken dann einfach weiter
		}
		return new IdleDynActivity(this.stageInteractionType, now + configGroup.getUnparkduration());
	}

	@Override
	protected DynAction nextStateAfterCarTrip(DynAction oldAction, double now) {
		if (this.plan.getPlanElements().get(planIndex + 1) instanceof Activity && ((Activity) this.plan.getPlanElements().get(
			planIndex + 1)).getType().equals(ParkingUtils.WaitingForParkingActivityType)) {
			//next activity is waiting for parking. Thats why we have no parkVehicleHere at this moment
			this.lastParkActionState = LastParkActionState.PARKACTIVITY;
			this.currentlyAssignedVehicleId = null;
			this.parkingLogic.reset();
			return new IdleDynActivity(this.stageInteractionType, now + configGroup.getParkduration());
		}
		// car trip is complete, we have found a parking space (not part of the logic), block it and start to park
		if (this.parkingManager.parkVehicleHere(Id.create(this.agent.getId(), Vehicle.class), agent.getCurrentLinkId(), now)) {
			this.lastParkActionState = LastParkActionState.PARKACTIVITY;
			this.currentlyAssignedVehicleId = null;
			this.parkingLogic.reset();
			return new IdleDynActivity(this.stageInteractionType, now + configGroup.getParkduration());
		} else {
			throw new RuntimeException("No parking possible");
		}
	}

	@Override
	protected DynAction nextStateAfterNonCarTrip(DynAction oldAction, double now) {

		this.currentPlanElement = plan.getPlanElements().get(planIndex + 1);
		Activity nextPlannedActivity = (Activity) this.currentPlanElement;
		// checks if you can extend parking here until getIn
		if (nextPlannedActivity.getType().equals(ParkingUtils.ParkingActivityType) && plan.getPlanElements().get(planIndex + 2) instanceof Leg) {
			checkIfParkingIsPossibleUntilNextActivities(planIndex + 1, planIndex + 1);
		}
		// switch back to activity
		planIndex++;
		this.lastParkActionState = LastParkActionState.ACTIVITY;
		final double endTime;
		if (nextPlannedActivity.getEndTime().isUndefined()) {
			if (nextPlannedActivity.getMaximumDuration().isUndefined()) {
				endTime = Double.POSITIVE_INFINITY;
				//last activity of a day
			} else {
				endTime = now + nextPlannedActivity.getMaximumDuration().seconds();
			}
		} else {
			endTime = nextPlannedActivity.getEndTime().seconds();
		}
		return new IdleDynActivity(nextPlannedActivity.getType(), endTime);

	}

	private boolean checkIfParkingIsPossibleUntilNextActivities(int indexOfCurrentActivity, int indexOfParkingActivity) {
		int indexOfFollowingActivity = indexOfCurrentActivity + 2;
		Activity followingActivity = ((Activity) plan.getPlanElements().get(indexOfFollowingActivity));
		//checks if it is possible to stay from the current getOff until the getIn
		if (indexOfFollowingActivity == indexOfParkingActivity) {
			Activity currentActivity = ((Activity) plan.getPlanElements().get(this.planIndex));
			Activity activityAfterFollowing = ((Activity) plan.getPlanElements().get(this.planIndex + 4));
			if (agent.getCurrentLinkId().equals(activityAfterFollowing.getLinkId()) && !ParkingUtils.checkIfActivityHasNoParking(
				(Activity) currentPlanElement)) {
				boolean canParkAtFacilityUntilGetIn = ((FacilityBasedParkingManager) parkingManager).canParkAtThisFacilityUntilEnd(
					agent.getCurrentLinkId(),
					timer.getTimeOfDay(), currentActivity.getMaximumDuration().seconds(), followingActivity.getMaximumDuration().seconds(),
					activityAfterFollowing.getMaximumDuration().seconds());
				if (canParkAtFacilityUntilGetIn) {
					plan.getPlanElements().remove(this.planIndex + 3);
					plan.getPlanElements().remove(this.planIndex + 1);
					((FacilityBasedParkingManager) parkingManager).registerStayFromGetOffUntilGetIn(this.agent.getVehicle().getId());
					return true;
				}
			}
		}
		// checks if the now started parking activity can extend until the end of the following GetIn activity
		else if (indexOfCurrentActivity == indexOfParkingActivity) {
			Activity currentActivity = ((Activity) plan.getPlanElements().get(this.planIndex + 1));
			if (agent.getCurrentLinkId().equals(followingActivity.getLinkId()) && !ParkingUtils.checkIfActivityHasNoParking(
				followingActivity)) {
				boolean canParkAtFacilityUntilGetIn = ((FacilityBasedParkingManager) parkingManager).canParkAtThisFacilityUntilEnd(
					agent.getCurrentLinkId(),
					timer.getTimeOfDay(), 0., currentActivity.getMaximumDuration().seconds(),
					followingActivity.getMaximumDuration().seconds());
				if (canParkAtFacilityUntilGetIn) {
					plan.getPlanElements().remove(indexOfParkingActivity + 1);
					currentActivity.setEndTime(followingActivity.getStartTime().seconds());
					((FacilityBasedParkingManager) parkingManager).registerParkingBeforeGetIn(this.agent.getVehicle().getId());
					return true;
				}
			}
		}
		return false;
	}
}
