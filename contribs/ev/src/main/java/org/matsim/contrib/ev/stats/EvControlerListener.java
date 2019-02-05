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
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.google.inject.Inject;

public class EvControlerListener implements IterationEndsListener {

	@Inject
	DriveDischargingHandler driveDischargingHandler;
	@Inject
	ChargerPowerCollector chargerPowerCollector;
	@Inject
	OutputDirectoryHierarchy controlerIO;
	@Inject
	Network network;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			Files.write(Paths.get(controlerIO.getIterationFilename(event.getIteration(), "chargingStats.csv")),
					() -> chargerPowerCollector.getLogList().stream().<CharSequence>map(e -> e.toString()).iterator());
			Files.write(Paths.get(controlerIO.getIterationFilename(event.getIteration(), "evConsumptionPerLink.csv")),
					() -> driveDischargingHandler.getEnergyConsumptionPerLink().entrySet().stream().<CharSequence>map(
							e -> e.getKey() + ";" + (EvUnits.J_to_kWh(e.getValue())) / (network.getLinks()
									.get(e.getKey())
									.getLength() / 1000.0) + ";" + EvUnits.J_to_kWh(e.getValue())).iterator());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
