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

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class RoutingIT {
	/*package*/ static final Logger log = Logger.getLogger(RoutingIT.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private interface RouterProvider {
		public String getName();
		public LeastCostPathCalculatorFactory getFactory(Network network, TravelDisutility costCalc, TravelTime timeCalc);
	}
	@Test
	public void testDijkstra() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "Dijkstra";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new DijkstraFactory();
			}
		});
	}
	@Test
	public void testFastDijkstra() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "FastDijkstra";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new FastDijkstraFactory();
			}
		});
	}
	@Test	
	public void testDijkstraPruneDeadEnds() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "DijkstraPruneDeadends";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new DijkstraFactory(true);
			}
		});
	}
	@Test
	public void testFastDijkstraPruneDeadEnds() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "FastDijkstraPruneDeadends";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new FastDijkstraFactory();
			}
		});
	}
	@Test	
	public void testAStarEuclidean() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "AStarEuclidean";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new AStarEuclideanFactory();
			}
		});
	}
	@Test
	public void testFastAStarEuclidean() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "FastAStarEuclidean";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new FastAStarEuclideanFactory();
			}
		});
	}
	@Test	
	public void testAStarLandmarks() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "AStarLandmarks";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new AStarLandmarksFactory();
			}
		});
	}
	@Test
	public void testFastAStarLandmarks() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "FastAStarLandmarks";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new FastAStarLandmarksFactory();
			}
		});
	}

	private void doTest(final RouterProvider provider) {
//		final Config config = loadConfig("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/config.xml");
		final Config config = ConfigUtils.loadConfig( utils.getClassInputDirectory() + "/config.xml" );
		final Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
//		final String inPlansName = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/plans.xml.gz";
		final String inPlansName = utils.getClassInputDirectory() + "/plans.xml.gz" ;
		new PopulationReader(scenario).readFile(inPlansName);
			
		calcRoute(provider, scenario);

		final Scenario referenceScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(referenceScenario.getNetwork()).readFile(config.network().getInputFile());
		new PopulationReader(referenceScenario).readFile(inPlansName);
		
		final boolean isEqual = PopulationUtils.equalPopulation(referenceScenario.getPopulation(), scenario.getPopulation());
		if ( !isEqual ) {
			new PopulationWriter(referenceScenario.getPopulation(), scenario.getNetwork()).write(this.utils.getOutputDirectory() + "/reference_population.xml.gz");
			new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(this.utils.getOutputDirectory() + "/output_population.xml.gz");
		}
		Assert.assertTrue("different plans files.", isEqual);
	}

	private static void calcRoute(
			final RouterProvider provider,
			final Scenario scenario) {
		log.info("### calcRoute with router " + provider.getName());


		final FreespeedTravelTimeAndDisutility calculator =
				new FreespeedTravelTimeAndDisutility(
						scenario.getConfig().planCalcScore() );
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
		final PersonAlgorithm router = new PlanRouter(tripRouter);
		
		for ( Person p : scenario.getPopulation().getPersons().values() ) {
			router.run(p);
		}
	}
}