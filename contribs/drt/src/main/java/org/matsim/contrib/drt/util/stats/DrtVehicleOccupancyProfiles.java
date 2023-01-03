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
package org.matsim.contrib.drt.util.stats;

import java.awt.Color;
import java.awt.Paint;
import java.util.Comparator;
import java.util.Map;

import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileCalculator;
import org.matsim.contrib.util.stats.VehicleOccupancyProfileWriter;
import org.matsim.core.controler.MatsimServices;

import com.google.common.collect.ImmutableMap;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DrtVehicleOccupancyProfiles {
	private static final Comparator<Task.TaskType> nonPassengerTaskTypeComparator = Comparator.comparing(type -> {
		//we want the following order on the plot: STAY, RELOCATE, other
		if (type.equals(DrtStayTask.TYPE)) {
			return "C";
		} else if (type.equals(EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE)) {
			return "B";
		} else {
			return "A" + type.name();
		}
	});

	private static final Map<Task.TaskType, Paint> taskTypePaints = ImmutableMap.of(DrtStayTask.TYPE, Color.LIGHT_GRAY);

	public static VehicleOccupancyProfileWriter createProfileWriter(MatsimServices matsimServices, String mode,
			VehicleOccupancyProfileCalculator calculator) {
		return new VehicleOccupancyProfileWriter(matsimServices, mode, calculator, nonPassengerTaskTypeComparator,
				taskTypePaints);
	}

}
