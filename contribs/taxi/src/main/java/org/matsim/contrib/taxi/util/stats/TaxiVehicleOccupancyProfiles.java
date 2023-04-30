/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
package org.matsim.contrib.taxi.util.stats;

import java.awt.Color;
import java.awt.Paint;
import java.util.Comparator;
import java.util.Map;

import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.util.stats.ProfileWriter;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileCalculator;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileView;
import org.matsim.core.controler.MatsimServices;

import com.google.common.collect.ImmutableMap;

/**
 * @author michalm (Michal Maciejewski)
 */
public class TaxiVehicleOccupancyProfiles {
	private static final Comparator<Task.TaskType> nonPassengerTaskTypeComparator = Comparator.comparing(type -> {
		//we want the following order on the plot: STAY, other
		if (type.equals(TaxiStayTask.TYPE)) {
			return "B";
		} else {
			return "A" + type.name();
		}
	});

	private static final Map<Task.TaskType, Paint> taskTypePaints = ImmutableMap.of(TaxiStayTask.TYPE, Color.LIGHT_GRAY);

	public static ProfileWriter createProfileWriter(MatsimServices matsimServices, String mode, VehicleOccupancyProfileCalculator calculator) {
		return new ProfileWriter(matsimServices, mode, new VehicleOccupancyProfileView(calculator, nonPassengerTaskTypeComparator, taskTypePaints),
				"occupancy_time_profiles");
	}
}
