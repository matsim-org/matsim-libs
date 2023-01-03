/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.taxi.util.stats;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.dvrp.schedule.Task;

public class TaxiStats {
	public final String id;

	// all requests submissions made within the analyzed time period
	public final DescriptiveStatistics passengerWaitTime = new DescriptiveStatistics();

	// duration of all task types within a given time period
	// each vehicle's contribution is between 0 and N s (where N is the length of time period)
	// (vehicle may not operate all the time)
	// similar, yet slightly less accurate, results can be obtained by averaging time profile
	// values in a given time period
	public final Map<Task.TaskType, Double> taskTypeDurations = new HashMap<>();

	// all drives that started within the analyzed time period
	// in the case of hourly stats, expect high variations:
	// can be 1.0 if a single empty drive started just before the end of this hour;
	// can also be 0.0 if a single occupied drive started just after the beginning of this hour
	public final DescriptiveStatistics vehicleEmptyDriveRatio = new DescriptiveStatistics();

	// vehicles' operation may be of different lengths, which may bias these stats
	// if this effect is not desired, consider using taskTypeSums instead
	public final DescriptiveStatistics vehicleStayRatio = new DescriptiveStatistics();

	public TaxiStats(String id) {
		this.id = id;
	}

	public double calculateTotalDuration() {
		return taskTypeDurations.values().stream().mapToDouble(Double::doubleValue).sum();
	}

	public OptionalDouble calculateFleetEmptyDriveRatio() {
		return TaxiStatsCalculator.calculateEmptyDriveRatio(taskTypeDurations);
	}

	public OptionalDouble calculateFleetStayRatio() {
		return TaxiStatsCalculator.calculateStayRatio(taskTypeDurations);
	}

	public OptionalDouble calculateOccupiedDriveRatio() {
		return TaxiStatsCalculator.calculateOccupiedDriveRatio(taskTypeDurations);
	}
}
