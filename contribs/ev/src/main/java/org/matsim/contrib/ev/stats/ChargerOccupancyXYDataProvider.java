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

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.util.XYDataCollector;
import org.matsim.contrib.util.XYDataCollector.XYDataCalculator;
import org.matsim.contrib.util.XYDataCollectors;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

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

	private static final String PLUGS_ID = "plugs";
	private static final String PLUGGED_ID = "plugged";
	private static final String QUEUED_ID = "queued";
	private static final String ASSIGNED_ID = "assigned";
	private static final String RELATIVE_SUFFIX = "_rel";

	public static XYDataCalculator<Charger> createChargerOccupancyCalculator(
			final ChargingInfrastructure chargingInfrastructure, boolean relative) {
		String[] header = relative ?
				new String[] { PLUGS_ID, PLUGGED_ID + RELATIVE_SUFFIX, QUEUED_ID + RELATIVE_SUFFIX,
						ASSIGNED_ID + RELATIVE_SUFFIX } :
				new String[] { PLUGS_ID, PLUGGED_ID, QUEUED_ID, ASSIGNED_ID };

		return XYDataCollectors.createCalculator(header, charger -> {
			ChargingLogic logic = charger.getLogic();
			int plugs = charger.getPlugCount();
			int assignedCount = logic instanceof ChargingWithAssignmentLogic ?
					((ChargingWithAssignmentLogic)logic).getAssignedVehicles().size() :
					0;
			return new double[] { charger.getPlugCount(), //
					getValue(logic.getPluggedVehicles().size(), plugs, relative),
					getValue(logic.getQueuedVehicles().size(), plugs, relative),
					getValue(assignedCount, plugs, relative) };
		});
	}

	private static double getValue(int count, int plugs, boolean relative) {
		return relative ? (double)count / plugs : count;
	}
}
