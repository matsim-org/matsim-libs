/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.extension.services.tasks;

import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.evrp.ChargingTask;

import java.util.Set;

/**
 * @author steffenaxer
 */
public class DefaultStackableTasksImpl implements StackableTasks {
	private final static Set<Class<? extends Task>> STACKABLE_TASKS = Set.of(DrtServiceTask.class, ChargingTask.class);

	@Override
	public boolean isStackableTask(Task task) {
		return STACKABLE_TASKS.stream().anyMatch(t -> t.isAssignableFrom(task.getClass()) );
	}
}
