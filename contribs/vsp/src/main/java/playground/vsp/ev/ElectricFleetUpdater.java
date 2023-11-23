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

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Inject;

class ElectricFleetUpdater implements IterationStartsListener {

	@Inject
	private Population population;

	@Inject
	private Vehicles vehicles;

	@Inject
	private ElectricFleetSpecification fleetSpecification;

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		fleetSpecification.clear();

		//collect EV ids needed this iteration
		population.getPersons().values().stream().map(HasPlansAndId::getSelectedPlan).forEach(this::registerEVs);
	}

	private void registerEVs(Plan plan) {
		var modalVehicles = TripStructureUtils.getLegs(plan).stream()
				.map(leg -> vehicles.getVehicles().get(VehicleUtils.getVehicleId(plan.getPerson(), leg.getMode())))
				.toList();
		ElectricFleetUtils.createAndAddVehicleSpecificationsFromMatsimVehicles(fleetSpecification, modalVehicles );
	}
}
