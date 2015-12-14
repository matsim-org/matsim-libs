package org.matsim.core.scenario;

import com.google.inject.Provides;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.AbstractModule;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;
import javax.inject.Provider;

public class ScenarioByInstanceModule extends AbstractModule {

	private final Scenario scenario;

	public ScenarioByInstanceModule(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void install() {
		bind(Scenario.class).toInstance(scenario);
		if (getConfig().transit().isUseTransit()) {
			bind(TransitSchedule.class).toProvider(TransitScheduleProvider.class);
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

	private static class TransitScheduleProvider implements Provider<TransitSchedule> {

		@Inject
		Scenario scenario;

		@Override
		public TransitSchedule get() {
			return scenario.getTransitSchedule();
		}

	}

}
