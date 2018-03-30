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

package org.matsim.vsp.ev.stats;

import org.matsim.contrib.util.timeprofile.TimeProfileCharts.ChartType;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.util.timeprofile.TimeProfiles;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.vsp.ev.charging.ChargingLogic;
import org.matsim.vsp.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.EvData;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ChargerOccupancyTimeProfileCollectorProvider implements Provider<MobsimListener> {
	private final EvData evData;
	private final MatsimServices matsimServices;

	@Inject
	public ChargerOccupancyTimeProfileCollectorProvider(EvData evData, MatsimServices matsimServices) {
		this.evData = evData;
		this.matsimServices = matsimServices;
	}

	@Override
	public MobsimListener get() {
		ProfileCalculator calc = createChargerOccupancyCalculator(evData);
		TimeProfileCollector collector = new TimeProfileCollector(calc, 300, "charger_occupancy_time_profiles",
				matsimServices);
		collector.setChartTypes(ChartType.Line, ChartType.StackedArea);
		return collector;
	}

	public static ProfileCalculator createChargerOccupancyCalculator(final EvData evData) {
		String[] header = { "plugged", "queued", "assigned" };
		return TimeProfiles.createProfileCalculator(header, () -> {
			int plugged = 0;
			int queued = 0;
			int assigned = 0;
			for (Charger c : evData.getChargers().values()) {
				ChargingLogic logic = c.getLogic();
				plugged += logic.getPluggedVehicles().size();
				queued += logic.getQueuedVehicles().size();
				if (logic instanceof ChargingWithQueueingAndAssignmentLogic) {
					assigned += ((ChargingWithQueueingAndAssignmentLogic)logic).getAssignedVehicles().size();
				}
			}
			return new Integer[] { plugged, queued, assigned };
		});
	}
}
