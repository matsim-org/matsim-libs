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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task.TaskType;
import org.matsim.api.core.v01.events.HasPersonId;

/**
 * @author Michal Maciejewski (michalm)
 */
public abstract class AbstractTaskEvent extends Event implements HasPersonId, HasLinkId {
	public static final String ATTRIBUTE_DVRP_VEHICLE = "dvrpVehicle";
	public static final String ATTRIBUTE_TASK_TYPE = "taskType";
	public static final String ATTRIBUTE_TASK_INDEX = "taskIndex";
	public static final String ATTRIBUTE_DVRP_MODE = "dvrpMode";

	private final String dvrpMode;
	private final Id<DvrpVehicle> dvrpVehicleId;
	private final Id<Person> driverId;
	private final TaskType taskType;
	private final int taskIndex;
	private final Id<Link> linkId;

	protected AbstractTaskEvent(double time, String dvrpMode, Id<DvrpVehicle> dvrpVehicleId, Id<Person> driverId,
			TaskType taskType, int taskIndex, Id<Link> linkId) {
		super(time);
		this.dvrpMode = dvrpMode;
		this.dvrpVehicleId = dvrpVehicleId;
		this.driverId = driverId;
		this.taskType = taskType;
		this.taskIndex = taskIndex;
		this.linkId = linkId;
	}

	public final Id<DvrpVehicle> getDvrpVehicleId() {
		return dvrpVehicleId;
	}

	@Override
	public Id<Person> getPersonId() {
		return driverId;
	}

	public final TaskType getTaskType() {
		return taskType;
	}

	public final int getTaskIndex() {
		return taskIndex;
	}

	public String getDvrpMode() {
		return dvrpMode;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_DVRP_VEHICLE, dvrpVehicleId + "");
		attr.put(ATTRIBUTE_TASK_TYPE, taskType.name());
		attr.put(ATTRIBUTE_TASK_INDEX, taskIndex + "");
		attr.put(ATTRIBUTE_DVRP_MODE, dvrpMode + "");
		return attr;
	}
}
