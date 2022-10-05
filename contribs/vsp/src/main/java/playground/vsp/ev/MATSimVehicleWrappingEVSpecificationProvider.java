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

package playground.vsp.ev;

import static org.matsim.contrib.ev.fleet.ElectricVehicleSpecificationWithMatsimVehicle.EV_ENGINE_HBEFA_TECHNOLOGY;

import javax.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecificationImpl;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecificationWithMatsimVehicle;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Inject;

class MATSimVehicleWrappingEVSpecificationProvider
		implements Provider<ElectricFleetSpecification>, IterationStartsListener {

	@Inject
	Population population;

	@Inject
	Vehicles vehicles;

	private final ElectricFleetSpecification fleetSpecification = new ElectricFleetSpecificationImpl();

	@Override
	public ElectricFleetSpecification get() {
		return fleetSpecification;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		fleetSpecification.clear();

		//collect EV ids needed this iteration
		population.getPersons().values().stream().map(HasPlansAndId::getSelectedPlan).forEach(this::registerEVs);
	}

	private void registerEVs(Plan plan) {
		TripStructureUtils.getLegs(plan)
				.stream()
				.map(leg -> vehicles.getVehicles().get(VehicleUtils.getVehicleId(plan.getPerson(), leg.getMode())))
				.filter(vehicle -> EV_ENGINE_HBEFA_TECHNOLOGY.equals(
						VehicleUtils.getHbefaTechnology(vehicle.getType().getEngineInformation())))
				.forEach(this::createEV);
	}

	private void createEV(Vehicle vehicle) {
		var evId = Id.create(vehicle.getId(), ElectricVehicle.class);
		if (!fleetSpecification.getVehicleSpecifications().containsKey(evId)) {
			fleetSpecification.addVehicleSpecification(new ElectricVehicleSpecificationWithMatsimVehicle(vehicle));
		}
	}
}
