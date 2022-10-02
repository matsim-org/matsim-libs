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

import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ElectricFleetFileConverter {
	/**
	 * Convert a file containing EVs from the EV-custom format (http://matsim.org/files/dtd/electric_vehicles_v1.dtd)
	 * to the standard matsim vehicle v2 format (http://www.matsim.org/files/dtd/vehicleDefinitions_v2.0.xsd)
	 */
	public static void convertElectricFleetFile(String inputFile, String outputFile) {
		ElectricFleetSpecification fleetSpecification = new ElectricFleetSpecificationImpl();
		new ElectricFleetReader(fleetSpecification).readFile(inputFile);

		var vehicles = VehicleUtils.createVehiclesContainer();
		var typesByCapacity = new HashMap<Double, VehicleType>();

		// convert to matsim vehicles
		for (var ev : fleetSpecification.getVehicleSpecifications().values()) {
			var type = typesByCapacity.computeIfAbsent(ev.getBatteryCapacity(), aDouble -> createType(ev, vehicles));
			var vehicle = VehicleUtils.createVehicle(Id.createVehicleId(ev.getId().toString()), type);
			vehicle.getAttributes()
					.putAttribute(ElectricVehicleSpecificationWithMatsimVehicle.INITIAL_ENERGY_kWh,
							ev.getInitialSoc() / EvUnits.J_PER_kWh);
			vehicles.addVehicle(vehicle);
		}

		// write matsim vehicles
		new MatsimVehicleWriter(vehicles).writeFile(outputFile);
	}

	private static VehicleType createType(ElectricVehicleSpecification ev, Vehicles vehicles) {
		var capacity = ev.getBatteryCapacity() / EvUnits.J_PER_kWh;
		var type = VehicleUtils.createVehicleType(Id.create("EV_" + capacity + "kWh", VehicleType.class));
		VehicleUtils.setEnergyCapacity(type.getEngineInformation(), capacity);
		type.getEngineInformation()
				.getAttributes()
				.putAttribute(ElectricVehicleSpecificationWithMatsimVehicle.CHARGER_TYPES,
						List.copyOf(ev.getChargerTypes()));
		VehicleUtils.setHbefaTechnology(type.getEngineInformation(),
				ElectricVehicleSpecificationWithMatsimVehicle.EV_ENGINE_HBEFA_TECHNOLOGY);
		vehicles.addVehicleType(type);
		return type;
	}
}
