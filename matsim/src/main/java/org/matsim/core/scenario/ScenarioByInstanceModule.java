package org.matsim.core.scenario;

import com.google.inject.Provides;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.AbstractModule;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.pt.transitSchedule.api.Transit;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import javax.inject.Inject;
import javax.inject.Provider;

public class ScenarioByInstanceModule extends AbstractModule {

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
