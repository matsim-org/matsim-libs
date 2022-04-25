/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.extension.preplanned.optimizer;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STAY;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Tasks;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public class PreplannedDrtOptimizer implements DrtOptimizer {
	private final PreplannedSchedules preplannedSchedules;

	private final Map<PreplannedRequest, DrtRequest> openRequests = new HashMap<>();

	private final String mode;
	private final Network network;
	private final TravelTime travelTime;
	private final MobsimTimer timer;
	private final DrtTaskFactory taskFactory;
	private final EventsManager eventsManager;

	private final LeastCostPathCalculator router;
	private final double stopDuration;

	public PreplannedDrtOptimizer(DrtConfigGroup drtCfg, PreplannedSchedules preplannedSchedules, Network network,
			TravelTime travelTime, TravelDisutility travelDisutility, MobsimTimer timer, DrtTaskFactory taskFactory,
			EventsManager eventsManager, Fleet fleet) {
		Preconditions.checkArgument(
				fleet.getVehicles().keySet().equals(preplannedSchedules.vehicleToPreplannedStops.keySet()),
				"Some schedules are preplanned for vehicles outside the fleet");

		this.mode = drtCfg.getMode();
		this.preplannedSchedules = preplannedSchedules;
		this.network = network;
		this.travelTime = travelTime;
		this.timer = timer;
		this.taskFactory = taskFactory;
		this.eventsManager = eventsManager;

		router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
		stopDuration = drtCfg.getStopDuration();

		initSchedules(fleet);
	}

	private void initSchedules(Fleet fleet) {
		for (DvrpVehicle veh : fleet.getVehicles().values()) {
			veh.getSchedule()
					.addTask(taskFactory.createStayTask(veh, veh.getServiceBeginTime(), veh.getServiceBeginTime(),
							veh.getStartLink()));
		}
	}

	@Override
	public void requestSubmitted(Request request) {
		var drtRequest = (DrtRequest)request;
		var preplannedRequest = PreplannedRequest.createFromRequest(drtRequest);
		openRequests.put(preplannedRequest, drtRequest);

		var vehicleId = preplannedSchedules.preplannedRequestToVehicle.get(preplannedRequest);

		if (vehicleId == null) {
			Preconditions.checkState(preplannedSchedules.unassignedRequests.contains(preplannedRequest),
					"Pre-planned request (%s) not assigned to any vehicle and not marked as unassigned.",
					preplannedRequest);
			eventsManager.processEvent(new PassengerRequestRejectedEvent(timer.getTimeOfDay(), mode, request.getId(),
					drtRequest.getPassengerId(), "Marked as unassigned"));
			return;
		}

		var preplannedStops = preplannedSchedules.vehicleToPreplannedStops.get(vehicleId);

		Preconditions.checkState(!preplannedStops.isEmpty(),
				"Expected to contain at least preplanned stops for request (%s)", request.getId());

		//TODO in the current implementation we do not know the scheduled pickup and dropoff times
		eventsManager.processEvent(
				new PassengerRequestScheduledEvent(timer.getTimeOfDay(), drtRequest.getMode(), drtRequest.getId(),
						drtRequest.getPassengerId(), vehicleId, Double.NaN, Double.NaN));
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		var schedule = vehicle.getSchedule();

		//TODO we could even skip adding this dummy task
		if (schedule.getStatus() == Schedule.ScheduleStatus.PLANNED) {
			//just execute the initially inserted 0-duration wait task
			schedule.nextTask();
			return;
		}

		var currentTask = schedule.getCurrentTask();
		var currentLink = Tasks.getEndLink(currentTask);
		double currentTime = timer.getTimeOfDay();
		var nonVisitedPreplannedStops = preplannedSchedules.vehicleToPreplannedStops.get(vehicle.getId());
		var nextStop = nonVisitedPreplannedStops.peek();

		if (nextStop == null) {
			// no more preplanned stops, add STAY only if still operating
			if (currentTime < vehicle.getServiceEndTime()) {
				// fill the time gap with STAY
				schedule.addTask(
						taskFactory.createStayTask(vehicle, currentTime, vehicle.getServiceEndTime(), currentLink));
			} else if (!STAY.isBaseTypeOf(currentTask)) {
				// always end with STAY even if delayed
				schedule.addTask(taskFactory.createStayTask(vehicle, currentTime, currentTime, currentLink));
			}
			//if none of the above, this is the end of schedule
		} else if (!nextStop.getLinkId().equals(currentLink.getId())) {
			var nextLink = network.getLinks().get(nextStop.getLinkId());
			VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(currentLink, nextLink, currentTime, router,
					travelTime);
			schedule.addTask(taskFactory.createDriveTask(vehicle, path, DrtDriveTask.TYPE));
		} else if (nextStop.preplannedRequest.earliestStartTime >= timer.getTimeOfDay()) {
			// we need to wait 1 time step to make sure we have already received the request submission event
			// otherwise we may not be able to get the request and insert it to the stop task
			// TODO currently assuming the mobsim time step is 1 s
			schedule.addTask(
					new WaitForStopTask(currentTime, nextStop.preplannedRequest.earliestStartTime + 1, currentLink));
		} else {
			nonVisitedPreplannedStops.poll();//remove this stop from queue

			var stopTask = taskFactory.createStopTask(vehicle, currentTime, currentTime + stopDuration, currentLink);
			if (nextStop.pickup) {
				var request = Preconditions.checkNotNull(openRequests.get(nextStop.preplannedRequest),
						"Request (%s) has not been yet submitted", nextStop.preplannedRequest);
				stopTask.addPickupRequest(request);
			} else {
				var request = Preconditions.checkNotNull(openRequests.remove(nextStop.preplannedRequest),
						"Request (%s) has not been yet submitted", nextStop.preplannedRequest);
				stopTask.addDropoffRequest(request);
			}
			schedule.addTask(stopTask);
		}

		// switch to the next task and update currentTasks
		schedule.nextTask();
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
	}

	public static class PreplannedSchedules {
		private final Map<PreplannedRequest, Id<DvrpVehicle>> preplannedRequestToVehicle;
		//TODO use (immutable)list instead of queue (queue assumes we modify this collection, but we should not - it's input data)
		private final Map<Id<DvrpVehicle>, Queue<PreplannedStop>> vehicleToPreplannedStops;
		private final Set<PreplannedRequest> unassignedRequests;

		public PreplannedSchedules(Map<PreplannedRequest, Id<DvrpVehicle>> preplannedRequestToVehicle,
				Map<Id<DvrpVehicle>, Queue<PreplannedStop>> vehicleToPreplannedStops,
				Set<PreplannedRequest> unassignedRequests) {
			this.preplannedRequestToVehicle = preplannedRequestToVehicle;
			this.vehicleToPreplannedStops = vehicleToPreplannedStops;
			this.unassignedRequests = unassignedRequests;
		}
	}

	// also input to the external optimiser
	public static final class PreplannedRequest {
		private final Id<Person> passengerId;
		private final double earliestStartTime;
		private final double latestStartTime;
		private final double latestArrivalTime;
		private final Id<Link> fromLinkId;
		private final Id<Link> toLinkId;

		static PreplannedRequest createFromRequest(DrtRequest request) {
			return new PreplannedRequest(request.getPassengerId(), request.getEarliestStartTime(),
					request.getLatestStartTime(), request.getLatestArrivalTime(), request.getFromLink().getId(),
					request.getToLink().getId());
		}

		public PreplannedRequest(Id<Person> passengerId, double earliestStartTime, double latestStartTime,
				double latestArrivalTime, Id<Link> fromLinkId, Id<Link> toLinkId) {
			this.passengerId = passengerId;
			this.earliestStartTime = earliestStartTime;
			this.latestStartTime = latestStartTime;
			this.latestArrivalTime = latestArrivalTime;
			this.fromLinkId = fromLinkId;
			this.toLinkId = toLinkId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof PreplannedRequest))
				return false;
			PreplannedRequest that = (PreplannedRequest)o;
			return Objects.equal(passengerId, that.passengerId)
					&& Objects.equal(fromLinkId, that.fromLinkId)
					&& Objects.equal(toLinkId, that.toLinkId);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(passengerId, fromLinkId, toLinkId);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("passengerId", passengerId)
					.add("earliestStartTime", earliestStartTime)
					.add("latestStartTime", latestStartTime)
					.add("latestArrivalTime", latestArrivalTime)
					.add("fromLinkId", fromLinkId)
					.add("toLinkId", toLinkId)
					.toString();
		}
	}

	// sequence of preplanned tasks is the output from the external optimiser and the input to the drt simulation
	public static final class PreplannedStop {
		private final PreplannedRequest preplannedRequest;
		private final boolean pickup;//pickup or dropoff

		public PreplannedStop(PreplannedRequest preplannedRequest, boolean pickup) {
			this.preplannedRequest = preplannedRequest;
			this.pickup = pickup;
		}

		private Id<Link> getLinkId() {
			return pickup ? preplannedRequest.fromLinkId : preplannedRequest.toLinkId;
		}
	}
}
