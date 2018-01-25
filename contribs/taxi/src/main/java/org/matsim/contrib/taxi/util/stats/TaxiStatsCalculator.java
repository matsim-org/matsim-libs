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

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.*;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.util.LongEnumAdder;

public class TaxiStatsCalculator {
	private final int hours;
	private final TaxiStats[] hourlyStats;
	private final TaxiStats dailyStats = new TaxiStats(TaxiStatsCalculators.DAILY_STATS_ID);
	private final List<TaxiStats> taxiStats;

	public TaxiStatsCalculator(Iterable<? extends Vehicle> vehicles) {
		hours = TaxiStatsCalculators.calcHourCount(vehicles);
		hourlyStats = new TaxiStats[hours];
		for (int h = 0; h < hours; h++) {
			hourlyStats[h] = new TaxiStats(h + "");
		}

		taxiStats = TaxiStatsCalculators.createStatsList(hourlyStats, dailyStats);

		for (Vehicle v : vehicles) {
			updateStatsForVehicle(v);
		}
	}

	public List<TaxiStats> getTaxiStats() {
		return taxiStats;
	}

	public TaxiStats getDailyStats() {
		return dailyStats;
	}

	private void updateStatsForVehicle(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
			return;// do not evaluate - the vehicle is unused
		}

		@SuppressWarnings("unchecked")
		LongEnumAdder<TaxiTaskType>[] vehicleHourlySums = new LongEnumAdder[hours];

		for (Task t : schedule.getTasks()) {
			TaxiTask tt = (TaxiTask)t;
			int[] hourlyDurations = TaxiStatsCalculators.calcHourlyDurations((int)t.getBeginTime(),
					(int)t.getEndTime());
			int fromHour = TaxiStatsCalculators.getHour(t.getBeginTime());
			for (int i = 0; i < hourlyDurations.length; i++) {
				includeTaskIntoHourlySums(vehicleHourlySums, fromHour + i, tt, hourlyDurations[i]);
			}

			if (tt.getTaxiTaskType() == TaxiTaskType.PICKUP) {
				TaxiRequest req = ((TaxiPickupTask)t).getRequest();
				double waitTime = Math.max(t.getBeginTime() - req.getEarliestStartTime(), 0);
				int hour = TaxiStatsCalculators.getHour(req.getEarliestStartTime());
				hourlyStats[hour].passengerWaitTime.addValue(waitTime);
				dailyStats.passengerWaitTime.addValue(waitTime);
			}
		}

		includeVehicleHourlySumsIntoStats(vehicleHourlySums);
	}

	private void includeTaskIntoHourlySums(LongEnumAdder<TaxiTaskType>[] hourlySums, int hour, TaxiTask task,
			int duration) {
		if (duration > 0) {
			if (hourlySums[hour] == null) {
				hourlySums[hour] = new LongEnumAdder<>(TaxiTaskType.class);
			}
			hourlySums[hour].add(task.getTaxiTaskType(), duration);
		}
	}

	private void includeVehicleHourlySumsIntoStats(LongEnumAdder<TaxiTaskType>[] vehicleHourlySums) {
		LongEnumAdder<TaxiTaskType> vehicleDailySums = new LongEnumAdder<>(TaxiTaskType.class);

		for (int h = 0; h < hours; h++) {
			LongEnumAdder<TaxiTaskType> vhs = vehicleHourlySums[h];
			if (vhs != null && vhs.getLongTotal() > 0) {
				updateTaxiStats(hourlyStats[h], vhs);
				vehicleDailySums.addAll(vhs);
			}
		}

		updateTaxiStats(dailyStats, vehicleDailySums);
	}

	private void updateTaxiStats(TaxiStats taxiStats, LongEnumAdder<TaxiTaskType> vehicleSums) {
		updateEmptyDriveRatio(taxiStats.vehicleEmptyDriveRatio, vehicleSums);
		updateStayRatio(taxiStats.vehicleStayRatio, vehicleSums);
		taxiStats.taskTimeSumsByType.addAll(vehicleSums);
	}

	private void updateEmptyDriveRatio(DescriptiveStatistics emptyDriveRatioStats,
			LongEnumAdder<TaxiTaskType> durations) {
		double empty = durations.getLong(TaxiTaskType.EMPTY_DRIVE);
		double occupied = durations.getLong(TaxiTaskType.OCCUPIED_DRIVE);

		if (empty != 0 || occupied != 0) {
			double emptyRatio = empty / (empty + occupied);
			emptyDriveRatioStats.addValue(emptyRatio);
		}
	}

	private void updateStayRatio(DescriptiveStatistics stayRatioStats, LongEnumAdder<TaxiTaskType> durations) {
		double total = durations.getLongTotal();
		if (total != 0) {
			double stayRatio = durations.getLong(TaxiTaskType.STAY) / total;
			stayRatioStats.addValue(stayRatio);
		}
	}
}
