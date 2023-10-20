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

import java.util.Collection;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.vehicles.*;

public final class ElectricFleetUtils {
	public static final String EV_ENGINE_HBEFA_TECHNOLOGY = "electricity";
	public static final String INITIAL_SOC = "initialSoc";// in [0, 1]
	public static final String CHARGER_TYPES = "chargerTypes";
	private static final String INITIAL_ENERGY_kWh = "initialEnergyInKWh";
	private ElectricFleetUtils(){} // do not instantiate
	public static void setInitialSoc(Vehicle vehicle, double initialSoc) {
		vehicle.getAttributes().putAttribute( INITIAL_SOC, initialSoc );
	}

	public static void setChargerTypes(EngineInformation engineInformation, Collection<String> chargerTypes) {
		engineInformation.getAttributes().putAttribute( CHARGER_TYPES, chargerTypes );
	}
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
	public static ElectricVehicle create( ElectricVehicleSpecification vehicleSpecification,
					      DriveEnergyConsumption.Factory driveFactory, AuxEnergyConsumption.Factory auxFactory,
					      ChargingPower.Factory chargingFactory ) {
		ElectricVehicleDefaultImpl ev = new ElectricVehicleDefaultImpl(vehicleSpecification);
		ev.driveEnergyConsumption = Objects.requireNonNull(driveFactory.create(ev ) );
		ev.auxEnergyConsumption = Objects.requireNonNull(auxFactory.create(ev));
		ev.chargingPower = Objects.requireNonNull(chargingFactory.create(ev));
		return ev;
	}
	public static void createAndAddVehicleSpecificationsFromMatsimVehicles(ElectricFleetSpecification fleetSpecification, Collection<Vehicle> vehicles) {
		vehicles.stream()
				.filter(vehicle -> EV_ENGINE_HBEFA_TECHNOLOGY.equals(VehicleUtils.getHbefaTechnology(vehicle.getType().getEngineInformation())))
				.map( ElectricVehicleSpecificationDefaultImpl::new )
				.forEach(fleetSpecification::addVehicleSpecification);
	}
	public static ElectricVehicleSpecification createElectricVehicleSpecificationDefaultImpl( Vehicle matsimVehicle ){
		return new ElectricVehicleSpecificationDefaultImpl( matsimVehicle );
	}
	public static ElectricFleet createDefaultFleet(ElectricFleetSpecification fleetSpecification,
			DriveEnergyConsumption.Factory driveConsumptionFactory, AuxEnergyConsumption.Factory auxConsumptionFactory,
			ChargingPower.Factory chargingFactory) {
		ImmutableMap<Id<Vehicle>, ElectricVehicle> vehicles = fleetSpecification.getVehicleSpecifications()
											.values()
											.stream()
											.map(s -> create(s, driveConsumptionFactory, auxConsumptionFactory, chargingFactory ))
											.collect(ImmutableMap.toImmutableMap(ElectricVehicle::getId, v -> v));
		return () -> vehicles;
	}
}
