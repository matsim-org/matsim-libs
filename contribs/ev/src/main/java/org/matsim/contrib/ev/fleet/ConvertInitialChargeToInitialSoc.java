/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.ev.fleet;

import static org.matsim.contrib.ev.fleet.ElectricVehicleSpecificationImpl.INITIAL_ENERGY_kWh;
import static org.matsim.contrib.ev.fleet.ElectricVehicleSpecificationImpl.INITIAL_SOC;

import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ConvertInitialChargeToInitialSoc {
	public static void run(String file) {
		var vehicles = VehicleUtils.createVehiclesContainer();
		var reader = new MatsimVehicleReader(vehicles);
		reader.readFile(file);

		for (var v : vehicles.getVehicles().values()) {
			double battery_kWh = VehicleUtils.getEnergyCapacity(v.getType().getEngineInformation());
			double initial_kWh = (double)v.getAttributes().getAttribute(INITIAL_ENERGY_kWh);
			double initial_soc = initial_kWh / battery_kWh;
			v.getAttributes().removeAttribute(INITIAL_ENERGY_kWh);
			v.getAttributes().putAttribute(INITIAL_SOC, initial_soc);
		}

		var writer = new MatsimVehicleWriter(vehicles);
		writer.writeFile(file);
	}
}
