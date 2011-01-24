/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.integration.controler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class LeastCostPathCalculatorFactoryIntegrationTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testSetLeastCostPathCalculatorFactory() {
		Config config = this.utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(1);
		config.global().setNumberOfThreads(1);
		config.setParam("strategy", "ModuleProbability_1", "0.0");
		config.setParam("strategy", "ModuleProbability_2", "1.0");
		Scenario s = new ScenarioLoaderImpl(config).loadScenario();

		DummyRoutingAlgorithmFactory factory = new DummyRoutingAlgorithmFactory(s);

		Controler c = new Controler((ScenarioImpl) s);
		c.setLeastCostPathCalculatorFactory(factory);
		c.setCreateGraphs(false);
		c.run();

		Assert.assertEquals(2, factory.counter.get());
	}

	private static class DummyRoutingAlgorithm implements LeastCostPathCalculator {

		private final AtomicInteger counter;
		private final Node node;

		public DummyRoutingAlgorithm(final AtomicInteger counter, final Node node) {
			this.counter = counter;
			this.node = node;
		}

		@Override
		public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime) {
			this.counter.incrementAndGet();
			List<Node> nodes = new ArrayList<Node>(1);
			nodes.add(node); // add a single node to the route, this will not work in the simulation, but ok for test
			List<Link> links = new ArrayList<Link>(0);
			return new Path(nodes, links, 1.0, 1.0);
		}

	}

	private static class DummyRoutingAlgorithmFactory implements LeastCostPathCalculatorFactory {

		private final AtomicInteger counter = new AtomicInteger(0);
		private final Node node1;

		public DummyRoutingAlgorithmFactory(final Scenario s) {
			this.node1 = s.getNetwork().getNodes().get(s.createId("1"));
		}

		@Override
		public LeastCostPathCalculator createPathCalculator(Network network, TravelCost travelCosts, TravelTime travelTimes) {
			return new DummyRoutingAlgorithm(this.counter, this.node1);
		}

	}

}
