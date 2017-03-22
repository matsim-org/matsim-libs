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

package playground.michalm.drt.scheduler;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.*;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import playground.michalm.drt.data.NDrtRequest;
import playground.michalm.drt.optimizer.VehicleData;
import playground.michalm.drt.optimizer.VehicleData.Stop;
import playground.michalm.drt.optimizer.insertion.SingleVehicleInsertionProblem.Insertion;
import playground.michalm.drt.schedule.*;
import playground.michalm.drt.schedule.NDrtTask.NDrtTaskType;

/**
 * @author michalm
 */
public class DrtScheduler implements ScheduleInquiry {
	private final Fleet fleet;
	protected final DrtSchedulerParams params;
	private final MobsimTimer timer;
	private final TravelTime travelTime;

	public DrtScheduler(Scenario scenario, Fleet fleet, MobsimTimer timer, DrtSchedulerParams params,
			TravelTime travelTime) {
		this.fleet = fleet;
		this.params = params;
		this.timer = timer;
		this.travelTime = travelTime;

		initFleet(scenario);
	}

	private void initFleet(Scenario scenario) {
		if (TaxiConfigGroup.get(scenario.getConfig()).isChangeStartLinkToLastLinkInSchedule()) {
			for (Vehicle veh : fleet.getVehicles().values()) {
				Vehicles.changeStartLinkToLastLinkInSchedule(veh);
			}
		}

		((FleetImpl)fleet).resetSchedules();
		for (Vehicle veh : fleet.getVehicles().values()) {
			veh.getSchedule()
					.addTask(new NDrtStayTask(veh.getServiceBeginTime(), veh.getServiceEndTime(), veh.getStartLink()));
		}
	}

	public DrtSchedulerParams getParams() {
		return params;
	}

	@Override
	public boolean isIdle(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (timer.getTimeOfDay() >= vehicle.getServiceEndTime() || schedule.getStatus() != ScheduleStatus.STARTED) {
			return false;
		}

		NDrtTask currentTask = (NDrtTask)schedule.getCurrentTask();
		return currentTask.getTaskIdx() == schedule.getTaskCount() - 1 // last task (because no prebooking)
				&& currentTask.getDrtTaskType() == NDrtTaskType.STAY;
	}

	/**
	 * Check and decide if the schedule should be updated due to if vehicle is Update timings (i.e. beginTime and
	 * endTime) of all tasks in the schedule.
	 */
	public void updateBeforeNextTask(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		// Assumption: there is no delay as long as the schedule has not been started (PLANNED)
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		updateTimelineStartingFromCurrentTask(vehicle, timer.getTimeOfDay());
	}

	public void updateTimeline(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		double predictedEndTime = TaskTrackers.predictEndTime(schedule.getCurrentTask(), timer.getTimeOfDay());
		updateTimelineStartingFromCurrentTask(vehicle, predictedEndTime);
	}

	private void updateTimelineStartingFromCurrentTask(Vehicle vehicle, double newEndTime) {
		Schedule schedule = vehicle.getSchedule();
		Task currentTask = schedule.getCurrentTask();
		if (currentTask.getEndTime() != newEndTime) {
			currentTask.setEndTime(newEndTime);
			updateTimelineStartingFromTaskIdx(vehicle, currentTask.getTaskIdx() + 1, newEndTime);
		}
	}

	private void updateTimelineStartingFromTaskIdx(Vehicle vehicle, int startIdx, double newBeginTime) {
		Schedule schedule = vehicle.getSchedule();
		List<? extends Task> tasks = schedule.getTasks();

		for (int i = startIdx; i < tasks.size(); i++) {
			NDrtTask task = (NDrtTask)tasks.get(i);
			double calcEndTime = calcNewEndTime(vehicle, task, newBeginTime);

			if (calcEndTime == Time.UNDEFINED_TIME) {
				schedule.removeTask(task);
				i--;
			} else if (calcEndTime < newBeginTime) {// 0 s is fine (e.g. last 'wait')
				throw new IllegalStateException();
			} else {
				task.setBeginTime(newBeginTime);
				task.setEndTime(calcEndTime);
				newBeginTime = calcEndTime;
			}
		}
	}

	protected double calcNewEndTime(Vehicle vehicle, NDrtTask task, double newBeginTime) {
		switch (task.getDrtTaskType()) {
			case STAY: {
				if (Schedules.getLastTask(vehicle.getSchedule()).equals(task)) {// last task
					// even if endTime=beginTime, do not remove this task!!! A DRT schedule should end with WAIT
					return Math.max(newBeginTime, vehicle.getServiceEndTime());
				} else {
					// if this is not the last task then some other task (e.g. DRIVE or PICKUP)
					// must have been added at time submissionTime <= t
					double oldEndTime = task.getEndTime();
					if (oldEndTime <= newBeginTime) {// may happen if the previous task is delayed
						return Time.UNDEFINED_TIME;// remove the task
					} else {
						return oldEndTime;
					}
				}
			}

			case DRIVE: {
				// cannot be shortened/lengthen, therefore must be moved forward/backward
				VrpPathWithTravelData path = (VrpPathWithTravelData)((DriveTask)task).getPath();
				// TODO one may consider recalculation of SP!!!!
				return newBeginTime + path.getTravelTime();
			}

			case STOP: {
				// TODO does not consider prebooking!!!
				double duration = params.stopDuration;
				return newBeginTime + duration;
			}

			default:
				throw new IllegalStateException();
		}
	}

	// =========================================================================================

	public void insertRequest(VehicleData.Entry vehicleEntry, NDrtRequest request, Insertion insertion) {
		insertPickup(vehicleEntry, request, insertion);
		insertDropoff(vehicleEntry, request, insertion);
	}

	private void insertPickup(VehicleData.Entry vehicleEntry, NDrtRequest request, Insertion insertion) {
		Schedule schedule = vehicleEntry.vehicle.getSchedule();
		List<Stop> stops = vehicleEntry.stops;

		NDrtTask currentTask = (NDrtTask)schedule.getCurrentTask();
		Task beforePickupTask;

		if (insertion.pickupIdx == 0 && currentTask.getDrtTaskType() == NDrtTaskType.DRIVE) { // divert vehicle
			beforePickupTask = currentTask;
			VrpPathWithTravelData vrpPath = VrpPaths.createPath(vehicleEntry.start.link, request.getFromLink(),
					vehicleEntry.start.time, insertion.pathToPickup.path, travelTime);
			((OnlineDriveTaskTracker)beforePickupTask.getTaskTracker()).divertPath(vrpPath);
		} else { // insert pickup after an existing stop/stay task
			NDrtStayTask stayTask = null;
			NDrtStopTask stopTask = null;
			if (insertion.pickupIdx == 0) {
				if (currentTask.getDrtTaskType() == NDrtTaskType.STAY) {
					stayTask = (NDrtStayTask)currentTask; // ongoing stay task
					double oldEndTime = stayTask.getEndTime();
					double now = timer.getTimeOfDay();
					if (oldEndTime> now) { //split stay task into two, so that a new stop/drive task can be inserted now 
						stayTask.setEndTime(now);
						schedule.addTask(new NDrtStayTask(now, oldEndTime, stayTask.getLink()));
					}
				} else {
					stopTask = (NDrtStopTask)currentTask; // ongoing stop task
				}
			} else {
				stopTask = stops.get(insertion.pickupIdx - 1).task; // future stop task
			}

			if (stopTask != null && request.getFromLink() == stopTask.getLink()) { // no detour; no new stop task
				// add pickup request to stop task
				stopTask.addPickupRequest(request);
				request.setPickupTask(stopTask);
				return;
			} else if (stayTask != null && request.getFromLink() == stayTask.getLink()) {
				// the bus stays where it is
				beforePickupTask = stayTask;
			} else { // add drive task to pickup location

				// remove drive i->i+1 (if there is one)
				if (insertion.pickupIdx < stops.size()) {// there is at least one following stop
					NDrtStopTask nextStopTask = stops.get(insertion.pickupIdx).task;
					if (stopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx()) {// there must a drive task in between
						throw new RuntimeException();
					}
					if (stopTask.getTaskIdx() + 2 == nextStopTask.getTaskIdx()) {// there must a drive task in between
						int driveTaskIdx = stopTask.getTaskIdx() + 1;
						schedule.removeTask(schedule.getTasks().get(driveTaskIdx));
					}
				}

				// insert drive i->pickup
				StayTask stayOrStopTask = stayTask != null ? stayTask : stopTask;
				VrpPathWithTravelData vrpPath = VrpPaths.createPath(stayOrStopTask.getLink(), request.getFromLink(),
						stayOrStopTask.getEndTime(), insertion.pathToPickup.path, travelTime);
				beforePickupTask = new NDrtDriveTask(vrpPath);
				schedule.addTask(stayOrStopTask.getTaskIdx() + 1, beforePickupTask);
			}
		}

		// insert pickup stop task
		double startTime = beforePickupTask.getEndTime();
		int taskIdx = beforePickupTask.getTaskIdx() + 1;
		NDrtStopTask stopTask = new NDrtStopTask(startTime, startTime + params.stopDuration, request.getFromLink());
		schedule.addTask(taskIdx, stopTask);
		request.setPickupTask(stopTask);

		// add drive from pickup
		Link toLink = insertion.pickupIdx == insertion.dropoffIdx ? request.getToLink() // pickup->dropoff
				: stops.get(insertion.pickupIdx).task.getLink(); // pickup->i+1

		VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getFromLink(), toLink,
				startTime + params.stopDuration, insertion.pathFromPickup.path, travelTime);
		Task driveFromPickupTask = new NDrtDriveTask(vrpPath);
		schedule.addTask(taskIdx + 1, driveFromPickupTask);

		// update timings
		// TODO should be enough to update the timeline only till dropoffIdx...
		updateTimelineStartingFromTaskIdx(vehicleEntry.vehicle, taskIdx + 2, driveFromPickupTask.getEndTime());
	}

	private void insertDropoff(VehicleData.Entry vehicleEntry, NDrtRequest request, Insertion insertion) {
		Schedule schedule = vehicleEntry.vehicle.getSchedule();
		List<Stop> stops = vehicleEntry.stops;

		Task driveToDropoffTask;
		if (insertion.pickupIdx == insertion.dropoffIdx) { // no drive to dropoff
			int pickupTaskIdx = request.getPickupTask().getTaskIdx();
			driveToDropoffTask = schedule.getTasks().get(pickupTaskIdx + 1);
		} else {
			NDrtStopTask stopTask = stops.get(insertion.dropoffIdx - 1).task;
			if (request.getToLink() == stopTask.getLink()) { // no detour; no new stop task
				// add dropoff request to stop task
				stopTask.addDropoffRequest(request);
				request.setDropoffTask(stopTask);
				return;
			} else { // add drive task to dropoff location

				// remove drive j->j+1 (if j is not the last stop)
				if (insertion.dropoffIdx < stops.size()) {
					NDrtStopTask nextStopTask = stops.get(insertion.dropoffIdx).task;
					if (stopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx()) {
						throw new IllegalStateException();
					}
					int driveTaskIdx = stopTask.getTaskIdx() + 1;
					schedule.removeTask(schedule.getTasks().get(driveTaskIdx));
				}

				// insert drive i->dropoff
				VrpPathWithTravelData vrpPath = VrpPaths.createPath(stopTask.getLink(), request.getToLink(),
						stopTask.getEndTime(), insertion.pathToDropoff.path, travelTime);
				driveToDropoffTask = new NDrtDriveTask(vrpPath);
				schedule.addTask(stopTask.getTaskIdx() + 1, driveToDropoffTask);
			}
		}

		// insert dropoff stop task
		double startTime = driveToDropoffTask.getEndTime();
		int taskIdx = driveToDropoffTask.getTaskIdx() + 1;
		NDrtStopTask stopTask = new NDrtStopTask(startTime, startTime + params.stopDuration, request.getToLink());
		schedule.addTask(taskIdx, stopTask);
		request.setDropoffTask(stopTask);

		// add drive from dropoff
		if (insertion.dropoffIdx == stops.size()) {// bus stays at dropoff
			updateTimelineStartingFromTaskIdx(vehicleEntry.vehicle, taskIdx + 1, stopTask.getEndTime());
		} else {
			Link toLink = stops.get(insertion.dropoffIdx).task.getLink(); // dropoff->j+1

			VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getToLink(), toLink,
					startTime + params.stopDuration, insertion.pathFromDropoff.path, travelTime);
			Task driveFromDropoffTask = new NDrtDriveTask(vrpPath);
			schedule.addTask(taskIdx + 1, driveFromDropoffTask);

			// update timings
			updateTimelineStartingFromTaskIdx(vehicleEntry.vehicle, taskIdx + 2, driveFromDropoffTask.getEndTime());
		}
	}
}
