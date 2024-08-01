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
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.contrib.ev.charging.ChargingLogic;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;

public final class ChargerOccupancyXYDataCollector implements MobsimInitializedListener, MobsimBeforeSimStepListener, MobsimBeforeCleanupListener {

	private final ChargingInfrastructure chargingInfrastructure;
	private final MatsimServices matsimServices;

	private CompactCSVWriter writer;

	@Inject
	ChargerOccupancyXYDataCollector(ChargingInfrastructure chargingInfrastructure, MatsimServices matsimServices) {
		this.chargingInfrastructure = chargingInfrastructure;
		this.matsimServices = matsimServices;
	}

	@Override
	public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent e) {
		String file = matsimServices.getControlerIO().getIterationFilename(matsimServices.getIterationNumber(), "charger_occupancy_absolute");
		writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file + ".xy.gz"));
		writer.writeNext(new CSVLineBuilder().addAll("time", "id", "x", "y", "plugs", "plugged", "queued", "assigned"));
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		final int interval = 300;
		if (e.getSimulationTime() % interval == 0) {
			String time = (int)e.getSimulationTime() + "";
			for (var c : chargingInfrastructure.getChargers().values()) {
				Coord coord = c.getCoord();
				CSVLineBuilder builder = new CSVLineBuilder().addAll(time, c.getId() + "", coord.getX() + "", coord.getY() + "");
				for (int value : calculate(c)) {
					builder.add(value + "");
				}
				writer.writeNext(builder);
			}
		}
	}

	private int[] calculate(Charger charger) {
		ChargingLogic logic = charger.getLogic();
		int assignedCount = logic instanceof ChargingWithAssignmentLogic ? ((ChargingWithAssignmentLogic)logic).getAssignedVehicles().size() : 0;
		return new int[] { charger.getPlugCount(), logic.getPluggedVehicles().size(), logic.getQueuedVehicles().size(), assignedCount };
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		writer.close();
	}
}
