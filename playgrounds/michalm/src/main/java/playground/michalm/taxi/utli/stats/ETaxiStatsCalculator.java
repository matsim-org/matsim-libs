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

package playground.michalm.taxi.utli.stats;

import java.util.List;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.taxi.util.stats.TaxiStatsCalculators;

import playground.michalm.taxi.schedule.ETaxiChargingTask;
import playground.michalm.taxi.utli.stats.ETaxiStats.ETaxiState;

public class ETaxiStatsCalculator {
	private final int hours;
	private final ETaxiStats[] hourlyEStats;
	private final ETaxiStats dailyEStats = new ETaxiStats(TaxiStatsCalculators.DAILY_STATS_ID);
	private final List<ETaxiStats> eTaxiStats;

	public ETaxiStatsCalculator(Iterable<? extends Vehicle> vehicles) {
		hours = TaxiStatsCalculators.calcHourCount(vehicles);
		hourlyEStats = new ETaxiStats[hours];
		for (int h = 0; h < hours; h++) {
			hourlyEStats[h] = new ETaxiStats(h + "");
		}

		eTaxiStats = TaxiStatsCalculators.createStatsList(hourlyEStats, dailyEStats);

		for (Vehicle v : vehicles) {
			updateEStatsForVehicle(v);
		}
	}

	public List<ETaxiStats> geteTaxiStats() {
		return eTaxiStats;
	}

	public ETaxiStats getDailyEStats() {
		return dailyEStats;
	}

	private void updateEStatsForVehicle(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
			return;// do not evaluate - the vehicle is unused
		}

		for (Task t : schedule.getTasks()) {
			if (t instanceof ETaxiChargingTask) {
				ETaxiChargingTask chargingTask = (ETaxiChargingTask)t;

				int arrivalTime = (int)t.getBeginTime();
				int chargingStartTime = (int)chargingTask.getChargingStartedTime();
				int chargingEndTime = (int)t.getEndTime();

				updateHourlyStateTimes(ETaxiState.QUEUED, arrivalTime, chargingStartTime);
				updateHourlyStateTimes(ETaxiState.PLUGGED, chargingStartTime, chargingEndTime);

				dailyEStats.stateTimeSumsByState.add(ETaxiState.QUEUED, chargingStartTime - arrivalTime);
				dailyEStats.stateTimeSumsByState.add(ETaxiState.PLUGGED, chargingEndTime - chargingStartTime);
			}
		}
	}

	private void updateHourlyStateTimes(ETaxiState state, int from, int to) {
		int[] hourlyDurations = TaxiStatsCalculators.calcHourlyDurations(from, to);
		int fromHour = TaxiStatsCalculators.getHour(from);
		for (int i = 0; i < hourlyDurations.length; i++) {
			hourlyEStats[fromHour + i].stateTimeSumsByState.add(state, hourlyDurations[i]);
		}
	}
}
