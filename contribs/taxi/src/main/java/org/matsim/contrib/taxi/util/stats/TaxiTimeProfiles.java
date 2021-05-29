/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.*;

import java.util.Collection;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.passenger.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.passenger.TaxiRequests;
import org.matsim.contrib.taxi.schedule.TaxiTaskTypes;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.common.timeprofile.TimeProfiles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class TaxiTimeProfiles {
	public static ProfileCalculator createIdleVehicleCounter(final Fleet fleet, final ScheduleInquiry scheduleInquiry) {
		return TimeProfiles.createSingleValueCalculator("Idle",
				() -> fleet.getVehicles().values().stream().filter(scheduleInquiry::isIdle).count());
	}

	public static ProfileCalculator createCurrentTaxiTaskTypeCounter(final Fleet fleet) {
		ImmutableList<String> header = TaxiTaskTypes.DEFAULT_TAXI_TYPES.stream()
				.map(Task.TaskType::name)
				.collect(toImmutableList());
		return TimeProfiles.createProfileCalculator(header, () -> calculateTaxiTaskTypeCounts(fleet));
	}

	public static ImmutableMap<String, Double> calculateTaxiTaskTypeCounts(Fleet fleet) {
		return fleet.getVehicles()
				.values()
				.stream()
				.map(DvrpVehicle::getSchedule)
				.filter(schedule -> schedule.getStatus() == ScheduleStatus.STARTED)
				.collect(collectingAndThen(
						groupingBy(schedule -> schedule.getCurrentTask().getTaskType().name(), summingDouble(e -> 1)),
						ImmutableMap::copyOf));
	}

	public static ProfileCalculator createRequestsWithStatusCounter(final Collection<? extends Request> requests,
			final TaxiRequestStatus requestStatus) {
		return TimeProfiles.createSingleValueCalculator(requestStatus.name(),
				() -> TaxiRequests.countRequestsWithStatus(requests.stream(), requestStatus));
	}
}
