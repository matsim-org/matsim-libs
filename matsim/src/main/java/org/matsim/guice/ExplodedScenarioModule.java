package org.matsim.guice;


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


public class ExplodedScenarioModule extends AbstractModule {

	private final Scenario scenario;

	public ExplodedScenarioModule(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void install() {
		bind(Scenario.class).toInstance(scenario);
		bind(Network.class).toInstance(scenario.getNetwork());
		bind(Population.class).toInstance(scenario.getPopulation());
		bind(PopulationFactory.class).toInstance(scenario.getPopulation().getFactory());
		bind(ActivityFacilities.class).toInstance(scenario.getActivityFacilities());
		bind(Households.class).toInstance(scenario.getHouseholds());
		bind(Vehicles.class).toInstance(scenario.getVehicles());
		bind(Lanes.class).toInstance(scenario.getLanes());
		if (getConfig().transit().isUseTransit()) {
			bind(TransitSchedule.class).toInstance(scenario.getTransitSchedule());
			bind(Vehicles.class).annotatedWith(Transit.class).toInstance(scenario.getTransitVehicles());
		}

	}
}
