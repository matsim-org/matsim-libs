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

package org.matsim.contrib.taxi.schedule;

import org.matsim.contrib.dvrp.schedule.Task;

/**
 * Defines the base set of task types supported by the default optimiser
 *
 * @author Michal Maciejewski (michalm)
 */
public enum TaxiTaskBaseType {
	// not directly related to any customer (although may be related to serving a customer; e.g. a pickup drive)
	EMPTY_DRIVE, //
	// serving a customer (TaxiTaskWithRequest)
	PICKUP, OCCUPIED_DRIVE, DROPOFF,//
	// not directly related to any customer
	STAY;

	public static TaxiTaskBaseType getBaseTypeOrElseThrow(Task task) {
		return ((TaxiTaskType)task.getTaskType()).getBaseType()
				.orElseThrow(() -> new IllegalArgumentException("Task: " + task + "does not have a base type"));
	}

	public boolean isBaseTypeOf(Task task) {
		return ((TaxiTaskType)task.getTaskType()).getBaseType().orElse(null) == this;
	}
}
