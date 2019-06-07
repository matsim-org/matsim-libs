/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;

public class Schedules {
	public static final Comparator<Task> TASK_SCHEDULE_IDX_COMPARATOR = (t1, t2) -> t1.getTaskIdx() - t2.getTaskIdx();

	public static Task getFirstTask(Schedule schedule) {
		return schedule.getTasks().get(0);
	}

	public static Task getSecondTask(Schedule schedule) {
		return schedule.getTasks().get(1);
	}

	public static Task getNextToLastTask(Schedule schedule) {
		List<? extends Task> tasks = schedule.getTasks();
		return tasks.get(tasks.size() - 2);
	}

	public static Task getLastTask(Schedule schedule) {
		List<? extends Task> tasks = schedule.getTasks();
		return tasks.get(tasks.size() - 1);
	}

	public static Task getNextTask(Schedule schedule) {
		int taskIdx = schedule.getStatus() == ScheduleStatus.PLANNED ? //
				0 : schedule.getCurrentTask().getTaskIdx() + 1;
		return schedule.getTasks().get(taskIdx);
	}

	public static Task getPreviousTask(Schedule schedule) {
		int taskIdx = schedule.getStatus() == ScheduleStatus.COMPLETED ? //
				schedule.getTaskCount() - 1 : schedule.getCurrentTask().getTaskIdx() - 1;
		return schedule.getTasks().get(taskIdx);
	}

	public static Link getLastLinkInSchedule(DvrpVehicle vehicle) {
		List<? extends Task> tasks = vehicle.getSchedule().getTasks();
		return tasks.isEmpty() ? //
				vehicle.getStartLink() : //
				Tasks.getEndLink(tasks.get(tasks.size() - 1));
	}

	@SuppressWarnings("unchecked")
	public static Stream<StayTask> stayTasks(Schedule schedule) {
		return (Stream<StayTask>)schedule.tasks().filter(t -> t instanceof StayTask);
	}

	@SuppressWarnings("unchecked")
	public static Stream<DriveTask> driveTasks(Schedule schedule) {
		return (Stream<DriveTask>)schedule.tasks().filter(t -> t instanceof DriveTask);
	}
}
