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

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.name.Named;

/**
 * @author michalm
 */
public class RequestInsertionScheduler {
	private final Fleet fleet;
	private final double stopDuration;
	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final ScheduleTimingUpdater scheduleTimingUpdater;
	private final DrtTaskFactory taskFactory;

	public RequestInsertionScheduler(DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer timer,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			ScheduleTimingUpdater scheduleTimingUpdater, DrtTaskFactory taskFactory) {
		this.fleet = fleet;
		this.stopDuration = drtCfg.getStopDuration();
		this.timer = timer;
		this.travelTime = travelTime;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.taskFactory = taskFactory;
		initSchedules();
	}

	public void initSchedules() {
		for (DvrpVehicle veh : fleet.getVehicles().values()) {
			veh.getSchedule()
					.addTask(taskFactory.createStayTask(veh, veh.getServiceBeginTime(), veh.getServiceEndTime(),
							veh.getStartLink()));
		}
	}

	public void scheduleRequest(DrtRequest request, InsertionWithDetourData<PathData> insertion) {
		insertPickup(request, insertion);
		insertDropoff(request, insertion);
	}

	private void insertPickup(DrtRequest request, InsertionWithDetourData<PathData> insertion) {
		VehicleData.Entry vehicleEntry = insertion.getVehicleEntry();
		Schedule schedule = vehicleEntry.vehicle.getSchedule();
		List<Waypoint.Stop> stops = vehicleEntry.stops;
		int pickupIdx = insertion.getPickup().index;
		int dropoffIdx = insertion.getDropoff().index;

		ScheduleStatus scheduleStatus = schedule.getStatus();
		Task currentTask = scheduleStatus == ScheduleStatus.PLANNED ? null : schedule.getCurrentTask();
		Task beforePickupTask;

		if (pickupIdx == 0 && scheduleStatus != ScheduleStatus.PLANNED && DRIVE.isBaseTypeOf(currentTask)) {
			LinkTimePair diversion = ((OnlineDriveTaskTracker)currentTask.getTaskTracker()).getDiversionPoint();
			if (diversion != null) { // divert vehicle
				beforePickupTask = currentTask;
				VrpPathWithTravelData vrpPath = VrpPaths.createPath(vehicleEntry.start.link, request.getFromLink(),
						vehicleEntry.start.time, insertion.getDetourToPickup(), travelTime);
				((OnlineDriveTaskTracker)beforePickupTask.getTaskTracker()).divertPath(vrpPath);
			} else { // too late for diversion
				if (request.getFromLink() != vehicleEntry.start.link) { // add a new drive task
					VrpPathWithTravelData vrpPath = VrpPaths.createPath(vehicleEntry.start.link, request.getFromLink(),
							vehicleEntry.start.time, insertion.getDetourToPickup(), travelTime);
					beforePickupTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
					schedule.addTask(currentTask.getTaskIdx() + 1, beforePickupTask);
				} else { // no need for a new drive task
					beforePickupTask = currentTask;
				}
			}
		} else { // insert pickup after an existing stop/stay task
			DrtStayTask stayTask = null;
			DrtStopTask stopTask = null;
			if (pickupIdx == 0) {
				if (scheduleStatus == ScheduleStatus.PLANNED) {// PLANNED schedule
					stayTask = (DrtStayTask)schedule.getTasks().get(0);
					stayTask.setEndTime(stayTask.getBeginTime());// could get later removed with ScheduleTimingUpdater
				} else if (STAY.isBaseTypeOf(currentTask)) {
					stayTask = (DrtStayTask)currentTask; // ongoing stay task
					double now = timer.getTimeOfDay();
					if (stayTask.getEndTime() > now) { // stop stay task; a new stop/drive task can be inserted now
						stayTask.setEndTime(now);
					}
				} else {
					stopTask = (DrtStopTask)currentTask; // ongoing stop task
				}
			} else {
				stopTask = stops.get(pickupIdx - 1).task; // future stop task
			}

			if (stopTask != null && request.getFromLink() == stopTask.getLink()) { // no detour; no new stop task
				// add pickup request to stop task
				stopTask.addPickupRequest(request);
				request.setPickupTask(stopTask);
				stopTask.setEndTime(Math.max(stopTask.getBeginTime() + stopDuration, request.getEarliestStartTime()));

				/// ADDED
				//// TODO this is copied, but has not been updated !!!!!!!!!!!!!!!
				// add drive from pickup
				if (pickupIdx == dropoffIdx) {
					// remove drive i->i+1 (if there is one)
					if (pickupIdx < stops.size()) {// there is at least one following stop
						DrtStopTask nextStopTask = stops.get(pickupIdx).task;
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
							stopTask.getEndTime(), insertion.getDetourFromPickup(), travelTime);
					Task driveFromPickupTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath,
							DrtDriveTask.TYPE);
					schedule.addTask(stopTask.getTaskIdx() + 1, driveFromPickupTask);

					// update timings
					// TODO should be enough to update the timeline only till dropoffIdx...
					scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle,
							stopTask.getTaskIdx() + 2, driveFromPickupTask.getEndTime());
					///////
				}

				return;
			} else {
				StayTask stayOrStopTask = stayTask != null ? stayTask : stopTask;

				// remove drive i->i+1 (if there is one)
				if (pickupIdx < stops.size()) {// there is at least one following stop
					DrtStopTask nextStopTask = stops.get(pickupIdx).task;

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
							stayOrStopTask.getEndTime(), insertion.getDetourToPickup(), travelTime);
					beforePickupTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
					schedule.addTask(stayOrStopTask.getTaskIdx() + 1, beforePickupTask);
				}
			}
		}

		// insert pickup stop task
		double startTime = beforePickupTask.getEndTime();
		int taskIdx = beforePickupTask.getTaskIdx() + 1;
		DrtStopTask pickupStopTask = taskFactory.createStopTask(vehicleEntry.vehicle, startTime,
				Math.max(startTime + stopDuration, request.getEarliestStartTime()), request.getFromLink());
		schedule.addTask(taskIdx, pickupStopTask);
		pickupStopTask.addPickupRequest(request);
		request.setPickupTask(pickupStopTask);

		// add drive from pickup
		Link toLink = pickupIdx == dropoffIdx ? request.getToLink() // pickup->dropoff
				: stops.get(pickupIdx).task.getLink(); // pickup->i+1

		VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getFromLink(), toLink, pickupStopTask.getEndTime(),
				insertion.getDetourFromPickup(), travelTime);
		Task driveFromPickupTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
		schedule.addTask(taskIdx + 1, driveFromPickupTask);

		// update timings
		// TODO should be enough to update the timeline only till dropoffIdx...
		scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, taskIdx + 2,
				driveFromPickupTask.getEndTime());
	}

	private void insertDropoff(DrtRequest request, InsertionWithDetourData<PathData> insertion) {
		VehicleData.Entry vehicleEntry = insertion.getVehicleEntry();
		Schedule schedule = vehicleEntry.vehicle.getSchedule();
		List<Waypoint.Stop> stops = vehicleEntry.stops;
		int pickupIdx = insertion.getPickup().index;
		int dropoffIdx = insertion.getDropoff().index;

		Task driveToDropoffTask;
		if (pickupIdx == dropoffIdx) { // no drive to dropoff
			int pickupTaskIdx = request.getPickupTask().getTaskIdx();
			driveToDropoffTask = schedule.getTasks().get(pickupTaskIdx + 1);
		} else {
			DrtStopTask stopTask = stops.get(dropoffIdx - 1).task;
			if (request.getToLink() == stopTask.getLink()) { // no detour; no new stop task
				// add dropoff request to stop task
				stopTask.addDropoffRequest(request);
				request.setDropoffTask(stopTask);
				return;
			} else { // add drive task to dropoff location

				// remove drive j->j+1 (if j is not the last stop)
				if (dropoffIdx < stops.size()) {
					DrtStopTask nextStopTask = stops.get(dropoffIdx).task;
					if (stopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx()) {
						throw new IllegalStateException();
					}
					int driveTaskIdx = stopTask.getTaskIdx() + 1;
					schedule.removeTask(schedule.getTasks().get(driveTaskIdx));
				}

				// insert drive i->dropoff
				VrpPathWithTravelData vrpPath = VrpPaths.createPath(stopTask.getLink(), request.getToLink(),
						stopTask.getEndTime(), insertion.getDetourToDropoff(), travelTime);
				driveToDropoffTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
				schedule.addTask(stopTask.getTaskIdx() + 1, driveToDropoffTask);
			}
		}

		// insert dropoff stop task
		double startTime = driveToDropoffTask.getEndTime();
		int taskIdx = driveToDropoffTask.getTaskIdx() + 1;
		DrtStopTask dropoffStopTask = taskFactory.createStopTask(vehicleEntry.vehicle, startTime,
				startTime + stopDuration, request.getToLink());
		schedule.addTask(taskIdx, dropoffStopTask);
		dropoffStopTask.addDropoffRequest(request);
		request.setDropoffTask(dropoffStopTask);

		// add drive from dropoff
		if (dropoffIdx == stops.size()) {// bus stays at dropoff
			if (taskIdx + 2 == schedule.getTaskCount()) {// remove stay task from the end of schedule,
				DrtStayTask oldStayTask = (DrtStayTask)schedule.getTasks().get(taskIdx + 1);
				schedule.removeTask(oldStayTask);
			}
			if (taskIdx + 1 == schedule.getTaskCount()) {
				// no stay task at the end if the pickup follows the existing stay task
				schedule.addTask(taskFactory.createStayTask(vehicleEntry.vehicle, dropoffStopTask.getEndTime(),
						vehicleEntry.vehicle.getServiceEndTime(), dropoffStopTask.getLink()));
			} else {
				throw new RuntimeException();
			}
		} else {
			Link toLink = stops.get(dropoffIdx).task.getLink(); // dropoff->j+1

			VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getToLink(), toLink, startTime + stopDuration,
					insertion.getDetourFromDropoff(), travelTime);
			Task driveFromDropoffTask = taskFactory.createDriveTask(vehicleEntry.vehicle, vrpPath, DrtDriveTask.TYPE);
			schedule.addTask(taskIdx + 1, driveFromDropoffTask);

			// update timings
			scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, taskIdx + 2,
					driveFromDropoffTask.getEndTime());
		}
	}
}
