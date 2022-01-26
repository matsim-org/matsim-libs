/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ElectricVehicleSpecificationWithMatsimVehicle implements ElectricVehicleSpecification {
	public static final String EV_ENGINE_HBEFA_TECHNOLOGY = "electricity";

	public static final String INITIAL_ENERGY_kWh = "initialEnergyInKWh";
	public static final String CHARGER_TYPES = "chargerTypes";

	public static ElectricFleetSpecification createFleetSpecificationFromMatsimVehicles(Vehicles vehicles) {
		ElectricFleetSpecification fleetSpecification = new ElectricFleetSpecificationImpl();
		vehicles.getVehicles()
				.values()
				.stream()
				.filter(vehicle -> EV_ENGINE_HBEFA_TECHNOLOGY.equals(
						VehicleUtils.getHbefaTechnology(vehicle.getType().getEngineInformation())))
				.map(ElectricVehicleSpecificationWithMatsimVehicle::new)
				.forEach(fleetSpecification::addVehicleSpecification);
		return fleetSpecification;
	}

	private final Id<ElectricVehicle> id;
	private final Vehicle matsimVehicle;// matsim vehicle is mutable!
	private final String vehicleType;
	private final ImmutableList<String> chargerTypes;
	private final double initialSoc;
	private final double batteryCapacity;

	public ElectricVehicleSpecificationWithMatsimVehicle(Vehicle matsimVehicle) {
		id = Objects.requireNonNull(Id.create(matsimVehicle.getId(), ElectricVehicle.class));
		this.matsimVehicle = matsimVehicle;
		vehicleType = matsimVehicle.getType().getId().toString();

		//provided per vehicle type (in engine info)
		var engineInfo = matsimVehicle.getType().getEngineInformation();
		chargerTypes = ImmutableList.copyOf((Collection<String>)engineInfo.getAttributes().getAttribute(CHARGER_TYPES));
		batteryCapacity = VehicleUtils.getEnergyCapacity(engineInfo) * EvUnits.J_PER_kWh;

		//provided per vehicle
		initialSoc = (double)matsimVehicle.getAttributes().getAttribute(INITIAL_ENERGY_kWh) * EvUnits.J_PER_kWh;

		if (initialSoc < 0 || initialSoc > batteryCapacity) {
			throw new IllegalArgumentException("Invalid initialSoc/batteryCapacity of vehicle: " + id);
		}
	}

	@Override
	public Id<ElectricVehicle> getId() {
		return id;
	}

	@Override
	public Optional<Vehicle> getMatsimVehicle() {
		return Optional.of(matsimVehicle);
	}

	@Override
	public String getVehicleType() {
		return vehicleType;
	}

	@Override
	public ImmutableList<String> getChargerTypes() {
		return chargerTypes;
	}

	@Override
	public double getInitialSoc() {
		return initialSoc;
	}

	@Override
	public double getBatteryCapacity() {
		return batteryCapacity;
	}
}
