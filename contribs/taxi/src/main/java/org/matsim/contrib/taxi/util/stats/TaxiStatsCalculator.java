/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import static org.matsim.contrib.taxi.schedule.TaxiTaskBaseType.*;

import java.util.Collection;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task.TaskType;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.schedule.TaxiTaskBaseType;
import org.matsim.contrib.taxi.schedule.TaxiTaskType;
import org.matsim.contrib.util.stats.DurationStats;

import com.google.common.collect.ImmutableList;

import one.util.streamex.EntryStream;

public class TaxiStatsCalculator {
	public static final String DAILY_STATS_ID = "daily";

	private final SortedMap<Integer, TaxiStats> hourlyStats = new TreeMap<>();
	private final TaxiStats dailyStats = new TaxiStats(DAILY_STATS_ID);

	public TaxiStatsCalculator(Collection<? extends DvrpVehicle> vehicles) {
		for (DvrpVehicle vehicle : vehicles) {
			DurationStats.taskDurationByTimeBinAndType(vehicle, 3600)
					.forEach((hour, taskTypeDurations) -> updateTaxiStats(getHourlyStats(hour), taskTypeDurations));

			Map<TaskType, Double> dailyTaskTypeDuration = DurationStats.taskDurationByTimeBinAndType(vehicle,
					Integer.MAX_VALUE).entrySet().iterator().next().getValue();
			updateTaxiStats(dailyStats, dailyTaskTypeDuration);

			updatePassengerWaitTimeStats(vehicle);
		}
	}

	public ImmutableList<TaxiStats> getTaxiStats() {
		return ImmutableList.<TaxiStats>builder().addAll(hourlyStats.values()).add(dailyStats).build();
	}

	public TaxiStats getDailyStats() {
		return dailyStats;
	}

	private TaxiStats getHourlyStats(int hour) {
		return hourlyStats.computeIfAbsent(hour, h -> new TaxiStats(h + ""));
	}

	private static void updateTaxiStats(TaxiStats stats, Map<TaskType, Double> taskTypeDurations) {
		updateTaskDurations(stats, taskTypeDurations);
		calculateEmptyDriveRatio(taskTypeDurations).ifPresent(stats.vehicleEmptyDriveRatio::addValue);
		calculateStayRatio(taskTypeDurations).ifPresent(stats.vehicleStayRatio::addValue);
	}

	static OptionalDouble calculateEmptyDriveRatio(Map<TaskType, Double> taskTypeDurations) {
		double empty = sumBaseTaskTypeDurations(taskTypeDurations, EMPTY_DRIVE);
		double occupied = sumBaseTaskTypeDurations(taskTypeDurations, OCCUPIED_DRIVE);
		return (empty != 0 || occupied != 0) ? OptionalDouble.of(empty / (empty + occupied)) : OptionalDouble.empty();
	}

	static OptionalDouble calculateStayRatio(Map<TaskType, Double> taskTypeDurations) {
		double stay = sumBaseTaskTypeDurations(taskTypeDurations, STAY);
		double total = taskTypeDurations.values().stream().mapToDouble(Double::doubleValue).sum();
		return total != 0 ? OptionalDouble.of(stay / total) : OptionalDouble.empty();
	}

	static OptionalDouble calculateOccupiedDriveRatio(Map<TaskType, Double> taskTypeDurations) {
		double occupied = sumBaseTaskTypeDurations(taskTypeDurations, OCCUPIED_DRIVE);
		double total = taskTypeDurations.values().stream().mapToDouble(Double::doubleValue).sum();
		return total != 0 ? OptionalDouble.of(occupied / total) : OptionalDouble.empty();
	}

	private static double sumBaseTaskTypeDurations(Map<TaskType, Double> taskTypeDurations, TaxiTaskBaseType baseType) {
		return EntryStream.of(taskTypeDurations)
				.filterKeys(taskType -> ((TaxiTaskType)taskType).getBaseType().orElse(null) == baseType)
				.mapToDouble(Map.Entry::getValue)
				.sum();
	}

	private static void updateTaskDurations(TaxiStats stats, Map<TaskType, Double> taskTypeDurations) {
		taskTypeDurations.forEach(
				(taskType, duration) -> stats.taskTypeDurations.merge(taskType, duration, Double::sum));
	}

	private void updatePassengerWaitTimeStats(DvrpVehicle vehicle) {
		vehicle.getSchedule().tasks().filter(task -> PICKUP.isBaseTypeOf(task)).forEach(task -> {
			TaxiRequest req = ((TaxiPickupTask)task).getRequest();
			double waitTime = Math.max(task.getBeginTime() - req.getEarliestStartTime(), 0);
			int hour = (int)(req.getEarliestStartTime() / 3600);
			getHourlyStats(hour).passengerWaitTime.addValue(waitTime);
			dailyStats.passengerWaitTime.addValue(waitTime);
		});
	}
}
