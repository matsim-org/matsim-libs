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

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.stats.XYDataCollector.XYDataCalculator;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ChargerOccupancyXYDataProvider implements Provider<MobsimListener> {
	private final ChargingInfrastructure chargingInfrastructure;
	private final MatsimServices matsimServices;

	@Inject
	public ChargerOccupancyXYDataProvider(ChargingInfrastructure chargingInfrastructure, MatsimServices matsimServices) {
		this.chargingInfrastructure = chargingInfrastructure;
		this.matsimServices = matsimServices;
	}

	@Override
	public MobsimListener get() {
		String[] header = new String[] { "plugs", "plugged", "queued", "assigned" };

		XYDataCalculator<Charger> calc = new XYDataCalculator<>() {
			@Override
			public String[] getHeader() {
				return header;
			}

			@Override
			public Coord getCoord(Charger object) {
				return object.getCoord();
			}

			@Override
			public double[] calculate(Charger object) {
				ChargingLogic logic = object.getLogic();
				int assignedCount = logic instanceof ChargingWithAssignmentLogic ?
						((ChargingWithAssignmentLogic)logic).getAssignedVehicles().size() :
						0;
				return new double[] { object.getPlugCount(), (double)logic.getPluggedVehicles().size(), (double)logic.getQueuedVehicles().size(),
						(double)assignedCount };
			}
		};

		return new XYDataCollector<>(chargingInfrastructure.getChargers().values(), calc, 300, "charger_occupancy_absolute", matsimServices);
	}
}
