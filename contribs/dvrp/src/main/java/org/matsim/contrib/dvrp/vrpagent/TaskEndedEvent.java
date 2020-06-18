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

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TaskEndedEvent extends AbstractTaskEvent {
	public static final String EVENT_TYPE = "dvrpTaskEnded";

	public TaskEndedEvent(double time, Id<DvrpVehicle> dvrpVehicleId, Task.TaskType taskType, int taskIndex) {
		super(time, dvrpVehicleId, taskType, taskIndex);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
}
