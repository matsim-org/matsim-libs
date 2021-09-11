/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ExecutedScheduleCollector implements TaskStartedEventHandler, TaskEndedEventHandler {
	public static class ExecutedSchedule {
		public final Id<DvrpVehicle> vehicleId;
		private final List<ExecutedTask> executedTasks = new ArrayList<>();

		public ExecutedSchedule(Id<DvrpVehicle> vehicleId) {
			this.vehicleId = vehicleId;
		}

		public List<ExecutedTask> getExecutedTasks() {
			// so we can safely share the task list without worrying about external changes to the list
			return Collections.unmodifiableList(executedTasks);
		}
	}

	public static ExecutedScheduleCollector createWithDefaultTaskCreator(String mode) {
		return new ExecutedScheduleCollector(mode,
				(taskStarted, taskEnded) -> new ExecutedTask(taskStarted.getTaskType(), taskStarted.getTime(),
						taskEnded.getTime(), taskStarted.getLinkId(), taskEnded.getLinkId()));
	}

	private final String mode;
	private final BiFunction<TaskStartedEvent, TaskEndedEvent, ExecutedTask> executedTaskCreator;

	private final Map<Id<DvrpVehicle>, TaskStartedEvent> taskStartedEvents = new HashMap<>();
	private final Map<Id<DvrpVehicle>, ExecutedSchedule> executedSchedules = new HashMap<>();

	public ExecutedScheduleCollector(String mode,
			BiFunction<TaskStartedEvent, TaskEndedEvent, ExecutedTask> executedTaskCreator) {
		this.mode = mode;
		this.executedTaskCreator = executedTaskCreator;
	}

	public Collection<ExecutedSchedule> getExecutedSchedules() {
		return executedSchedules.values();
	}

	@Override
	public void handleEvent(TaskStartedEvent startEvent) {
		if (!startEvent.getDvrpMode().equals(mode)) {
			return;
		}

		var previousEvent = taskStartedEvents.put(startEvent.getDvrpVehicleId(), startEvent);
		Preconditions.checkState(previousEvent == null, "No end event reported for previous task start event: (%s).",
				previousEvent);
	}

	@Override
	public void handleEvent(TaskEndedEvent endEvent) {
		if (!endEvent.getDvrpMode().equals(mode)) {
			return;
		}

		var startEvent = taskStartedEvents.remove(endEvent.getDvrpVehicleId());
		Preconditions.checkNotNull(startEvent, "No start event reported for task end event: (%s).", startEvent);
		Preconditions.checkArgument(startEvent.getTaskType().equals(endEvent.getTaskType()),
				"Start event (%s) and end event (%s) refer to tasks of different types", startEvent, endEvent);

		executedSchedules.computeIfAbsent(endEvent.getDvrpVehicleId(), ExecutedSchedule::new) //
				.executedTasks.add(executedTaskCreator.apply(startEvent, endEvent));
	}

	@Override
	public void reset(int iteration) {
		executedSchedules.clear();
	}
}
