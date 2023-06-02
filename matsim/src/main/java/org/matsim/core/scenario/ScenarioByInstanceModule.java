
/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioByInstanceModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.scenario;

import com.google.inject.Provides;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.AbstractModule;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;
import org.matsim.lanes.Lanes;
import org.matsim.pt.transitSchedule.api.Transit;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public final class ScenarioByInstanceModule extends AbstractModule {

	private final Scenario scenario;

	public ScenarioByInstanceModule(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void install() {
		// if no network provided, assume it comes from somewhere else, and this module just provides
		// the scenario "elements"
		if ( scenario != null ) bind(Scenario.class).toInstance(scenario);
		if (getConfig().transit().isUseTransit()) {
			bind(TransitSchedule.class).toProvider(TransitScheduleProvider.class);
			bind(Vehicles.class).annotatedWith(Transit.class).toProvider(TransitVehiclesProvider.class);
		}
	}

	@Provides Network provideNetwork(Scenario scenario) {
		return scenario.getNetwork();
	}

	@Provides Population providePopulation(Scenario scenario) {
		return scenario.getPopulation();
	}

	@Provides PopulationFactory providePopulationFactory(Population population) {
		return population.getFactory();
	}

	@Provides ActivityFacilities provideActivityFacilities(Scenario scenario) {
		return scenario.getActivityFacilities();
	}

	@Provides
	Households provideHouseholds(Scenario scenario) {
		return scenario.getHouseholds();
	}

	@Provides
	Vehicles provideVehicles(Scenario scenario) {
		return scenario.getVehicles();
	}

	@Provides
	Lanes provideLanes(Scenario scenario) {
		return scenario.getLanes();
	}

	private static class TransitScheduleProvider implements Provider<TransitSchedule> {

		@Inject
		Scenario scenario;

		@Override
		public TransitSchedule get() {
			return scenario.getTransitSchedule();
		}

	}

	private static class TransitVehiclesProvider implements Provider<Vehicles> {

		@Inject
		Scenario scenario;

		@Override
		public Vehicles get() {
			return scenario.getTransitVehicles();
		}
	}
}
