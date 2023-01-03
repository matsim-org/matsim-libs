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

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TaxiTaskTypes {
	public static final ImmutableList<Task.TaskType> DEFAULT_TAXI_TYPES = ImmutableList.of(TaxiEmptyDriveTask.TYPE,
			TaxiPickupTask.TYPE, TaxiOccupiedDriveTask.TYPE, TaxiDropoffTask.TYPE, TaxiStayTask.TYPE);
}
