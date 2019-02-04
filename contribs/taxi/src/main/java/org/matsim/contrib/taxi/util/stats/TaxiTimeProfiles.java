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

import java.util.Collection;

import org.matsim.contrib.dvrp.data.DvrpVehicle;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.data.TaxiRequests;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.util.LongEnumAdder;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.util.timeprofile.TimeProfiles;

public class TaxiTimeProfiles {
	public static ProfileCalculator createIdleVehicleCounter(final Fleet fleet, final ScheduleInquiry scheduleInquiry) {
		return TimeProfiles.createSingleValueCalculator("Idle",
				() -> fleet.getVehicles().values().stream().filter(scheduleInquiry::isIdle).count());
	}

	public static ProfileCalculator createCurrentTaxiTaskTypeCounter(final Fleet fleet) {
		String[] header = TimeProfiles.combineValuesIntoStrings((Object[])TaxiTaskType.values());
		return TimeProfiles.createProfileCalculator(header, () -> calculateTaxiTaskTypeCounts(fleet));
	}

	public static Long[] calculateTaxiTaskTypeCounts(Fleet fleet) {
		LongEnumAdder<TaxiTaskType> counter = new LongEnumAdder<>(TaxiTaskType.class);
		for (DvrpVehicle veh : fleet.getVehicles().values()) {
			if (veh.getSchedule().getStatus() == ScheduleStatus.STARTED) {
				TaxiTask currentTask = (TaxiTask)veh.getSchedule().getCurrentTask();
				counter.increment(currentTask.getTaxiTaskType());
			}
		}

		Long[] counts = new Long[TaxiTaskType.values().length];
		for (TaxiTaskType e : TaxiTaskType.values()) {
			counts[e.ordinal()] = counter.getLong(e);
		}
		return counts;
	}

	public static ProfileCalculator createRequestsWithStatusCounter(final Collection<? extends Request> requests,
			final TaxiRequestStatus requestStatus) {
		return TimeProfiles.createSingleValueCalculator(requestStatus.name(),
				() -> TaxiRequests.countRequestsWithStatus(requests.stream(), requestStatus));
	}
}
