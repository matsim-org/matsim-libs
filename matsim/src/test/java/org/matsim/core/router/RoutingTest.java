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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.AStarEuclideanFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarEuclideanFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.testcases.MatsimTestCase;

public class RoutingTest extends MatsimTestCase {

	/*package*/ static final Logger log = Logger.getLogger(RoutingTest.class);

	private interface RouterProvider {
		public String getName();
		public LeastCostPathCalculatorFactory getFactory(Network network, TravelDisutility costCalc, TravelTime timeCalc);
	}

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
	
	public void testDijkstraPruneDeadEnds() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "DijkstraPruneDeadends";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				PreProcessDijkstra preProcessData = new PreProcessDijkstra();
				preProcessData.run(network);
				return new DijkstraFactory(preProcessData);
			}
		});
	}

	public void testFastDijkstraPruneDeadEnds() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "FastDijkstraPruneDeadends";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				PreProcessDijkstra preProcessData = new PreProcessDijkstra();
				preProcessData.run(network);
				return new FastDijkstraFactory(preProcessData);
			}
		});
	}
	
	public void testAStarEuclidean() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "AStarEuclidean";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new AStarEuclideanFactory(network, costCalc);
			}
		});
	}

	public void testFastAStarEuclidean() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "FastAStarEuclidean";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new FastAStarEuclideanFactory(network, costCalc);
			}
		});
	}
	
	public void testAStarLandmarks() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "AStarLandmarks";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new AStarLandmarksFactory(network, costCalc);
			}
		});
	}

	public void testFastAStarLandmarks() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "FastAStarLandmarks";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelDisutility costCalc, final TravelTime timeCalc) {
				return new FastAStarLandmarksFactory(network, costCalc);
			}
		});
	}

	private void doTest(final RouterProvider provider) {
		final Config config = loadConfig("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/config.xml");
		final Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		final String inPlansName = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/plans.xml.gz";
		new MatsimPopulationReader(scenario).readFile(inPlansName);
			
		calcRoute(provider, scenario);

		final Scenario referenceScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(referenceScenario).readFile(config.network().getInputFile());
		new MatsimPopulationReader(referenceScenario).readFile(inPlansName);
		
		assertTrue("different plans files.", PopulationUtils.equalPopulation(referenceScenario.getPopulation(), scenario.getPopulation()));
	}

	private void calcRoute(
			final RouterProvider provider,
			final Scenario scenario) {
		log.info("### calcRoute with router " + provider.getName());

		FreespeedTravelTimeAndDisutility calculator =
			new FreespeedTravelTimeAndDisutility(
					scenario.getConfig().planCalcScore() );

		final TripRouterFactoryBuilderWithDefaults builder =
			new TripRouterFactoryBuilderWithDefaults();
		builder.setLeastCostPathCalculatorFactory(
				provider.getFactory(
					scenario.getNetwork(),
					calculator,
					calculator ) );
		final TripRouterFactory factory = builder.build( scenario );
		final TripRouter tripRouter =
			factory.instantiateAndConfigureTripRouter(
					new RoutingContextImpl(
						calculator,
						calculator ) );
		final PersonAlgorithm router = new PlanRouter( tripRouter );
		
		for ( Person p : scenario.getPopulation().getPersons().values() ) {
			router.run(p);
		}
	}
}
