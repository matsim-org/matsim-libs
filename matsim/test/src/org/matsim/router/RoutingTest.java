/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.router.AStarEuclidean;
import org.matsim.router.AStarLandmarks;
import org.matsim.router.Dijkstra;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.PreProcessDijkstra;
import org.matsim.router.util.PreProcessEuclidean;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelMinCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

public class RoutingTest extends MatsimTestCase {

	private boolean initRan = false;
	private long referenceChecksum = -1L;
	private NetworkLayer network = null;
	private Plans population = null;

	private interface RouterProvider {
		public String getName();
		public LeastCostPathCalculator getRouter(NetworkLayer network, TravelMinCostI costCalc, TravelTimeI timeCalc);
	}

	public void testDijkstra() {
		doTest(new RouterProvider() {
			public String getName() {
				return "Dijkstra";
			}
			public LeastCostPathCalculator getRouter(final NetworkLayer network, final TravelMinCostI costCalc, final TravelTimeI timeCalc) {
				return new Dijkstra(network, costCalc, timeCalc, null);
			}
		});
	}

	public void testDijkstraPruneDeadEnds() {
		doTest(new RouterProvider() {
			public String getName() {
				return "DijkstraPruneDeadends";
			}
			public LeastCostPathCalculator getRouter(final NetworkLayer network, final TravelMinCostI costCalc, final TravelTimeI timeCalc) {
				PreProcessDijkstra preProcessData = new PreProcessDijkstra();
				long now = System.currentTimeMillis();
				preProcessData.run(network);
				System.out.println("Elapsed time for preprocessing:\n" + Gbl.printTimeDiff(System.currentTimeMillis(), now));
				return new Dijkstra(network, costCalc, timeCalc, preProcessData);
			}
		});
	}

	public void testAStarEuclidean() {
		doTest(new RouterProvider() {
			public String getName() {
				return "AStarEuclidean";
			}
			public LeastCostPathCalculator getRouter(final NetworkLayer network, final TravelMinCostI costCalc, final TravelTimeI timeCalc) {
				PreProcessEuclidean preProcessData = new PreProcessEuclidean(costCalc);
				long now = System.currentTimeMillis();
				preProcessData.run(network);
				System.out.println("Elapsed time for preprocessing:\n" + Gbl.printTimeDiff(System.currentTimeMillis(), now));
				return new AStarEuclidean(network, preProcessData, timeCalc);
			}
		});
	}

	public void testAStarLandmarks() {
		doTest(new RouterProvider() {
			public String getName() {
				return "AStarLandmarks";
			}
			public LeastCostPathCalculator getRouter(final NetworkLayer network, final TravelMinCostI costCalc, final TravelTimeI timeCalc) {
				PreProcessLandmarks preProcessData = new PreProcessLandmarks(costCalc);
				long now = System.currentTimeMillis();
				preProcessData.run(network);
				System.out.println("Elapsed time for preprocessing:\n" + Gbl.printTimeDiff(System.currentTimeMillis(), now));
				return new AStarLandmarks(network, preProcessData, timeCalc);
			}
		});
	}

	private void doTest(final RouterProvider provider) {
		init("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/config.xml");

		String outPlansName = getOutputDirectory() + provider.getName() + ".plans.xml.gz";

		calcRoute(provider, this.network, this.population);
		PlansWriter plansWriter = new PlansWriter(this.population, outPlansName,
				Gbl.getConfig().plans().getOutputVersion());
		plansWriter.write();
		final long routerChecksum = CRCChecksum.getCRCFromGZFile(outPlansName);
		System.out.println("routerChecksum = " + routerChecksum + " file: " + outPlansName);
		assertEquals(this.referenceChecksum, routerChecksum);
		System.out.println();
	}

	private void init(final String configFile) {
		loadConfig(configFile);

		if (this.initRan) return;

		this.network = readNetwork();
		String inPlansName = "test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/plans.xml.gz";
		this.population = readPlans(inPlansName);
		this.referenceChecksum = CRCChecksum.getCRCFromGZFile(inPlansName);
		System.out.println("Reference checksum = " + this.referenceChecksum + " file: " + inPlansName);
		System.out.println();

		this.initRan = true;
	}

	private Plans readPlans(String inPlansName) {
		Plans plans = new Plans();
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(inPlansName);
		return plans;
	}

	private NetworkLayer readNetwork() {
		System.out.println("  reading the network...");
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");
		return network;
	}

	private void calcRoute(final RouterProvider provider, final NetworkLayer network, final Plans plans) {

		System.out.println("### calcRoute with router " + provider.getName());

		FreespeedTravelTimeCost calculator = new FreespeedTravelTimeCost();
		LeastCostPathCalculator routingAlgo = provider.getRouter(network, calculator, calculator);

		PlansCalcRoute router = null;
		router = new PlansCalcRoute(network, calculator, calculator, false, routingAlgo, routingAlgo);
		plans.addAlgorithm(router);
		long now = System.currentTimeMillis();
		plans.printPlansCount();
		if (Gbl.getConfig().plans().switchOffPlansStreaming()) {
			now = System.currentTimeMillis();
			plans.runAlgorithms();
		}
		plans.clearAlgorithms();

		System.out.println("Elapsed time for routing using " +
				routingAlgo.getClass().getName() + ":\n" +
				Gbl.printTimeDiff(System.currentTimeMillis(), now));

		if (routingAlgo instanceof Dijkstra) {
			((Dijkstra) routingAlgo).printInformation();
		}
	}

}