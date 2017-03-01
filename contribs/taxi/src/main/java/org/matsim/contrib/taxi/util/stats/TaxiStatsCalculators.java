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

import java.util.*;

import org.matsim.contrib.dvrp.data.Vehicle;

public class TaxiStatsCalculators {
	public static final String DAILY_STATS_ID = "daily";

	public static int calcHourCount(Iterable<? extends Vehicle> vehicles) {
		double maxEndTime = 0;
		for (Vehicle v : vehicles) {
			double endTime = v.getSchedule().getEndTime();
			if (endTime > maxEndTime) {
				maxEndTime = endTime;
			}
		}

		return (int)Math.ceil(maxEndTime / 3600);
	}

	public static int getHour(double time) {
		return (int)(time / 3600);
	}

	public static <T> List<T> createStatsList(T[] hourlyStats, T dailyStats) {
		List<T> allStats = new ArrayList<>(hourlyStats.length + 1);
		Collections.addAll(allStats, hourlyStats);
		allStats.add(dailyStats);
		return Collections.unmodifiableList(allStats);
	}

	public static int[] calcHourlyDurations(int from, int to) {
		int firstHour = (int)from / 3600;
		int lastHour = (int)to / 3600;

		if (firstHour == lastHour) {
			return new int[] { to - from };
		}

		int[] hourlyDurations = new int[lastHour - firstHour + 1];
		hourlyDurations[0] = 3600 - from % 3600;
		hourlyDurations[hourlyDurations.length - 1] = to % 3600;
		for (int i = 1; i < hourlyDurations.length - 1; i++) {
			hourlyDurations[i] = 3600;
		}

		return hourlyDurations;
	}
}