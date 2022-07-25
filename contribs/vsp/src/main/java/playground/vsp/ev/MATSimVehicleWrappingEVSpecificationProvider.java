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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecificationImpl;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ImmutableElectricVehicleSpecification;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Inject;

class MATSimVehicleWrappingEVSpecificationProvider implements Provider<ElectricFleetSpecification>, IterationStartsListener {

	@Inject
	Population population;

	@Inject
	Vehicles vehicles;

	private ElectricFleetSpecification fleetSpecification;

	@Override
	public ElectricFleetSpecification get() {
		return fleetSpecification;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		//idk whether clearing the existing one or replacing the object is better.
		//but as clearing is not too straightforward (3-5 lines), i chose replacing. tschlenther sep '20
		this.fleetSpecification = new ElectricFleetSpecificationImpl();

		//collect EV ids needed this iteration
		Set<Id<Vehicle>> evIds = new HashSet<>();
		population.getPersons().values().stream()
				.map(person -> person.getSelectedPlan())
				.forEach(plan -> registerEVs(plan));
	}

	private void registerEVs(Plan plan){
		Set<Vehicle> vehSet = TripStructureUtils.getLegs(plan).stream()
				.map(leg -> vehicles.getVehicles().get(VehicleUtils.getVehicleId(plan.getPerson(), leg.getMode())))
				.filter(vehicle -> VehicleUtils.getHbefaTechnology(vehicle.getType().getEngineInformation()) != null &&
						VehicleUtils.getHbefaTechnology(vehicle.getType().getEngineInformation()).equals("electricity"))
				.collect(Collectors.toSet());
		vehSet.forEach(vehicle -> createEV(vehicle));
	}

	private void createEV(Vehicle vehicle) {
		if (this.fleetSpecification.getVehicleSpecifications().containsKey(getWrappedElectricVehicleId(vehicle.getId()))) return;
		var electricVehicleSpecification = ImmutableElectricVehicleSpecification.newBuilder()
				.vehicleType(vehicle.getType().getId().toString())
				.chargerTypes(EVUtils.getChargerTypes(vehicle.getType().getEngineInformation()))
				.initialSoc(EVUtils.getInitialEnergy(vehicle.getType().getEngineInformation()) * EvUnits.J_PER_kWh)
				.batteryCapacity(VehicleUtils.getEnergyCapacity(vehicle.getType().getEngineInformation()) * EvUnits.J_PER_kWh)
				.id(getWrappedElectricVehicleId(vehicle.getId()))
				.build();
		this.fleetSpecification.addVehicleSpecification(electricVehicleSpecification);
	}

	static Id<ElectricVehicle> getWrappedElectricVehicleId(Id<Vehicle> vehicleId) { return Id.create(vehicleId, ElectricVehicle.class); }

}
