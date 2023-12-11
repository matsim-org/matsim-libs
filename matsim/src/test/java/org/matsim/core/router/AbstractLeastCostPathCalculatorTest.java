/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractLeastCostPathCalculatorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public abstract class AbstractLeastCostPathCalculatorTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	protected abstract LeastCostPathCalculator getLeastCostPathCalculator(final Network network);

	private static final String MODE_RESTRICTION_NOT_SUPPORTED = "Router algo does not support mode restrictions. ";

	@Test
	void testCalcLeastCostPath_Normal() throws SAXException, ParserConfigurationException, IOException {
		Config config = utils.loadConfig((String)null);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
		Node node12 = network.getNodes().get(Id.create("12", Node.class));
		Node node15 = network.getNodes().get(Id.create("15", Node.class));

		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(network);
		Path path = routerAlgo.calcLeastCostPath(node12, node15, 8.0*3600, null, null);

		assertEquals(4, path.nodes.size(), "number of nodes wrong.");
		assertEquals(3, path.links.size(), "number of links wrong.");
		assertEquals(network.getNodes().get(Id.create("12", Node.class)), path.nodes.get(0));
		assertEquals(network.getNodes().get(Id.create("13", Node.class)), path.nodes.get(1));
		assertEquals(network.getNodes().get(Id.create("14", Node.class)), path.nodes.get(2));
		assertEquals(network.getNodes().get(Id.create("15", Node.class)), path.nodes.get(3));
		assertEquals(network.getLinks().get(Id.create("20", Link.class)), path.links.get(0));
		assertEquals(network.getLinks().get(Id.create("21", Link.class)), path.links.get(1));
		assertEquals(network.getLinks().get(Id.create("22", Link.class)), path.links.get(2));
	}

	@Test
	void testCalcLeastCostPath_SameFromTo() throws SAXException, ParserConfigurationException, IOException {
		Scenario scenario = ScenarioUtils.createScenario(utils.loadConfig((String)null));
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
		Node node12 = network.getNodes().get(Id.create("12", Node.class));

		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(network);
		Path path = routerAlgo.calcLeastCostPath(node12, node12, 8.0*3600, null, null);

		assertEquals(1, path.nodes.size(), "number of nodes wrong.");
		assertEquals(0, path.links.size(), "number of links wrong.");
		assertEquals(network.getNodes().get(Id.create("12", Node.class)), path.nodes.get(0));
	}

}
