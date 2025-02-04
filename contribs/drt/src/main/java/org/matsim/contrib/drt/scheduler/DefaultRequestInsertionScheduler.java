/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.scheduler;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;
import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STAY;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.path.DivertedVrpPath;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.CapacityChangeTask;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelTime;

import com.google.common.base.Verify;

/**
 * @author michalm
 * @author Sebastian Hörl, IRT SystemX (sebhoerl)
 */
public class DefaultRequestInsertionScheduler implements RequestInsertionScheduler {

	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final ScheduleTimingUpdater scheduleTimingUpdater;
	private final DrtTaskFactory taskFactory;
	private final StopTimeCalculator stopTimeCalculator;
	private final boolean scheduleWaitBeforeDrive;

	public DefaultRequestInsertionScheduler(Fleet fleet, MobsimTimer timer, TravelTime travelTime,
											ScheduleTimingUpdater scheduleTimingUpdater, DrtTaskFactory taskFactory,
											StopTimeCalculator stopTimeCalculator, boolean scheduleWaitBeforeDrive) {
		this.timer = timer;
		this.travelTime = travelTime;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.taskFactory = taskFactory;
		this.stopTimeCalculator = stopTimeCalculator;
		this.scheduleWaitBeforeDrive = scheduleWaitBeforeDrive;
		initSchedules(fleet);
	}

	private void initSchedules(Fleet fleet) {
		for (DvrpVehicle veh : fleet.getVehicles().values()) {
			veh.getSchedule()
					.addTask(taskFactory.createInitialTask(veh, veh.getServiceBeginTime(), veh.getServiceEndTime(),
							veh.getStartLink()));
		}
	}

	@Override
	public PickupDropoffTaskPair scheduleRequest(AcceptedDrtRequest request, InsertionWithDetourData insertion) {
		var pickupTask = insertPickup(request, insertion);
		verifyTimes("Inconsistent pickup departure time", insertion.detourTimeInfo.pickupDetourInfo.departureTime,
				pickupTask.getEndTime());

		var dropoffTask = insertDropoff(request, insertion, pickupTask);
		verifyTimes("Inconsistent dropoff arrival time", insertion.detourTimeInfo.dropoffDetourInfo.arrivalTime,
				dropoffTask.getBeginTime());

		verifyTaskContinuity(insertion);
		verifyConstraints(insertion);
		verifyStructure(insertion.insertion.vehicleEntry.vehicle.getSchedule());

		return new PickupDropoffTaskPair(pickupTask, dropoffTask);
	}

	private void verifyTaskContinuity(InsertionWithDetourData insertion) {
		/*
		 * During insertion of tasks the common sanity checks that happen when appending
		 * to the schedule (next start time = previous end time) are not evaluated. This
		 * method verifies that the timing remains intact after insertion.
		 */

		Schedule schedule = insertion.insertion.vehicleEntry.vehicle.getSchedule();
		for (int i = 1; i < schedule.getTaskCount(); i++) {
			Task first = schedule.getTasks().get(i - 1);
			Task second = schedule.getTasks().get(i);
			Verify.verify(first.getEndTime() == second.getBeginTime());
		}
	}

	private final boolean verifyConstraints = false;

	private void verifyConstraints(InsertionWithDetourData insertion) {
		/*
		 * Verifies that no request constraints are violated after scheduling. This
		 * check is only valid when simulating under free-flow conditions.
		 */
		if (verifyConstraints) {
			Schedule schedule = insertion.insertion.vehicleEntry.vehicle.getSchedule();

			for (Task task : schedule.getTasks()) {
				if (task instanceof DrtStopTask stopTask) {

					for (AcceptedDrtRequest request : stopTask.getPickupRequests().values()) {
						Verify.verify(stopTask.getEndTime() <= request.getLatestStartTime());
					}

					for (AcceptedDrtRequest request : stopTask.getDropoffRequests().values()) {
						Verify.verify(stopTask.getBeginTime() <= request.getLatestArrivalTime());
					}
				}
			}
		}
	}

	private void verifyStructure(Schedule schedule) {
		DriveTask previousDrive = null;

		int startIndex = schedule.getStatus().equals(ScheduleStatus.STARTED) ? schedule.getCurrentTask().getTaskIdx()
				: 0;

		for (int index = startIndex; index < schedule.getTaskCount(); index++) {
			Task task = schedule.getTasks().get(index);

			if (task instanceof DriveTask driveTask) {
				if(previousDrive != null) {
					Verify.verify(previousDrive.getPath() instanceof DivertedVrpPath,
							"The first of two subsequent drive tasks has to be a diverted path.");
					Id<Link> firstEnd = previousDrive.getPath().getToLink().getId();
					Id<Link> secondStart = driveTask.getPath().getFromLink().getId();
					Verify.verify(firstEnd.equals(secondStart),
							String.format("Subsequent drive tasks are not connected link %s !=> %s", firstEnd.toString(), secondStart.toString()));
				}
				previousDrive = driveTask;
			} else {
				previousDrive = null;
			}
		}
	}

	private void verifyTimes(String messageStart, double timeFromInsertionData, double timeFromScheduler) {
		// The source of discrepancies is the first link travel time (as it should be taken into consideration
		// when a vehicle enters the traffic (a new drive task), but not when a vehicle is diverted (an ongoing drive task)
		// In addition, there may be some small rounding errors (therefore 1e-10 is considered)
		// TODO temporarily commented out. Requires some investigation
		// Verify.verify(Math.abs(timeFromInsertionData - timeFromScheduler) <= VrpPaths.FIRST_LINK_TT + 1e-10,
		// 		"%s: %s (insertion data) vs %s (scheduler)", messageStart, timeFromInsertionData,
		// 		timeFromScheduler);
	}

	private DrtStopTask insertPickup(AcceptedDrtRequest request, InsertionWithDetourData insertionWithDetourData) {
		final double now = timer.getTimeOfDay();
		var insertion = insertionWithDetourData.insertion;
		VehicleEntry vehicleEntry = insertion.vehicleEntry;
		Schedule schedule = vehicleEntry.vehicle.getSchedule();
		List<Waypoint.Stop> stops = vehicleEntry.stops;
		int pickupIdx = insertion.pickup.index;
		int dropoffIdx = insertion.dropoff.index;
		var detourData = insertionWithDetourData.detourData;

		ScheduleStatus scheduleStatus = schedule.getStatus();
		Task currentTask = scheduleStatus == ScheduleStatus.PLANNED ? null : schedule.getCurrentTask();
		Task beforePickupTask;

		if (pickupIdx == 0 && scheduleStatus != ScheduleStatus.PLANNED && DRIVE.isBaseTypeOf(currentTask)) {
			LinkTimePair diversion = ((OnlineDriveTaskTracker)currentTask.getTaskTracker()).getDiversionPoint();
			if (diversion != null) { // divert vehicle
				beforePickupTask = currentTask;
				VrpPathWithTravelData vrpPath = VrpPaths.createPath(vehicleEntry.start.link, request.getFromLink(),
						vehicleEntry.start.time, detourData.detourToPickup, travelTime);

				if (vrpPath.getArrivalTime() < request.getEarliestStartTime() && scheduleWaitBeforeDrive) {
					// prebooking case: we need to wait right now
					((OnlineDriveTaskTracker)beforePickupTask.getTaskTracker()).divertPath(VrpPaths.createZeroLengthPathForDiversion(diversion));
					beforePickupTask = insertDriveWithWait(vehicleEntry.vehicle, currentTask, vrpPath, request.getEarliestStartTime());
				} else {
					((OnlineDriveTaskTracker)beforePickupTask.getTaskTracker()).divertPath(vrpPath);
					// prebooking: may want to wait after continuing to drive
					beforePickupTask = insertWait(vehicleEntry.vehicle, beforePickupTask, request.getEarliestStartTime());
				}
			} else { // too late for diversion
				if (request.getFromLink() != vehicleEntry.start.link) { // add a new drive task
					VrpPathWithTravelData vrpPath = VrpPaths.createPath(vehicleEntry.start.link, request.getFromLink(),
							vehicleEntry.start.time, detourData.detourToPickup, travelTime);
					// prebooking: may want to wait before or after driving
					beforePickupTask = insertDriveWithWait(vehicleEntry.vehicle, currentTask, vrpPath, request.getEarliestStartTime());
				} else { // no need for a new drive task, but may want to wait until start of stop
					beforePickupTask = insertWait(vehicleEntry.vehicle, currentTask, dropoffIdx);
				}
			}
			if(!stops.isEmpty() && stops.size() + 1 > pickupIdx) {
				//there is an existing stop which was scheduled earlier and was not the destination of the already diverted drive task
				removeBetween(schedule, beforePickupTask, stops.get(pickupIdx).task);
			}
		} else { // insert pickup after an existing stop/stay task
			StayTask stayTask = null;
			DrtStopTask stopTask = null;
			if (pickupIdx == 0) {
				if (scheduleStatus == ScheduleStatus.PLANNED) {// PLANNED schedule
					stayTask = (StayTask)schedule.getTasks().getFirst();
					stayTask.setEndTime(stayTask.getBeginTime());// could get later removed with ScheduleTimingUpdater
				} else if (STAY.isBaseTypeOf(currentTask)) {
					stayTask = (StayTask)currentTask; // ongoing stay task
					if (stayTask.getEndTime() > now) { // stop stay task; a new stop/drive task can be inserted now
						stayTask.setEndTime(now);
					}
				} else {
					stopTask = (DrtStopTask)currentTask; // ongoing stop task
				}
			} else {
				stopTask = stops.get(pickupIdx - 1).task; // future stop task
			}

			boolean canMergePickup = stopTask != null && !(stopTask instanceof CapacityChangeTask) && request.getFromLink() == stopTask.getLink()
					&& stopTask.getEndTime() >= request.getEarliestStartTime();

			if (canMergePickup) { // no detour; no new stop task
				// add pickup request to stop task
				stopTask.addPickupRequest(request);

				// potentially extend task
				stopTask.setEndTime(stopTimeCalculator.updateEndTimeForPickup(vehicleEntry.vehicle, stopTask,
						now, request.getRequest()));

				// add drive from pickup
				if (pickupIdx == dropoffIdx) {
					// remove drive i->i+1 (if there is one)
					if (pickupIdx < stops.size()) {// there is at least one following stop
						DrtStopTask nextStopTask = stops.get(pickupIdx).task;
						removeBetween(schedule, stopTask, nextStopTask);
					}

					Link toLink = request.getToLink(); // pickup->dropoff

					VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getFromLink(), toLink,
							stopTask.getEndTime(), detourData.detourFromPickup, travelTime);
					Task driveFromPickupTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath,
							DrtDriveTask.TYPE); // immediate drive to dropoff
					schedule.addTask(stopTask.getTaskIdx() + 1, driveFromPickupTask);
				} else {
					scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle,
							stopTask.getTaskIdx() + 1, stopTask.getEndTime());
				}

				return stopTask;
			} else {
				StayTask stayOrStopTask = stayTask != null ? stayTask : stopTask;

				// remove drive i->i+1 (if there is one)
				if (pickupIdx < stops.size()) {// there is at least one following stop
					DrtStopTask nextStopTask = stops.get(pickupIdx).task;
					removeBetween(schedule, stayOrStopTask, nextStopTask);
				}

				if (request.getFromLink() == stayOrStopTask.getLink()) {
					// the bus stays where it is
					beforePickupTask = stayOrStopTask;

					// prebooking: but we may want to wait a bit if next stop is in a while
					beforePickupTask = insertWait(vehicleEntry.vehicle, beforePickupTask, request.getEarliestStartTime());
				} else {// add drive task to pickup location
					// insert drive i->pickup
					VrpPathWithTravelData vrpPath = VrpPaths.createPath(stayOrStopTask.getLink(), request.getFromLink(),
							stayOrStopTask.getEndTime(), detourData.detourToPickup, travelTime);
					// we may want to wait before or after driving
					beforePickupTask = insertDriveWithWait(vehicleEntry.vehicle, stayOrStopTask, vrpPath, request.getEarliestStartTime());
				}
			}
		}

		// insert pickup stop task
		int taskIdx = beforePickupTask.getTaskIdx() + 1;
		double stopEndTime = stopTimeCalculator.initEndTimeForPickup(vehicleEntry.vehicle, beforePickupTask.getEndTime(), request.getRequest());
		DrtStopTask pickupStopTask = taskFactory.createStopTask(vehicleEntry.vehicle, beforePickupTask.getEndTime(),
				stopEndTime, request.getFromLink());
		schedule.addTask(taskIdx, pickupStopTask);
		pickupStopTask.addPickupRequest(request);

		// add drive from pickup
		Link toLink = pickupIdx == dropoffIdx ? request.getToLink() // pickup->dropoff
				: stops.get(pickupIdx).task.getLink(); // pickup->i+1

		double nextBeginTime = pickupIdx == dropoffIdx ? //
				pickupStopTask.getEndTime() : // asap
				stops.get(pickupIdx).task instanceof CapacityChangeTask capacityChangeTask ?
					capacityChangeTask.getBeginTime() :
				stops.get(pickupIdx).task.getPickupRequests().values()
						.stream()
						.mapToDouble(AcceptedDrtRequest::getEarliestStartTime)
						.min()
						.orElse(pickupStopTask.getEndTime());

		if (request.getFromLink() == toLink) {
			// prebooking case when we are already at the stop location, but next stop task happens in the future
			Task afterPickupTask = insertWait(vehicleEntry.vehicle, pickupStopTask, nextBeginTime);

			// update timings
			if (pickupIdx != dropoffIdx) {
				// update schedule when inserting the dropoff, otherwise we will illegally shift
				// the begin time of a following prebooked stop if there is one
				scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, afterPickupTask.getTaskIdx() + 1,
						afterPickupTask.getEndTime());
			}
		} else {
			VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getFromLink(), toLink, pickupStopTask.getEndTime(),
					detourData.detourFromPickup, travelTime);

			// may want to wait now or before next stop task
			Task afterPickupTask = insertDriveWithWait(vehicleEntry.vehicle, pickupStopTask, vrpPath, nextBeginTime);

			// update timings
			if (pickupIdx != dropoffIdx) {
				// update schedule when inserting the dropoff, otherwise we will illegally shift
				// the begin time of a following prebooked stop if there is one
				scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, afterPickupTask.getTaskIdx() + 1,
						afterPickupTask.getEndTime());
			}
		}

		return pickupStopTask;
	}

	private DrtStopTask insertDropoff(AcceptedDrtRequest request, InsertionWithDetourData insertionWithDetourData,
									  DrtStopTask pickupTask) {
		final double now = timer.getTimeOfDay();
		var insertion = insertionWithDetourData.insertion;
		VehicleEntry vehicleEntry = insertion.vehicleEntry;
		Schedule schedule = vehicleEntry.vehicle.getSchedule();
		List<Waypoint.Stop> stops = vehicleEntry.stops;
		int pickupIdx = insertion.pickup.index;
		int dropoffIdx = insertion.dropoff.index;
		var detourData = insertionWithDetourData.detourData;

		final Task driveToDropoffTask;
		if (pickupIdx == dropoffIdx) { // no drive to dropoff
			int pickupTaskIdx = pickupTask.getTaskIdx();
			driveToDropoffTask = schedule.getTasks().get(pickupTaskIdx + 1);
		} else {
			DrtStopTask stopTask = stops.get(dropoffIdx - 1).task;
			if (request.getToLink() == stopTask.getLink()) { // no detour; no new stop task
				// add dropoff request to stop task, and extend the stop task (when incremental stop task duration is used)
				stopTask.addDropoffRequest(request);

				// potentially extend task
				stopTask.setEndTime(stopTimeCalculator.updateEndTimeForDropoff(vehicleEntry.vehicle, stopTask,
						now, request.getRequest()));
				scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle,
						stopTask.getTaskIdx() + 1, stopTask.getEndTime());

				return stopTask;
			} else { // add drive task to dropoff location

				// remove drive j->j+1 (if j is not the last stop)
				if (dropoffIdx < stops.size()) {
					DrtStopTask nextStopTask = stops.get(dropoffIdx).task;
					removeBetween(schedule, stopTask, nextStopTask);
				}

				// insert drive i->dropoff
				VrpPathWithTravelData vrpPath = VrpPaths.createPath(stopTask.getLink(), request.getToLink(),
						stopTask.getEndTime(), detourData.detourToDropoff, travelTime);
				// direct drive to dropoff (no waiting)
				driveToDropoffTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
				schedule.addTask(stopTask.getTaskIdx() + 1, driveToDropoffTask);
			}
		}

		// insert dropoff stop task
		double startTime = driveToDropoffTask.getEndTime();
		int taskIdx = driveToDropoffTask.getTaskIdx() + 1;
		double stopEndTime = stopTimeCalculator.initEndTimeForDropoff(vehicleEntry.vehicle, startTime, request.getRequest());
		DrtStopTask dropoffStopTask = taskFactory.createStopTask(vehicleEntry.vehicle, startTime,
				stopEndTime, request.getToLink());
		schedule.addTask(taskIdx, dropoffStopTask);
		dropoffStopTask.addDropoffRequest(request);

		// add drive from dropoff
		if (dropoffIdx == stops.size()) {// bus stays at dropoff
			if (taskIdx + 2 == schedule.getTaskCount()) {// remove stay task from the end of schedule,
				DrtStayTask oldStayTask = (DrtStayTask)schedule.getTasks().get(taskIdx + 1);
				schedule.removeTask(oldStayTask);
			}
			if (taskIdx + 1 == schedule.getTaskCount()) {
				// no stay task at the end if the pickup follows the existing stay task
				double endTime = Math.max(dropoffStopTask.getEndTime(), vehicleEntry.vehicle.getServiceEndTime());
				schedule.addTask(taskFactory.createStayTask(vehicleEntry.vehicle, dropoffStopTask.getEndTime(), endTime,
						dropoffStopTask.getLink()));
			} else {
				throw new RuntimeException();
			}
		} else {
			Link toLink = stops.get(dropoffIdx).task.getLink(); // dropoff->j+1

			VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getToLink(), toLink, dropoffStopTask.getEndTime(),
					detourData.detourFromDropoff, travelTime);

			if (toLink == request.getToLink()) {
				// prebooking case: we stay, but may add some wait time until the next stop
				Task afterDropoffTask = insertWait(vehicleEntry.vehicle, dropoffStopTask,
						stops.get(dropoffIdx).task.getBeginTime());

				scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle,
						afterDropoffTask.getTaskIdx() + 1, afterDropoffTask.getEndTime());
			} else {
				// may want to wait here or after driving before starting next stop
				double earliestArrivalTime = stops.get(dropoffIdx).getChangedCapacity() != null ?
					stops.get(dropoffIdx).getArrivalTime()
						: stops.get(dropoffIdx).task.getPickupRequests().values()
						.stream()
						.mapToDouble(AcceptedDrtRequest::getEarliestStartTime)
						.min()
						.orElse(dropoffStopTask.getEndTime());
				Task afterDropoffTask = insertDriveWithWait(vehicleEntry.vehicle, dropoffStopTask, vrpPath,
						earliestArrivalTime);

				scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle,
						afterDropoffTask.getTaskIdx() + 1, afterDropoffTask.getEndTime());
			}
		}
		return dropoffStopTask;
	}

	/**
	 * Removes drive and wait tasks between the startTask and the endTask
	 */
	private void removeBetween(Schedule schedule, Task startTask, Task endTask) {
		Verify.verify(endTask.getTaskIdx() >= startTask.getTaskIdx());
		Verify.verify(endTask.getTaskIdx() <= startTask.getTaskIdx() + 3);

		int waitCount = 0;
		int driveCount = 0;

		int removeCount = endTask.getTaskIdx() - startTask.getTaskIdx() - 1;

		for (int k = 0; k < removeCount; k++) {
			Task task = schedule.getTasks().get(startTask.getTaskIdx() + 1);

			if (DrtTaskBaseType.DRIVE.isBaseTypeOf(task.getTaskType())) {
				driveCount++;
			} else if (DrtStayTask.TYPE.equals(task.getTaskType())) {
				waitCount++;
			} else {
				throw new IllegalStateException("Invalid schedule structure: expected WAIT or DRIVE task");
			}

			schedule.removeTask(task);
		}

		Verify.verify(waitCount <= 1);
		Verify.verify(driveCount <= 1);
	}

	private Task insertWait(DvrpVehicle vehicle, Task departureTask, double earliestNextStartTime) {
		Schedule schedule = vehicle.getSchedule();

		final Link waitLink;
		if (departureTask instanceof StayTask) {
			waitLink = ((StayTask) departureTask).getLink();
		} else if (departureTask instanceof DriveTask) {
			waitLink = ((DriveTask) departureTask).getPath().getToLink();
		} else {
			throw new IllegalStateException();
		}

		if (departureTask.getEndTime() < earliestNextStartTime) {
			DrtStayTask waitTask = taskFactory.createStayTask(vehicle, departureTask.getEndTime(),
					earliestNextStartTime, waitLink);
			schedule.addTask(departureTask.getTaskIdx() + 1, waitTask);
			return waitTask;
		}

		return departureTask;
	}

	private Task insertDriveWithWait(DvrpVehicle vehicle, Task departureTask, VrpPathWithTravelData path, double latestArrivalTime) {
		Schedule schedule = vehicle.getSchedule();

		Task leadingTask = departureTask;

		if (scheduleWaitBeforeDrive) {
			double driveDepartureTime = latestArrivalTime - path.getTravelTime();

			if (driveDepartureTime > departureTask.getEndTime()) {
				// makes sense to insert a wait task before departure
				DrtStayTask waitTask = taskFactory.createStayTask(vehicle, departureTask.getEndTime(),
						driveDepartureTime, path.getFromLink());
				schedule.addTask(departureTask.getTaskIdx() + 1, waitTask);

				path = path.withDepartureTime(driveDepartureTime);
				leadingTask = waitTask;
			}
		}

		Task driveTask = taskFactory.createDriveTask(vehicle, path, DrtDriveTask.TYPE);
		schedule.addTask(leadingTask.getTaskIdx() + 1, driveTask);

		if (driveTask.getEndTime() < latestArrivalTime && !scheduleWaitBeforeDrive) {
			DrtStayTask waitTask = taskFactory.createStayTask(vehicle, driveTask.getEndTime(), latestArrivalTime,
					path.getToLink());
			schedule.addTask(driveTask.getTaskIdx() + 1, waitTask);
			return waitTask;
		} else {
			return driveTask;
		}
	}
}
