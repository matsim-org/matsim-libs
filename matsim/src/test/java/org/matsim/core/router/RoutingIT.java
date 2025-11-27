/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.router;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.DefaultPrepareForSimModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.speedy.SpeedyDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Arrays;
import java.util.List;

public class RoutingIT {
	/*package*/ static final Logger log = LogManager.getLogger(RoutingIT.class);

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private interface RouterProvider {
		public String getName();

		public LeastCostPathCalculatorFactory getFactory(Network network, TravelDisutility costCalc, TravelTime timeCalc);
	}

	@ParameterizedTest
	@EnumSource(value = RoutingConfigGroup.AccessEgressType.class, names = {"none", "accessEgressModeToLink"})
	void testDijkstra(RoutingConfigGroup.AccessEgressType accessEgressType) {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "Dijkstra";
			}

			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new DijkstraFactory();
			}
		}, accessEgressType);
	}

	@ParameterizedTest
	@EnumSource(value = RoutingConfigGroup.AccessEgressType.class, names = {"none", "accessEgressModeToLink"})
	void testSpeedyDijkstra(RoutingConfigGroup.AccessEgressType accessEgressType) {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "SpeedyDijkstra";
			}

			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new SpeedyDijkstraFactory();
			}
		}, accessEgressType);
	}

	@ParameterizedTest
	@EnumSource(value = RoutingConfigGroup.AccessEgressType.class, names = {"none", "accessEgressModeToLink"})
	void testDijkstraPruneDeadEnds(RoutingConfigGroup.AccessEgressType accessEgressType) {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "DijkstraPruneDeadends";
			}

			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new DijkstraFactory(true);
			}
		}, accessEgressType);
	}

	@ParameterizedTest
	@EnumSource(value = RoutingConfigGroup.AccessEgressType.class, names = {"none", "accessEgressModeToLink"})
	void testAStarEuclidean(RoutingConfigGroup.AccessEgressType accessEgressType) {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "AStarEuclidean";
			}

			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new AStarEuclideanFactory();
			}
		}, accessEgressType);
	}

	@ParameterizedTest
	@EnumSource(value = RoutingConfigGroup.AccessEgressType.class, names = {"none", "accessEgressModeToLink"})
	void testAStarLandmarks(RoutingConfigGroup.AccessEgressType accessEgressType) {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "AStarLandmarks";
			}

			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new AStarLandmarksFactory(2);
			}
		}, accessEgressType);
	}

	@ParameterizedTest
	@EnumSource(value = RoutingConfigGroup.AccessEgressType.class, names = {"none", "accessEgressModeToLink"})
	void testSpeedyALT(RoutingConfigGroup.AccessEgressType accessEgressType) {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "SpeedyALT";
			}

			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new SpeedyALTFactory();
			}
		}, accessEgressType);
	}

	private void doTest(final RouterProvider provider, RoutingConfigGroup.AccessEgressType accessEgress) {
//		final Config config = loadConfig("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/config.xml");
		final Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
		config.routing().setAccessEgressType(accessEgress);
		final Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
//		final String inPlansName = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/plans.xml.gz";
		final String inPlansName = utils.getClassInputDirectory() + "/plans.xml.gz";
		new PopulationReader(scenario).readFile(inPlansName);

		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				install(new ScenarioByInstanceModule(scenario));
				install(AbstractModule.override(List.of(new TripRouterModule()), new AbstractModule() {
					@Override
					public void install() {
						addTravelTimeBinding("car").toInstance(new FreespeedTravelTimeAndDisutility(config.scoring()));
						addTravelDisutilityFactoryBinding("car").toInstance(new OnlyTimeDependentTravelDisutilityFactory());
					}
				}));
				install(new TimeInterpretationModule());
				install(new DefaultPrepareForSimModule());
			}
		});
		injector.getInstance(PrepareForSim.class).run();

		calcRoute(provider, scenario);

		final Scenario referenceScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(referenceScenario.getNetwork()).readFile(config.network().getInputFile());
		new PopulationReader(referenceScenario).readFile(inPlansName);

		PopulationComparison.Result result = PopulationComparison.compare(referenceScenario.getPopulation(), scenario.getPopulation());
		if (result == PopulationComparison.Result.notEqual) {
			new PopulationWriter(referenceScenario.getPopulation(), scenario.getNetwork()).write(this.utils.getOutputDirectory() + "/reference_population.xml.gz");
			new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(this.utils.getOutputDirectory() + "/output_population.xml.gz");
		}
		Assertions.assertEquals(PopulationComparison.Result.equal, result, "different plans files");
	}

	private static void calcRoute(
		final RouterProvider provider,
		final Scenario scenario) {
		log.info("### calcRoute with router " + provider.getName());


		final FreespeedTravelTimeAndDisutility calculator =
			new FreespeedTravelTimeAndDisutility(
				scenario.getConfig().scoring());
		final LeastCostPathCalculatorFactory factory1 = provider.getFactory(
			scenario.getNetwork(),
			calculator,
			calculator);
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				install(AbstractModule.override(Arrays.asList(new TripRouterModule()), new AbstractModule() {
					@Override
					public void install() {
						install(new ScenarioByInstanceModule(scenario));
						addTravelTimeBinding("car").toInstance(calculator);
						install(new TimeInterpretationModule());
						addTravelDisutilityFactoryBinding("car").toInstance(new TravelDisutilityFactory() {
							@Override
							public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
								return calculator;
							}
						});
						bindLeastCostPathCalculatorFactory().toInstance(factory1);
					}
				}));
			}
		});

		final TripRouter tripRouter = injector.getInstance(TripRouter.class);
		final PersonAlgorithm router = new PlanRouter(tripRouter, injector.getInstance(TimeInterpretation.class));

		for (Person p : scenario.getPopulation().getPersons().values()) {
			router.run(p);
		}
	}
}
