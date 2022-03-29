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

package org.matsim.contrib.ev.stats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.common.timeprofile.TimeProfileCharts.ChartType;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.common.timeprofile.TimeProfiles;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

public class ChargerOccupancyTimeProfileCollectorProvider implements Provider<MobsimListener> {
	private final ChargingInfrastructure chargingInfrastructure;
	private final MatsimServices matsimServices;

	@Inject
	public ChargerOccupancyTimeProfileCollectorProvider(ChargingInfrastructure chargingInfrastructure,
			MatsimServices matsimServices) {
		this.chargingInfrastructure = chargingInfrastructure;
		this.matsimServices = matsimServices;
	}

	@Override
	public MobsimListener get() {
		ProfileCalculator calc = createChargerOccupancyCalculator(chargingInfrastructure);
		TimeProfileCollector collector = new TimeProfileCollector(calc, 300, "charger_occupancy_time_profiles",
				matsimServices);
		if (matsimServices.getConfig().controler().isCreateGraphs()) {
			collector.setChartTypes(ChartType.Line, ChartType.StackedArea);
		} else {
			collector.setChartTypes();
		}
		return collector;
	}

	private static final String PLUGGED_ID = "plugged";
	private static final String QUEUED_ID = "queued";
	private static final String ASSIGNED_ID = "assigned";

	public static ProfileCalculator createChargerOccupancyCalculator(
			final ChargingInfrastructure chargingInfrastructure) {
		ImmutableList<String> header = ImmutableList.of(PLUGGED_ID, QUEUED_ID, ASSIGNED_ID);
		return TimeProfiles.createProfileCalculator(header, () -> {
			int plugged = 0;
			int queued = 0;
			int assigned = 0;
			for (Charger c : chargingInfrastructure.getChargers().values()) {
				ChargingLogic logic = c.getLogic();
				plugged += logic.getPluggedVehicles().size();
				queued += logic.getQueuedVehicles().size();
				if (logic instanceof ChargingWithAssignmentLogic) {
					assigned += ((ChargingWithAssignmentLogic)logic).getAssignedVehicles().size();
				}
			}
			return ImmutableMap.of(PLUGGED_ID, (double)plugged, QUEUED_ID, (double)queued, ASSIGNED_ID,
					(double)assigned);
		});
	}
}
