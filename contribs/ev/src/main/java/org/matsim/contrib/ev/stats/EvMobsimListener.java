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

import com.google.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.discharging.DriveDischargingHandler;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.utils.misc.Time;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

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
			CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "chargingStats.csv"))), CSVFormat.DEFAULT.withDelimiter(';').
					withHeader("ChargerId", "chargeStartTime", "chargeEndTime", "ChargingDuration", "xCoord", "yCoord", "energyTransmitted_kWh"));
			for (ChargerPowerCollector.ChargingLogEntry e : chargerPowerCollector.getLogList()) {
				double energyKWh = Math.round(EvUnits.J_to_kWh(e.getTransmitted_Energy()) * 10.) / 10.;
				csvPrinter.printRecord(e.getCharger().getId(), Time.writeTime(e.getChargeStart()), Time.writeTime(e.getChargeEnd()), Time.writeTime(e.getChargeEnd() - e.getChargeStart()), e.getCharger().getCoord().getX(), e.getCharger().getCoord().getY(), energyKWh);
			}
			csvPrinter.close();

			CSVPrinter csvPrinter2 = new CSVPrinter(Files.newBufferedWriter(Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "evConsumptionPerLink.csv"))), CSVFormat.DEFAULT.withDelimiter(';').withHeader("Link", "TotalConsumptionPerKm", "TotalConsumption"));
			for (Map.Entry<Id<Link>, Double> e : driveDischargingHandler.getEnergyConsumptionPerLink().entrySet()) {
				csvPrinter2.printRecord(e.getKey(), (EvUnits.J_to_kWh(e.getValue())) / (network.getLinks()
						.get(e.getKey())
						.getLength() / 1000.0), EvUnits.J_to_kWh(e.getValue()));
			}
			csvPrinter2.close();


		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
