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

package org.matsim.contrib.drt.schedule;

import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author Michal Maciejewski (michalm)
 */
public enum DrtTaskBaseType {
	STAY, // idle
	STOP, // stopped to drop off and pick up passengers
	DRIVE; // driving with/without passengers

	public static DrtTaskBaseType getBaseTypeOrElseThrow(Task task) {
		return ((DrtTaskType)task.getTaskType()).getBaseType()
				.orElseThrow(() -> new IllegalArgumentException("Task: " + task + "does not have a base type"));
	}

	public boolean isBaseTypeOf(Task task) {
		return isBaseTypeOf(task.getTaskType());
	}

	public boolean isBaseTypeOf(Task.TaskType taskType) {
		return ((DrtTaskType)taskType).getBaseType().orElse(null) == this;
	}
}
