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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.inject.Inject;

/*
 * created by jbischoff, 26.10.2018
 */
public class EvMobsimListener implements MobsimBeforeCleanupListener, MobsimAfterSimStepListener {

	@Inject
	EnergyConsumptionCollector energyConsumptionCollector;
	@Inject
	ChargerPowerCollector chargerPowerCollector;
	@Inject
	OutputDirectoryHierarchy controlerIO;
	@Inject
	IterationCounter iterationCounter;
	@Inject
	Network network;
	@Inject
	EvConfigGroup evConfigGroup;
	@Inject
	ChargerPowerTimeProfileCollectorProvider chargerPowerTimeProfileCollectorProvider;
	@Inject
	EvMobsimListener() {
	} // to make the constructor non-public.

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent event) {
		try (CSVPrinter csvPrinter = new CSVPrinter(
				Files.newBufferedWriter(Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "chargingStats.csv"))),
				CSVFormat.DEFAULT.withDelimiter(';')
						.withHeader("ChargerId", "chargeStartTime", "chargeEndTime", "ChargingDuration", "xCoord", "yCoord",
								"energyTransmitted_kWh"))) {
			for (ChargerPowerCollector.ChargingLogEntry e : chargerPowerCollector.getLogList()) {
				double energyKWh = Math.round(EvUnits.J_to_kWh(e.transmitted_Energy()) * 10.) / 10.;
				csvPrinter.printRecord(e.charger().getId(), Time.writeTime(e.chargeStart()), Time.writeTime(e.chargeEnd()),
						Time.writeTime(e.chargeEnd() - e.chargeStart()), e.charger().getCoord().getX(), e.charger().getCoord().getY(), energyKWh);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try (CSVPrinter csvPrinter2 = new CSVPrinter(Files.newBufferedWriter(
				Paths.get(controlerIO.getIterationFilename(iterationCounter.getIterationNumber(), "evConsumptionPerLink.csv"))),
				CSVFormat.DEFAULT.withDelimiter(';').withHeader("Link", "TotalConsumptionPerKm", "TotalConsumption"))) {
			for (Map.Entry<Id<Link>, Double> e : energyConsumptionCollector.getEnergyConsumptionPerLink().entrySet()) {
				csvPrinter2.printRecord(e.getKey(), (EvUnits.J_to_kWh(e.getValue())) / (network.getLinks().get(e.getKey()).getLength() / 1000.0),
						EvUnits.J_to_kWh(e.getValue()));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** After each chargeTimeStep, the power transfer from each station to the vehicles present at that station (vehiclesAtCharger) is calculated.
	 * To do so, the energy at the previous chargeTimeStep needs to be known (vehiclesEnergyPreviousTimeStep). The power is then passed in to the
	 * chargerPowerTimeProfileCollectorProvider. */
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent event) {
		if ((event.getSimulationTime() + 1) % evConfigGroup.chargeTimeStep == 0) {
			ElectricFleet evFleet = chargerPowerTimeProfileCollectorProvider.getEvFleet();
			Map<Id<Charger>, List<Id<Vehicle>>> vehiclesAtCharger = chargerPowerTimeProfileCollectorProvider.getVehiclesAtCharger();
			Map<Id<Vehicle>, Double> vehiclesEnergyPreviousTimeStep = chargerPowerTimeProfileCollectorProvider.getVehiclesEnergyPreviousTimeStep();
			vehiclesAtCharger.forEach((charger, vehicleList) -> {
				if (!vehicleList.isEmpty()) {
					double energy = vehicleList.stream().mapToDouble(vehicleId -> EvUnits.J_to_kWh((Objects.requireNonNull(evFleet.getElectricVehicles().get(vehicleId)).getBattery()
							.getCharge() - vehiclesEnergyPreviousTimeStep.get(vehicleId))*(3600.0/evConfigGroup.chargeTimeStep))).sum();
					if (!Double.isNaN(energy) && !(energy == 0.0)) {
						chargerPowerTimeProfileCollectorProvider.setChargerEnergy(charger, energy);
						vehicleList.forEach(vehicleId -> chargerPowerTimeProfileCollectorProvider
								.setVehiclesEnergyPreviousTimeStep(vehicleId, Objects.requireNonNull(evFleet.getElectricVehicles().get(vehicleId)).getBattery().getCharge()));
					} else {
						chargerPowerTimeProfileCollectorProvider.setChargerEnergy(charger, 0.0);
					}
				} else {
					chargerPowerTimeProfileCollectorProvider.setChargerEnergy(charger, 0.0);
				}
			});

		}
	}
}
