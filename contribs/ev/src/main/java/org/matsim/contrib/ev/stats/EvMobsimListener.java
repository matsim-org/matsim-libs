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

package org.matsim.contrib.ev.stats;/*
 * created by jbischoff, 26.10.2018
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.discharging.DriveDischargingHandler;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;

import com.google.inject.Inject;

public class EvMobsimListener implements MobsimBeforeCleanupListener {

	@Inject
	DriveDischargingHandler driveDischargingHandler;
	@Inject
	ChargerPowerCollector chargerPowerCollector;
	@Inject
	OutputDirectoryHierarchy controlerIO;
	@Inject
	IterationCounter iterationCounter;
	@Inject
	Network network;

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent event) {
		try {
			Files.write(Paths.get(
					controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "chargingStats.csv")),
					() -> chargerPowerCollector.getLogList().stream().<CharSequence>map(Object::toString).iterator());
			Files.write(Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(),
					"evConsumptionPerLink.csv")),
					() -> driveDischargingHandler.getEnergyConsumptionPerLink().entrySet().stream().<CharSequence>map(
							ec -> ec.getKey() + ";" + (EvUnits.J_to_kWh(ec.getValue())) / (network.getLinks()
									.get(ec.getKey())
									.getLength() / 1000.0) + ";" + EvUnits.J_to_kWh(ec.getValue())).iterator());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
