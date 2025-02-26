/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.schedule;

import static org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;

import java.util.List;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.tracker.TaskTrackers;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class ScheduleTimingUpdater {
	public interface StayTaskEndTimeCalculator {
		double calcNewEndTime(DvrpVehicle vehicle, StayTask task, double newBeginTime);
	}

	private final MobsimTimer timer;
	private final StayTaskEndTimeCalculator stayTaskEndTimeCalculator;
	private final DriveTaskUpdater driveTaskUpdater;

	public final static double REMOVE_STAY_TASK = Double.NEGATIVE_INFINITY;

	public ScheduleTimingUpdater(MobsimTimer timer, StayTaskEndTimeCalculator stayTaskEndTimeCalculator, DriveTaskUpdater driveTaskUpdater) {
		this.timer = timer;
		this.stayTaskEndTimeCalculator = stayTaskEndTimeCalculator;
		this.driveTaskUpdater = driveTaskUpdater;
	}

	/**
	 * This method should be called inside {@code VrpOptimizer.nextTask()} before anything else is done.
	 * Check and decide if the schedule should be updated due to if vehicle is Update timings (i.e. beginTime and
	 * endTime) of all tasks in the schedule.
	 */
	public void updateBeforeNextTask(DvrpVehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		// Assumption: there is no delay as long as the schedule has not been started (PLANNED)
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		updateTimingsStartingFromCurrentTask(vehicle, timer.getTimeOfDay());
	}

	public void updateTimings(DvrpVehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() != ScheduleStatus.STARTED) {
			return;
		}

		if (schedule.getCurrentTask() instanceof DriveTask driveTask) {
			driveTaskUpdater.updateCurrentDriveTask(vehicle, driveTask);
		}

		double predictedEndTime = TaskTrackers.predictEndTime(schedule.getCurrentTask(), timer.getTimeOfDay());
		updateTimingsStartingFromCurrentTask(vehicle, predictedEndTime);
	}

	private void updateTimingsStartingFromCurrentTask(DvrpVehicle vehicle, double newEndTime) {
		Schedule schedule = vehicle.getSchedule();
		Task currentTask = schedule.getCurrentTask();
		if (currentTask.getEndTime() != newEndTime || driveTaskUpdater != DriveTaskUpdater.NOOP) {
			currentTask.setEndTime(newEndTime);
			updateTimingsStartingFromTaskIdx(vehicle, currentTask.getTaskIdx() + 1, newEndTime);
		}
	}

	public void updateTimingsStartingFromTaskIdx(DvrpVehicle vehicle, int startIdx, double newBeginTime) {
		Schedule schedule = vehicle.getSchedule();
		List<? extends Task> tasks = schedule.getTasks();

		for (int i = startIdx; i < tasks.size(); i++) {
			double calcEndTime = calcNewEndTime(vehicle, tasks.get(i), newBeginTime);

			Task task = tasks.get(i);
			if (calcEndTime == REMOVE_STAY_TASK) {
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

	private double calcNewEndTime(DvrpVehicle vehicle, Task task, double newBeginTime) {
		if (task instanceof DriveTask driveTask) {
			// depending on the implementation, update the path
			driveTaskUpdater.updatePlannedDriveTask(vehicle, driveTask, newBeginTime);
			
			VrpPathWithTravelData path = (VrpPathWithTravelData)((DriveTask)task).getPath();
			return newBeginTime + path.getTravelTime();
		} else {
			return stayTaskEndTimeCalculator.calcNewEndTime(vehicle, (StayTask)task, newBeginTime);
		}
	}
}
