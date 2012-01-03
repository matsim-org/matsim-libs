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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarEuclideanFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarEuclideanFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

public class RoutingTest extends MatsimTestCase {

	/*package*/ static final Logger log = Logger.getLogger(RoutingTest.class);

	private interface RouterProvider {
		public String getName();
		public LeastCostPathCalculatorFactory getFactory(Network network, TravelMinCost costCalc, TravelTime timeCalc);
	}

	public void testDijkstra() {
		doTest(new RouterProvider() {
			@Override
			public String getName() {
				return "Dijkstra";
			}
			@Override
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
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
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
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
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
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
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
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
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
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
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
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
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
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
			public LeastCostPathCalculatorFactory getFactory(final Network network, final TravelMinCost costCalc, final TravelTime timeCalc) {
				return new FastAStarLandmarksFactory(network, costCalc);
			}
		});
	}
	
	private void doTest(final RouterProvider provider) {
		Config config = loadConfig("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/config.xml");

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());

		String inPlansName = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/plans.xml.gz";
		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(inPlansName);
		long referenceChecksum = CRCChecksum.getCRCFromFile(inPlansName);
		log.info("Reference checksum = " + referenceChecksum + " file: " + inPlansName);

		String outPlansName = getOutputDirectory() + provider.getName() + ".plans.xml.gz";

		calcRoute(provider, network, population, config);
		new PopulationWriter(population, network).write(outPlansName);
		final long routerChecksum = CRCChecksum.getCRCFromFile(outPlansName);
		log.info("routerChecksum = " + routerChecksum + " file: " + outPlansName);
		assertEquals("different plans files.", referenceChecksum, routerChecksum);
	}

	private void calcRoute(final RouterProvider provider, final Network network, final Population population, final Config config) {
		log.info("### calcRoute with router " + provider.getName());

		FreespeedTravelTimeCost calculator = new FreespeedTravelTimeCost(config.planCalcScore());

		PlansCalcRoute router = null;
		router = new PlansCalcRoute(config.plansCalcRoute(), network, calculator, calculator, provider.getFactory(network, calculator, calculator), ((PopulationFactoryImpl) population.getFactory()).getModeRouteFactory());
		router.run(population);
	}

}