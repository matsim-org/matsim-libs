/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.urbanEV;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.vehicles.*;

import java.util.Arrays;

public class ConvertCarsInVehiclesFileToEVs {

	public static void main(String[] args) {

		String openBerlinVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-v5-mode-vehicle-types.xml";
		String outputVehiclesFile = "C:/Users/admin/IdeaProjects/matsim-berlin/test/input/berlin-v5.5-1pct/input/ev/berlin-v5-mode-vehicle-types-ev-cars.xml";

		Vehicles vehicles = VehicleUtils.createVehiclesContainer();


		new MatsimVehicleReader(vehicles).readFile(openBerlinVehiclesFile);

		VehicleType carVehicleType = vehicles.getVehicleTypes().get(Id.create("car", VehicleType.class));
		VehicleUtils.setHbefaTechnology(carVehicleType.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(carVehicleType.getEngineInformation(), 100);
		EVUtils.setInitialEnergy(carVehicleType.getEngineInformation(), 100);
		EVUtils.setChargerTypes(carVehicleType.getEngineInformation(), Arrays.asList("a", "b"));

		VehicleType bikeVehicleType = vehicles.getVehicleTypes().get(Id.create("bike", VehicleType.class));
		VehicleUtils.setHbefaTechnology(bikeVehicleType.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(bikeVehicleType.getEngineInformation(), 10);
		EVUtils.setInitialEnergy(bikeVehicleType.getEngineInformation(), 4);
		EVUtils.setChargerTypes(bikeVehicleType.getEngineInformation(), Arrays.asList("a", "b", "default"));

		new MatsimVehicleWriter(vehicles).writeFile(outputVehiclesFile);

	}
}
