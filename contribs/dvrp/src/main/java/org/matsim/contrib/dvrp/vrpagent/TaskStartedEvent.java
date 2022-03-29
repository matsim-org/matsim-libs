/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.vrpagent;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Tasks;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TaskStartedEvent extends AbstractTaskEvent {
	public static final String EVENT_TYPE = "dvrpTaskStarted";

	public TaskStartedEvent(double time, String dvrpMode, Id<DvrpVehicle> dvrpVehicleId, Id<Person> driverId,
			Task task) {
		this(time, dvrpMode, dvrpVehicleId, driverId, task.getTaskType(), task.getTaskIdx(),
				Tasks.getBeginLink(task).getId());
	}

	public TaskStartedEvent(double time, String dvrpMode, Id<DvrpVehicle> dvrpVehicleId, Id<Person> driverId,
			Task.TaskType taskType, int taskIndex, Id<Link> linkId) {
		super(time, dvrpMode, dvrpVehicleId, driverId, taskType, taskIndex, linkId);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public static TaskStartedEvent convert(GenericEvent event, Function<String, Task.TaskType> taskTypeFunction) {
		Map<String, String> attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		String mode = Objects.requireNonNull(attributes.get(ATTRIBUTE_DVRP_MODE));
		Id<DvrpVehicle> vehicleId = Id.create(attributes.get(ATTRIBUTE_DVRP_VEHICLE), DvrpVehicle.class);
		Id<Person> driverId = Id.createPersonId(attributes.get(ATTRIBUTE_PERSON));
		Task.TaskType taskType = Objects.requireNonNull(taskTypeFunction.apply(attributes.get(ATTRIBUTE_TASK_TYPE)));
		int taskIndex = Integer.parseInt(attributes.get(ATTRIBUTE_TASK_INDEX));
		Id<Link> linkId = Id.createLinkId(attributes.get(ATTRIBUTE_LINK));
		return new TaskStartedEvent(time, mode, vehicleId, driverId, taskType, taskIndex, linkId);
	}
}
