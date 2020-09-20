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
public class TaskEndedEvent extends AbstractTaskEvent {
	public static final String EVENT_TYPE = "dvrpTaskEnded";

	public TaskEndedEvent(double time, String dvrpMode, Id<DvrpVehicle> dvrpVehicleId, Id<Person> driverId, Task task) {
		this(time, dvrpMode, dvrpVehicleId, driverId, task.getTaskType(), task.getTaskIdx(),
				Tasks.getEndLink(task).getId());
	}

	public TaskEndedEvent(double time, String dvrpMode, Id<DvrpVehicle> dvrpVehicleId, Id<Person> driverId,
			Task.TaskType taskType, int taskIndex, Id<Link> linkId) {
		super(time, dvrpMode, dvrpVehicleId, driverId, taskType, taskIndex, linkId);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
}
