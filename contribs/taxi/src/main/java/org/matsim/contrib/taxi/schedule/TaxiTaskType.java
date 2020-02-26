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
 * @author Michal Maciejewski (michalm)
 */
public enum TaxiTaskType implements Task.TaskType {
	EMPTY_DRIVE, // not directly related to any customer (although may be related to serving a customer; e.g. a
	// pickup drive)
	PICKUP, OCCUPIED_DRIVE, DROPOFF, // serving a customer (TaxiTaskWithRequest)
	STAY;// not directly related to any customer
}
