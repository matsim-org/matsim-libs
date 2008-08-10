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

package org.matsim.router;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.PreProcessDijkstra;
import org.matsim.router.util.PreProcessEuclidean;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelMinCost;
import org.matsim.router.util.TravelTime;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;
import org.matsim.world.World;

public class RoutingTest extends MatsimTestCase {

	/*package*/ static final Logger log = Logger.getLogger(RoutingTest.class);

	private interface RouterProvider {
		public String getName();
		public LeastCostPathCalculator getRouter(NetworkLayer network, TravelMinCost costCalc, TravelTime timeCalc);
	}

	public void testDijkstra() {
		doTest(new RouterProvider() {
			public String getName() {
				return "Dijkstra";
			}
			public LeastCostPathCalculator getRouter(final NetworkLayer network, final TravelMinCost costCalc, final TravelTime timeCalc) {
				return new Dijkstra(network, costCalc, timeCalc, null);
			}
		});
	}

	public void testDijkstraPruneDeadEnds() {
		doTest(new RouterProvider() {
			public String getName() {
				return "DijkstraPruneDeadends";
			}
			public LeastCostPathCalculator getRouter(final NetworkLayer network, final TravelMinCost costCalc, final TravelTime timeCalc) {
				PreProcessDijkstra preProcessData = new PreProcessDijkstra();
				long now = System.currentTimeMillis();
				preProcessData.run(network);
				log.info("Elapsed time for preprocessing:\n" + Gbl.printTimeDiff(System.currentTimeMillis(), now));
				return new Dijkstra(network, costCalc, timeCalc, preProcessData);
			}
		});
	}

	public void testAStarEuclidean() {
		doTest(new RouterProvider() {
			public String getName() {
				return "AStarEuclidean";
			}
			public LeastCostPathCalculator getRouter(final NetworkLayer network, final TravelMinCost costCalc, final TravelTime timeCalc) {
				PreProcessEuclidean preProcessData = new PreProcessEuclidean(costCalc);
				long now = System.currentTimeMillis();
				preProcessData.run(network);
				log.info("Elapsed time for preprocessing:\n" + Gbl.printTimeDiff(System.currentTimeMillis(), now));
				return new AStarEuclidean(network, preProcessData, timeCalc);
			}
		});
	}

	public void testAStarLandmarks() {
		doTest(new RouterProvider() {
			public String getName() {
				return "AStarLandmarks";
			}
			public LeastCostPathCalculator getRouter(final NetworkLayer network, final TravelMinCost costCalc, final TravelTime timeCalc) {
				PreProcessLandmarks preProcessData = new PreProcessLandmarks(costCalc);
				long now = System.currentTimeMillis();
				preProcessData.run(network);
				log.info("Elapsed time for preprocessing:\n" + Gbl.printTimeDiff(System.currentTimeMillis(), now));
				return new AStarLandmarks(network, preProcessData, timeCalc);
			}
		});
	}

	private void doTest(final RouterProvider provider) {
		Config config = loadConfig("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/config.xml");
		World world = Gbl.createWorld();

		NetworkLayer network = (NetworkLayer) world.createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());

		String inPlansName = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/plans.xml.gz";
		Population population = new Population(Population.NO_STREAMING);
		new MatsimPopulationReader(population).readFile(inPlansName);
		population.printPlansCount();
		long referenceChecksum = CRCChecksum.getCRCFromGZFile(inPlansName);
		log.info("Reference checksum = " + referenceChecksum + " file: " + inPlansName);

		String outPlansName = getOutputDirectory() + provider.getName() + ".plans.xml.gz";

		calcRoute(provider, network, population);
		PopulationWriter plansWriter = new PopulationWriter(population, outPlansName,
				config.plans().getOutputVersion());
		plansWriter.write();
		final long routerChecksum = CRCChecksum.getCRCFromGZFile(outPlansName);
		log.info("routerChecksum = " + routerChecksum + " file: " + outPlansName);
		assertEquals(referenceChecksum, routerChecksum);
	}

	private void calcRoute(final RouterProvider provider, final NetworkLayer network, final Population population) {
		log.info("### calcRoute with router " + provider.getName());

		FreespeedTravelTimeCost calculator = new FreespeedTravelTimeCost();
		LeastCostPathCalculator routingAlgo = provider.getRouter(network, calculator, calculator);

		PlansCalcRoute router = null;
		router = new PlansCalcRoute(routingAlgo, routingAlgo);
		long now = System.currentTimeMillis();
		router.run(population);

		log.info("Elapsed time for routing using " +
				routingAlgo.getClass().getName() + ": " +
				Gbl.printTimeDiff(System.currentTimeMillis(), now));

		if (routingAlgo instanceof Dijkstra) {
			((Dijkstra) routingAlgo).printInformation();
		}
	}

}