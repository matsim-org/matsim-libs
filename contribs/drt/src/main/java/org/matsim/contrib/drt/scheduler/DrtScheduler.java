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

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleData.Stop;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.drt.schedule.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.Vehicles;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.TaskTrackers;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class DrtScheduler implements ScheduleInquiry {
	private final Fleet fleet;
	private final double stopDuration;
	private final MobsimTimer timer;
	private final TravelTime travelTime;

	@Inject
	public DrtScheduler(DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer timer,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime) {
		this.fleet = fleet;
		this.stopDuration = drtCfg.getStopDuration();
		this.timer = timer;
		this.travelTime = travelTime;

		initFleet(drtCfg);
	}

	private void initFleet(DrtConfigGroup drtCfg) {
		if (drtCfg.isChangeStartLinkToLastLinkInSchedule()) {
			for (Vehicle veh : fleet.getVehicles().values()) {
				Vehicles.changeStartLinkToLastLinkInSchedule(veh);
			}
		}

		((FleetImpl)fleet).resetSchedules();
		for (Vehicle veh : fleet.getVehicles().values()) {
			veh.getSchedule()
					.addTask(new DrtStayTask(veh.getServiceBeginTime(), veh.getServiceEndTime(), veh.getStartLink()));
		}
	}

	@Override
	public boolean isIdle(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (timer.getTimeOfDay() >= vehicle.getServiceEndTime() || schedule.getStatus() != ScheduleStatus.STARTED) {
			return false;
		}

		DrtTask currentTask = (DrtTask)schedule.getCurrentTask();
		return currentTask.getTaskIdx() == schedule.getTaskCount() - 1 // last task (because no prebooking)
				&& currentTask.getDrtTaskType() == DrtTaskType.STAY;
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

		updateTimingsStartingFromCurrentTask(vehicle, timer.getTimeOfDay());
	}

	public void updateTimings(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		double predictedEndTime = TaskTrackers.predictEndTime(schedule.getCurrentTask(), timer.getTimeOfDay());
		updateTimingsStartingFromCurrentTask(vehicle, predictedEndTime);
	}

	private void updateTimingsStartingFromCurrentTask(Vehicle vehicle, double newEndTime) {
		Schedule schedule = vehicle.getSchedule();
		Task currentTask = schedule.getCurrentTask();
		if (currentTask.getEndTime() != newEndTime) {
			currentTask.setEndTime(newEndTime);
			updateTimingsStartingFromTaskIdx(vehicle, currentTask.getTaskIdx() + 1, newEndTime);
		}
	}

	private void updateTimingsStartingFromTaskIdx(Vehicle vehicle, int startIdx, double newBeginTime) {
		Schedule schedule = vehicle.getSchedule();
		List<? extends Task> tasks = schedule.getTasks();

		for (int i = startIdx; i < tasks.size(); i++) {
			DrtTask task = (DrtTask)tasks.get(i);
			double calcEndTime = calcNewEndTime(vehicle, task, newBeginTime);

			if (Time.isUndefinedTime(calcEndTime)) {
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

	private double calcNewEndTime(Vehicle vehicle, DrtTask task, double newBeginTime) {
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
				double duration = stopDuration;
				return newBeginTime + duration;
			}

			default:
				throw new IllegalStateException();
		}
	}

	// =========================================================================================

	public void insertRequest(VehicleData.Entry vehicleEntry, DrtRequest request, Insertion insertion) {
		insertPickup(vehicleEntry, request, insertion);
		insertDropoff(vehicleEntry, request, insertion);
	}

	private void insertPickup(VehicleData.Entry vehicleEntry, DrtRequest request, Insertion insertion) {
		Schedule schedule = vehicleEntry.vehicle.getSchedule();
		List<Stop> stops = vehicleEntry.stops;

		DrtTask currentTask = (DrtTask)schedule.getCurrentTask();
		Task beforePickupTask;

		if (insertion.pickupIdx == 0 && currentTask.getDrtTaskType() == DrtTaskType.DRIVE) {
			LinkTimePair diversion = ((OnlineDriveTaskTracker)currentTask.getTaskTracker()).getDiversionPoint();
			if (diversion != null) { // divert vehicle
				beforePickupTask = currentTask;
				VrpPathWithTravelData vrpPath = VrpPaths.createPath(vehicleEntry.start.link, request.getFromLink(),
						vehicleEntry.start.time, insertion.pathToPickup, travelTime);
				((OnlineDriveTaskTracker)beforePickupTask.getTaskTracker()).divertPath(vrpPath);
			} else { // too late for diversion
				if (request.getFromLink() != vehicleEntry.start.link) { // add a new drive task
					VrpPathWithTravelData vrpPath = VrpPaths.createPath(vehicleEntry.start.link, request.getFromLink(),
							vehicleEntry.start.time, insertion.pathToPickup, travelTime);
					beforePickupTask = new DrtDriveTask(vrpPath);
					schedule.addTask(currentTask.getTaskIdx() + 1, beforePickupTask);
				} else { // no need for a new drive task
					beforePickupTask = currentTask;
				}
			}
		} else { // insert pickup after an existing stop/stay task
			DrtStayTask stayTask = null;
			DrtStopTask stopTask = null;
			if (insertion.pickupIdx == 0) {
				if (currentTask.getDrtTaskType() == DrtTaskType.STAY) {
					stayTask = (DrtStayTask)currentTask; // ongoing stay task
					double now = timer.getTimeOfDay();
					if (stayTask.getEndTime() > now) { // stop stay task; a new stop/drive task can be inserted now
						stayTask.setEndTime(now);
					}
				} else {
					stopTask = (DrtStopTask)currentTask; // ongoing stop task
				}
			} else {
				stopTask = stops.get(insertion.pickupIdx - 1).task; // future stop task
			}

			if (stopTask != null && request.getFromLink() == stopTask.getLink()) { // no detour; no new stop task
				// add pickup request to stop task
				stopTask.addPickupRequest(request);
				request.setPickupTask(stopTask);

				/// ADDED
				//// TODO this is copied, but has not been updated !!!!!!!!!!!!!!!
				// add drive from pickup
				if (insertion.pickupIdx == insertion.dropoffIdx) {
					// remove drive i->i+1 (if there is one)
					if (insertion.pickupIdx < stops.size()) {// there is at least one following stop
						DrtStopTask nextStopTask = stops.get(insertion.pickupIdx).task;
						if (stopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx()) {// there must a drive task in
																						// between
							throw new RuntimeException();
						}
						if (stopTask.getTaskIdx() + 2 == nextStopTask.getTaskIdx()) {// there must a drive task in
																						// between
							int driveTaskIdx = stopTask.getTaskIdx() + 1;
							schedule.removeTask(schedule.getTasks().get(driveTaskIdx));
						}
					}

					Link toLink = request.getToLink(); // pickup->dropoff

					VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getFromLink(), toLink,
							stopTask.getEndTime(), insertion.pathFromPickup, travelTime);
					Task driveFromPickupTask = new DrtDriveTask(vrpPath);
					schedule.addTask(stopTask.getTaskIdx() + 1, driveFromPickupTask);

					// update timings
					// TODO should be enough to update the timeline only till dropoffIdx...
					updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, stopTask.getTaskIdx() + 2,
							driveFromPickupTask.getEndTime());
					///////
				}

				return;
			} else {
				StayTask stayOrStopTask = stayTask != null ? stayTask : stopTask;

				// remove drive i->i+1 (if there is one)
				if (insertion.pickupIdx < stops.size()) {// there is at least one following stop
					DrtStopTask nextStopTask = stops.get(insertion.pickupIdx).task;

					// check: if there is at most one drive task in between
					if (stayOrStopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx() //
							&& stayTask != null && stayTask.getTaskIdx() + 1 != nextStopTask.getTaskIdx()) {
						throw new RuntimeException();
					}
					if (stayOrStopTask.getTaskIdx() + 2 == nextStopTask.getTaskIdx()) {
						// removing the drive task that is in between
						int driveTaskIdx = stayOrStopTask.getTaskIdx() + 1;
						schedule.removeTask(schedule.getTasks().get(driveTaskIdx));
					}
				}

				if (stayTask != null && request.getFromLink() == stayTask.getLink()) {
					// the bus stays where it is
					beforePickupTask = stayTask;
				} else {// add drive task to pickup location
					// insert drive i->pickup
					VrpPathWithTravelData vrpPath = VrpPaths.createPath(stayOrStopTask.getLink(), request.getFromLink(),
							stayOrStopTask.getEndTime(), insertion.pathToPickup, travelTime);
					beforePickupTask = new DrtDriveTask(vrpPath);
					schedule.addTask(stayOrStopTask.getTaskIdx() + 1, beforePickupTask);
				}
			}
		}

		// insert pickup stop task
		double startTime = beforePickupTask.getEndTime();
		int taskIdx = beforePickupTask.getTaskIdx() + 1;
		DrtStopTask pickupStopTask = new DrtStopTask(startTime, startTime + stopDuration, request.getFromLink());
		schedule.addTask(taskIdx, pickupStopTask);
		pickupStopTask.addPickupRequest(request);
		request.setPickupTask(pickupStopTask);

		// add drive from pickup
		Link toLink = insertion.pickupIdx == insertion.dropoffIdx ? request.getToLink() // pickup->dropoff
				: stops.get(insertion.pickupIdx).task.getLink(); // pickup->i+1

		VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getFromLink(), toLink, startTime + stopDuration,
				insertion.pathFromPickup, travelTime);
		Task driveFromPickupTask = new DrtDriveTask(vrpPath);
		schedule.addTask(taskIdx + 1, driveFromPickupTask);

		// update timings
		// TODO should be enough to update the timeline only till dropoffIdx...
		updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, taskIdx + 2, driveFromPickupTask.getEndTime());
	}

	private void insertDropoff(VehicleData.Entry vehicleEntry, DrtRequest request, Insertion insertion) {
		Schedule schedule = vehicleEntry.vehicle.getSchedule();
		List<Stop> stops = vehicleEntry.stops;

		Task driveToDropoffTask;
		if (insertion.pickupIdx == insertion.dropoffIdx) { // no drive to dropoff
			int pickupTaskIdx = request.getPickupTask().getTaskIdx();
			driveToDropoffTask = schedule.getTasks().get(pickupTaskIdx + 1);
		} else {
			DrtStopTask stopTask = stops.get(insertion.dropoffIdx - 1).task;
			if (request.getToLink() == stopTask.getLink()) { // no detour; no new stop task
				// add dropoff request to stop task
				stopTask.addDropoffRequest(request);
				request.setDropoffTask(stopTask);
				return;
			} else { // add drive task to dropoff location

				// remove drive j->j+1 (if j is not the last stop)
				if (insertion.dropoffIdx < stops.size()) {
					DrtStopTask nextStopTask = stops.get(insertion.dropoffIdx).task;
					if (stopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx()) {
						throw new IllegalStateException();
					}
					int driveTaskIdx = stopTask.getTaskIdx() + 1;
					schedule.removeTask(schedule.getTasks().get(driveTaskIdx));
				}

				// insert drive i->dropoff
				VrpPathWithTravelData vrpPath = VrpPaths.createPath(stopTask.getLink(), request.getToLink(),
						stopTask.getEndTime(), insertion.pathToDropoff, travelTime);
				driveToDropoffTask = new DrtDriveTask(vrpPath);
				schedule.addTask(stopTask.getTaskIdx() + 1, driveToDropoffTask);
			}
		}

		// insert dropoff stop task
		double startTime = driveToDropoffTask.getEndTime();
		int taskIdx = driveToDropoffTask.getTaskIdx() + 1;
		DrtStopTask dropoffStopTask = new DrtStopTask(startTime, startTime + stopDuration, request.getToLink());
		schedule.addTask(taskIdx, dropoffStopTask);
		dropoffStopTask.addDropoffRequest(request);
		request.setDropoffTask(dropoffStopTask);

		// add drive from dropoff
		if (insertion.dropoffIdx == stops.size()) {// bus stays at dropoff
			if (taskIdx + 2 == schedule.getTaskCount()) {// remove stay task from the end of schedule,
				DrtStayTask oldStayTask = (DrtStayTask)schedule.getTasks().get(taskIdx + 1);
				schedule.removeTask(oldStayTask);
			}
			if (taskIdx + 1 == schedule.getTaskCount()) {

				schedule.addTask(new DrtStayTask(dropoffStopTask.getEndTime(), vehicleEntry.vehicle.getServiceEndTime(),
						dropoffStopTask.getLink()));
			} else {
				throw new RuntimeException();
			}
		} else {
			Link toLink = stops.get(insertion.dropoffIdx).task.getLink(); // dropoff->j+1

			VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getToLink(), toLink, startTime + stopDuration,
					insertion.pathFromDropoff, travelTime);
			Task driveFromDropoffTask = new DrtDriveTask(vrpPath);
			schedule.addTask(taskIdx + 1, driveFromDropoffTask);

			// update timings
			updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, taskIdx + 2, driveFromDropoffTask.getEndTime());
		}
	}

	public void relocateEmptyVehicle(Vehicle vehicle, VrpPathWithTravelData vrpPath) {
		Schedule schedule = vehicle.getSchedule();
		DrtStayTask stayTask = (DrtStayTask)schedule.getCurrentTask();
		if (stayTask.getTaskIdx() != schedule.getTaskCount() - 1) {
			throw new IllegalStateException("The current STAY task is not last. Not possible without prebooking");
		}

		if (vrpPath.getDepartureTime() < timer.getTimeOfDay()) {
			throw new IllegalArgumentException("Too late. Planned departureTime=" + vrpPath.getDepartureTime()
					+ " currentTime=" + timer.getTimeOfDay());
		}

		stayTask.setEndTime(vrpPath.getDepartureTime()); // finish STAY
		schedule.addTask(new DrtDriveTask(vrpPath)); // add DRIVE
		// append STAY
		schedule.addTask(new DrtStayTask(vrpPath.getArrivalTime(), vehicle.getServiceEndTime(), vrpPath.getToLink()));
	}
}
