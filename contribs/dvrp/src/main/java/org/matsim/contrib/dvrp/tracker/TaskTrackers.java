/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.tracker;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.dvrp.vrpagent.VrpLeg;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * General assumptions:
 * <ul>
 * <li>An offline tracker knows/uses only the corresponding task and the schedule (i.e. plan)</li>
 * <li>An online tracker knows/uses also the corresponding {@link DynAction} (i.e. execution)</li>
 * </ul>
 * 
 * @author michalm
 */
public class TaskTrackers {
	public static void initOnlineDriveTaskTracking(Vehicle vehicle, VrpLeg vrpDynLeg,
			VrpOptimizerWithOnlineTracking optimizer, MobsimTimer timer) {
		initOnlineDriveTaskTracking(vehicle, vrpDynLeg,
				new OnlineDriveTaskTrackerImpl(vehicle, vrpDynLeg, optimizer, timer));
	}

	public static void initOnlineDriveTaskTracking(Vehicle vehicle, VrpLeg vrpDynLeg,
			OnlineDriveTaskTracker onlineTracker) {
		DriveTask driveTask = (DriveTask)vehicle.getSchedule().getCurrentTask();
		driveTask.initTaskTracker(onlineTracker);
		vrpDynLeg.initOnlineTracking(onlineTracker);
	}

	public static void initOnlineStayTaskTracking(StayTask stayTask, final DynActivity dynActivity) {
		stayTask.initTaskTracker(() -> dynActivity.getEndTime());
	}

	public static void initOfflineTaskTracking(final Task task, final MobsimTimer timer) {
		task.initTaskTracker(() -> TaskTrackers.predictEndTimeOffline(task, timer.getTimeOfDay()));
	}

	public static double predictEndTime(Task task, double currentTime) {
		if (task.getStatus() != TaskStatus.STARTED) {
			throw new IllegalStateException();
		}

		TaskTracker tracker = task.getTaskTracker();
		return tracker != null ? //
				tracker.predictEndTime() : predictEndTimeOffline(task, currentTime);
	}

	private static double predictEndTimeOffline(Task task, double currentTime) {
		return Math.max(task.getEndTime(), currentTime);
	}
}
