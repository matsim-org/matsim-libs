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

import java.util.Optional;

import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author Michal Maciejewski (michalm)
 */
public record DrtTaskType(String name, Optional<DrtTaskBaseType> baseType) implements Task.TaskType {
	public DrtTaskType(DrtTaskBaseType baseType) {
		this(baseType.name(), Optional.of(baseType));
	}

	public DrtTaskType(String name, DrtTaskBaseType baseType) {
		this(name, Optional.of(baseType));
	}

	// baseType can be empty if the task type requires a special handling which is not provided by the standard DRT algorithms
	public DrtTaskType(String name) {
		this(name, Optional.empty());
	}
}
