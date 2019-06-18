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

import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.util.XYDataCollector;
import org.matsim.contrib.util.XYDataCollector.XYDataCalculator;
import org.matsim.contrib.util.XYDataCollectors;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ChargerOccupancyXYDataProvider implements Provider<MobsimListener> {
	private final ChargingInfrastructure chargingInfrastructure;
	private final MatsimServices matsimServices;

	@Inject
	public ChargerOccupancyXYDataProvider(ChargingInfrastructure chargingInfrastructure,
			MatsimServices matsimServices) {
		this.chargingInfrastructure = chargingInfrastructure;
		this.matsimServices = matsimServices;
	}

	@Override
	public MobsimListener get() {
		XYDataCalculator<Charger> calc = createChargerOccupancyCalculator(chargingInfrastructure, false);
		return new XYDataCollector<>(chargingInfrastructure.getChargers().values(), calc, 300,
				"charger_occupancy_absolute", matsimServices);
	}

	public static XYDataCalculator<Charger> createChargerOccupancyCalculator(
			final ChargingInfrastructure chargingInfrastructure, boolean relative) {
		String[] header = relative ? //
				new String[] { "plugs", "plugged_rel", "queued_rel", "assigned_rel" } //
				: new String[] { "plugs", "plugged", "queued", "assigned" };

		return XYDataCollectors.createCalculator(header, charger -> {
			ChargingLogic logic = charger.getLogic();
			int plugs = charger.getPlugs();
			int assignedCount = logic instanceof ChargingWithQueueingAndAssignmentLogic //
					? ((ChargingWithQueueingAndAssignmentLogic)logic).getAssignedVehicles().size()//
					: 0;
			return new String[] { charger.getPlugs() + "", //
					getValue(logic.getPluggedVehicles().size(), plugs, relative), //
					getValue(logic.getQueuedVehicles().size(), plugs, relative), //
					getValue(assignedCount, plugs, relative) };
		});
	}

	private static String getValue(int count, int plugs, boolean relative) {
		return relative ? ((double)count / plugs) + "" : count + "";
	}
}
