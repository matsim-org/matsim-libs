package playground.agarwalamit;/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

/**
 * See a simple example {@link tutorial.programming.example18MultipleSubpopulations.RunSubpopulationsExample} without mode choice
 * Created by amit on 18.06.17.
 */

public class ModeChoiceWithMultipleSubpopulationIT {

	private static final String EQUIL_DIR = "../../examples/scenarios/equil-mixedTraffic/";
	private static final String PLANS_FILE = "../../examples/scenarios/equil-mixedTraffic/plans2000.xml.gz";

	private static final String SUBPOP_ATTRIB_NAME = "subpopulation";
	private static final String SUBPOP1_NAME = "lower"; // half of the persons will fall under this group
	private static final String SUBPOP2_NAME = "upper"; // rest half here.

	@Rule
	public MatsimTestUtils helper = new MatsimTestUtils();

	@Test
	public void run() {
		Config config = ConfigUtils.loadConfig(EQUIL_DIR + "/config-with-mode-vehicles.xml");
		config.controler().setOutputDirectory(helper.getOutputDirectory());
		config.plans().setInputFile(new File(PLANS_FILE).getAbsolutePath());

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// these are things which are required to add another mode as main/network mode
		updateScenarioForMotorbikeAsMainMode(scenario);

		// one can create them seperately and provide person_attribute file to plansConfigGroup
		createSubPopulationAttributes(scenario);

		//  following is required to differentiate the agents based on provide subpopulation attribute name
		scenario.getConfig().plans().setSubpopulationAttributeName(SUBPOP_ATTRIB_NAME); /* This is the default anyway. */

		// clear previous strategies
		scenario.getConfig().strategy().clearStrategySettings();

		// add innovative modules for SUBPOP1_NAME
		{
			StrategyConfigGroup.StrategySettings modeChoiceStrategySettings = new StrategyConfigGroup.StrategySettings() ;
			modeChoiceStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.toString());
			modeChoiceStrategySettings.setSubpopulation(SUBPOP1_NAME);
			modeChoiceStrategySettings.setWeight(0.3);
			scenario.getConfig().strategy().addStrategySettings(modeChoiceStrategySettings);

			// a set of modes for first sub population
			scenario.getConfig().changeMode().setModes(new String[] {"car", "bicycle"} );

			StrategyConfigGroup.StrategySettings changeExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
			changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation(SUBPOP1_NAME);
			changeExpBetaStrategySettings.setWeight(0.7);
			scenario.getConfig().strategy().addStrategySettings(changeExpBetaStrategySettings);
		}

		// add innovative modules for SUBPOP2_NAME
		final String CHANGE_TRIP_MODE_FOR_SUBPOP_2 = DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode.toString().concat(SUBPOP2_NAME);
		{
			StrategyConfigGroup.StrategySettings modeChoiceStrategySettings = new StrategyConfigGroup.StrategySettings() ;
			modeChoiceStrategySettings.setStrategyName(CHANGE_TRIP_MODE_FOR_SUBPOP_2); // a different name is must. Amit June'17
			modeChoiceStrategySettings.setSubpopulation(SUBPOP2_NAME);
			modeChoiceStrategySettings.setWeight(0.3);
			scenario.getConfig().strategy().addStrategySettings(modeChoiceStrategySettings);

			StrategyConfigGroup.StrategySettings changeExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
			changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation(SUBPOP2_NAME);
			changeExpBetaStrategySettings.setWeight(0.7);
			scenario.getConfig().strategy().addStrategySettings(changeExpBetaStrategySettings);
		}

		// disable innovation
		scenario.getConfig().strategy().setFractionOfIterationsToDisableInnovation(0.8);

		Controler controler = new Controler(scenario);

		// this is required to set the different set of available modes for second sub population example.
		// (The name of the innovative module should be same as set in config.strategy())
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding(CHANGE_TRIP_MODE_FOR_SUBPOP_2).toProvider(new javax.inject.Provider<PlanStrategy>() {
					final String[] availableModes = {"car", "motorbike"};
					@Inject Scenario sc;
					@Inject Provider<TripRouter> tripRouterProvider ;
					
					@Override
					public PlanStrategy get() {
						final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new TripsToLegsModule(tripRouterProvider, sc.getConfig().global()));
						builder.addStrategyModule(new ChangeLegMode(sc.getConfig().global().getNumberOfThreads(), availableModes, true));
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
						return builder.build();
					}
				});
			}
		});

		controler.run();
	}

	private void createSubPopulationAttributes (final Scenario scenario) {
		int totalPerson = scenario.getPopulation().getPersons().size();
		// probably, with the newer population version, one can have attributes together with plans. Amit June'17
		for (Id<Person> p : scenario.getPopulation().getPersons().keySet()) {
			int personIdInteger = Integer.valueOf(p.toString());
			if ( personIdInteger < totalPerson /2  ) {
				scenario.getPopulation().getPersonAttributes().putAttribute(p.toString(), SUBPOP_ATTRIB_NAME, SUBPOP1_NAME);
			} else {
				scenario.getPopulation().getPersonAttributes().putAttribute(p.toString(), SUBPOP_ATTRIB_NAME, SUBPOP2_NAME);
			}
		}
	}

	private void updateScenarioForMotorbikeAsMainMode(final Scenario scenario) {
		// add motorbike which is not already present in the scenario
		Set<String> allowedMode = new HashSet<>();
		allowedMode.add("car");
		allowedMode.add("motorbike");
		allowedMode.add("bicycle");

		VehicleType motorbike = scenario.getVehicles().getFactory().createVehicleType(Id.create("motorbike",VehicleType.class));
		motorbike.setMaximumVelocity(60/3.6);
		motorbike.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(motorbike);

		scenario.getConfig().plansCalcRoute().setNetworkModes(allowedMode);
		scenario.getConfig().qsim().setMainModes(allowedMode);
		scenario.getConfig().travelTimeCalculator().setAnalyzedModes("car,motorbike,bicycle");

		scenario.getConfig().planCalcScore().getOrCreateModeParams("motorbike"); // this will set default scoring params for motorbike

		for (Link l : scenario.getNetwork().getLinks().values()) {
			l.setAllowedModes(allowedMode);
		}
	}
}
