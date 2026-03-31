/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.network.algorithms;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser
 */
public class MultimodalNetworkCleanerTest {

	@Test
	@SuppressWarnings("deprecation")
	void testRun_singleMode() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet(TransportMode.car));
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_singleMode_separateLink() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		Node node1 = network.getNodes().get(f.nodeIds[1]);
		Node node3 = network.getNodes().get(f.nodeIds[3]);
		Node node4 = network.getNodes().get(f.nodeIds[4]);
		Node node6 = network.getNodes().get(f.nodeIds[6]);
		network.addLink(network.getFactory().createLink(f.linkIds[10], node1, node4));
		network.addLink(network.getFactory().createLink(f.linkIds[11], node6, node3));
		network.getLinks().get(f.linkIds[10]).setAllowedModes(f.modesW);
		network.getLinks().get(f.linkIds[11]).setAllowedModes(f.modesC);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet(TransportMode.car));
		Assertions.assertEquals(9, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[10]).getAllowedModes());
		Assertions.assertNull(network.getLinks().get(f.linkIds[11]));

		cleaner.run(createHashSet(TransportMode.walk));
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertNull(network.getLinks().get(f.linkIds[10]));
		Assertions.assertNull(network.getLinks().get(f.linkIds[11]));
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_singleInexistantMode() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet("other"));
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_singleMode_singleSink() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		Node node1 = network.getNodes().get(f.nodeIds[1]);
		Node node3 = network.getNodes().get(f.nodeIds[3]);
		Node node10 = network.getFactory().createNode(f.nodeIds[10], new Coord(0, 200));
		Node node11 = network.getFactory().createNode(f.nodeIds[11], new Coord(200, 200));
		network.addNode(node10);
		network.addNode(node11);
		network.addLink(network.getFactory().createLink(f.linkIds[10], node1, node10));
		network.addLink(network.getFactory().createLink(f.linkIds[11], node3, node11));
		network.getLinks().get(f.linkIds[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.linkIds[11]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assertions.assertEquals(10, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(8, network.getNodes().size(), "wrong number of nodes.");

		cleaner.run(createHashSet(TransportMode.car));
		Assertions.assertEquals(9, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(7, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[11]).getAllowedModes());
		Assertions.assertNull(network.getLinks().get(f.linkIds[10]));

		Assertions.assertEquals(9, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(7, network.getNodes().size(), "wrong number of nodes.");

		cleaner.run(createHashSet(TransportMode.walk));
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertNull(network.getLinks().get(f.linkIds[11]));

		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_singleMode_singleSinkIntegrated() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		network.getLinks().get(f.linkIds[1]).setAllowedModes(f.modesCW); // integrate the sinks into the existing network
		network.getLinks().get(f.linkIds[8]).setAllowedModes(f.modesCW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");

		cleaner.run(createHashSet(TransportMode.car));
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesCW, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes()); // only remove mode, not link!

		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");

		cleaner.run(createHashSet(TransportMode.walk));
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes()); // only remove mode, not link!
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());

		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_singleMode_doubleSink() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		Node node1 = network.getNodes().get(f.nodeIds[1]);
		Node node2 = network.getNodes().get(f.nodeIds[2]);
		Node node3 = network.getNodes().get(f.nodeIds[3]);
		Node node10 = network.getFactory().createNode(f.nodeIds[10], new Coord(0, 200));
		Node node11 = network.getFactory().createNode(f.nodeIds[11], new Coord(200, 200));
		network.addNode(node10);
		network.addNode(node11);
		network.addLink(network.getFactory().createLink(f.linkIds[10], node1, node10));
		network.addLink(network.getFactory().createLink(f.linkIds[11], node2, node10));
		network.addLink(network.getFactory().createLink(f.linkIds[12], node2, node11));
		network.addLink(network.getFactory().createLink(f.linkIds[13], node3, node11));
		network.getLinks().get(f.linkIds[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.linkIds[11]).setAllowedModes(f.modesC);
		network.getLinks().get(f.linkIds[12]).setAllowedModes(f.modesW);
		network.getLinks().get(f.linkIds[13]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assertions.assertEquals(12, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(8, network.getNodes().size(), "wrong number of nodes.");

		cleaner.run(createHashSet(TransportMode.walk));

		Assertions.assertEquals(10, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(7, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[10]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[11]).getAllowedModes());
		Assertions.assertNull(network.getLinks().get(f.linkIds[12]));
		Assertions.assertNull(network.getLinks().get(f.linkIds[13]));

		cleaner.run(createHashSet(TransportMode.car));
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertNull(network.getLinks().get(f.linkIds[10]));
		Assertions.assertNull(network.getLinks().get(f.linkIds[11]));
	}


	@Test
	@SuppressWarnings("deprecation")
	void testRun_singleMode_singleSource() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		Node node1 = network.getNodes().get(f.nodeIds[1]);
		Node node3 = network.getNodes().get(f.nodeIds[3]);
		Node node10 = network.getFactory().createNode(f.nodeIds[10], new Coord(0, 200));
		Node node11 = network.getFactory().createNode(f.nodeIds[11], new Coord(200, 200));
		network.addNode(node10);
		network.addNode(node11);
		network.addLink(network.getFactory().createLink(f.linkIds[10], node10, node1));
		network.addLink(network.getFactory().createLink(f.linkIds[11], node11, node3));
		network.getLinks().get(f.linkIds[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.linkIds[11]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assertions.assertEquals(10, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(8, network.getNodes().size(), "wrong number of nodes.");

		cleaner.run(createHashSet(TransportMode.car));
		Assertions.assertEquals(9, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(7, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[11]).getAllowedModes());
		Assertions.assertNull(network.getLinks().get(f.linkIds[10]));

		Assertions.assertEquals(9, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(7, network.getNodes().size(), "wrong number of nodes.");

		cleaner.run(createHashSet(TransportMode.walk));
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertNull(network.getLinks().get(f.linkIds[11]));

		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRemoveNodesWithoutLinks() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		network.addNode(network.getFactory().createNode(f.nodeIds[10], new Coord(300, 300)));
		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(7, network.getNodes().size(), "wrong number of nodes.");

		cleaner.run(createHashSet(TransportMode.walk));

		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links after cleaning.");
		Assertions.assertEquals(7, network.getNodes().size(), "empty node should not be removed by cleaning.");

		NetworkUtils.removeNodesWithoutLinks(network);

		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links after cleaning.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes after cleaning.");
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_singleMode_doubleSource() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		Node node1 = network.getNodes().get(f.nodeIds[1]);
		Node node2 = network.getNodes().get(f.nodeIds[2]);
		Node node3 = network.getNodes().get(f.nodeIds[3]);
		Node node10 = network.getFactory().createNode(f.nodeIds[10], new Coord(0, 200));
		Node node11 = network.getFactory().createNode(f.nodeIds[11], new Coord(200, 200));
		network.addNode(node10);
		network.addNode(node11);
		network.addLink(network.getFactory().createLink(f.linkIds[10], node10, node1));
		network.addLink(network.getFactory().createLink(f.linkIds[11], node10, node2));
		network.addLink(network.getFactory().createLink(f.linkIds[12], node11, node2));
		network.addLink(network.getFactory().createLink(f.linkIds[13], node11, node3));
		network.getLinks().get(f.linkIds[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.linkIds[11]).setAllowedModes(f.modesC);
		network.getLinks().get(f.linkIds[12]).setAllowedModes(f.modesW);
		network.getLinks().get(f.linkIds[13]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assertions.assertEquals(12, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(8, network.getNodes().size(), "wrong number of nodes.");

		cleaner.run(createHashSet(TransportMode.walk));

		Assertions.assertEquals(10, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(7, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[10]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[11]).getAllowedModes());
		Assertions.assertNull(network.getLinks().get(f.linkIds[12]));
		Assertions.assertNull(network.getLinks().get(f.linkIds[13]));

		cleaner.run(createHashSet(TransportMode.car));
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertNull(network.getLinks().get(f.linkIds[10]));
		Assertions.assertNull(network.getLinks().get(f.linkIds[11]));
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_multipleModes() {
		Fixture f = new MultimodeFixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet(TransportMode.car, TransportMode.walk));
		Assertions.assertEquals(12, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(9, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[9]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[10]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[11]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[12]).getAllowedModes());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_multipleModes_doubleSink() {
		Fixture f = new MultimodeFixture();
		Network network = f.scenario.getNetwork();

		Node node2 = network.getNodes().get(f.nodeIds[2]);
		Node node3 = network.getNodes().get(f.nodeIds[3]);
		Node node10 = network.getFactory().createNode(f.nodeIds[10], new Coord(200, 200));
		network.addNode(node10);
		network.addLink(network.getFactory().createLink(f.linkIds[18], node2, node10));
		network.addLink(network.getFactory().createLink(f.linkIds[19], node3, node10));

		Assertions.assertEquals(14, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(10, network.getNodes().size(), "wrong number of nodes.");

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet(TransportMode.car, TransportMode.walk));
		Assertions.assertEquals(12, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(9, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[9]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[10]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[11]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[12]).getAllowedModes());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_multipleModes_doubleSource() {
		Fixture f = new MultimodeFixture();
		Network network = f.scenario.getNetwork();

		Node node2 = network.getNodes().get(f.nodeIds[2]);
		Node node3 = network.getNodes().get(f.nodeIds[3]);
		Node node10 = network.getFactory().createNode(f.nodeIds[10], new Coord(200, 200));
		network.addNode(node10);
		network.addLink(network.getFactory().createLink(f.linkIds[18], node10, node2));
		network.addLink(network.getFactory().createLink(f.linkIds[19], node10, node3));

		Assertions.assertEquals(14, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(10, network.getNodes().size(), "wrong number of nodes.");

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet(TransportMode.car, TransportMode.walk));
		Assertions.assertEquals(12, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(9, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[9]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[10]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[11]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[12]).getAllowedModes());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_emptyModes() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(new HashSet<String>());
		// nothing should have changed from the initialization
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_unknownMode() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(Collections.singleton(TransportMode.pt));
		// nothing should have changed from the initialization
		Assertions.assertEquals(8, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(6, network.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, network.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesW, network.getLinks().get(f.linkIds[8]).getAllowedModes());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_singleLinkNetwork() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		NetworkFactory factory = network.getFactory();

		Id<Node> id1 = Id.create(1, Node.class);
		Id<Node> id2 = Id.create(2, Node.class);
		Id<Link> linkId1 = Id.create(1, Link.class);

		Node node1 = factory.createNode(id1, new Coord(0, 100));
		Node node2 = factory.createNode(id2, new Coord(100, 100));
		network.addNode(node1);
		network.addNode(node2);
		network.addLink(factory.createLink(linkId1, node1, node2));
		network.getLinks().get(linkId1).setAllowedModes(Collections.singleton(TransportMode.car));

		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.car));
		/* a single link is no complete network, as the link's
		 * from-node cannot be reached by the link's to-node
		 * */
		Assertions.assertEquals(0, network.getLinks().size(), "wrong number of links.");
		Assertions.assertEquals(0, network.getNodes().size(), "wrong number of nodes.");
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_singleModeWithConnectivity() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		Assertions.assertEquals(6, network.getNodes().size());
		Assertions.assertEquals(8, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assertions.assertEquals(6, network.getNodes().size());
		Assertions.assertEquals(8, network.getLinks().size());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_withConnectivity_connectedSource() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node node4 = network.getNodes().get(f.nodeIds[4]);
		Node node5 = network.getNodes().get(f.nodeIds[5]);
		Node node7 = nf.createNode(f.nodeIds[7], new Coord(600, 100));
		network.addNode(node7);
		network.addLink(nf.createLink(f.linkIds[10], node4, node7));
		network.addLink(nf.createLink(f.linkIds[11], node7, node5));
		network.getLinks().get(f.linkIds[10]).setAllowedModes(f.modesT);
		network.getLinks().get(f.linkIds[11]).setAllowedModes(f.modesW);
		Assertions.assertEquals(7, network.getNodes().size());
		Assertions.assertEquals(10, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assertions.assertEquals(7, network.getNodes().size());
		Assertions.assertEquals(10, network.getLinks().size());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_withConnectivity_connectedSink() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node node4 = network.getNodes().get(f.nodeIds[4]);
		Node node5 = network.getNodes().get(f.nodeIds[5]);
		Node node7 = nf.createNode(f.nodeIds[7], new Coord(600, 100));
		network.addNode(node7);
		network.addLink(nf.createLink(f.linkIds[10], node4, node7));
		network.addLink(nf.createLink(f.linkIds[11], node7, node5));
		network.getLinks().get(f.linkIds[10]).setAllowedModes(f.modesW);
		network.getLinks().get(f.linkIds[11]).setAllowedModes(f.modesT);
		Assertions.assertEquals(7, network.getNodes().size());
		Assertions.assertEquals(10, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assertions.assertEquals(7, network.getNodes().size());
		Assertions.assertEquals(10, network.getLinks().size());
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_withConnectivity_unconnectedSource() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node node4 = network.getNodes().get(f.nodeIds[4]);
		Node node5 = network.getNodes().get(f.nodeIds[5]);
		Node node7 = nf.createNode(f.nodeIds[7], new Coord(600, 100));
		network.addNode(node7);
		network.addLink(nf.createLink(f.linkIds[10], node4, node7));
		network.addLink(nf.createLink(f.linkIds[11], node7, node5));
		network.getLinks().get(f.linkIds[10]).setAllowedModes(Collections.singleton("bike"));
		network.getLinks().get(f.linkIds[11]).setAllowedModes(f.modesW);
		Assertions.assertEquals(7, network.getNodes().size());
		Assertions.assertEquals(10, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assertions.assertEquals(7, network.getNodes().size());
		Assertions.assertEquals(9, network.getLinks().size());
		Assertions.assertNull(network.getLinks().get(f.linkIds[11]));
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_withConnectivity_unconnectedSink() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node node4 = network.getNodes().get(f.nodeIds[4]);
		Node node5 = network.getNodes().get(f.nodeIds[5]);
		Node node7 = nf.createNode(f.nodeIds[7], new Coord(600, 100));
		network.addNode(node7);
		network.addLink(nf.createLink(f.linkIds[10], node4, node7));
		network.addLink(nf.createLink(f.linkIds[11], node7, node5));
		network.getLinks().get(f.linkIds[10]).setAllowedModes(f.modesW);
		network.getLinks().get(f.linkIds[11]).setAllowedModes(Collections.singleton("bike"));
		Assertions.assertEquals(7, network.getNodes().size());
		Assertions.assertEquals(10, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assertions.assertEquals(7, network.getNodes().size());
		Assertions.assertEquals(9, network.getLinks().size());
		Assertions.assertNull(network.getLinks().get(f.linkIds[10]));
	}

	@Test
	@SuppressWarnings("deprecation")
	void testRun_withConnectivity_unconnectedLink() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node node7 = nf.createNode(f.nodeIds[7], new Coord(600, 100));
		Node node8 = nf.createNode(f.nodeIds[8], new Coord(600, 0));
		network.addNode(node7);
		network.addNode(node8);
		network.addLink(nf.createLink(f.linkIds[10], node7, node8));
		network.addLink(nf.createLink(f.linkIds[11], node8, node7));
		network.getLinks().get(f.linkIds[10]).setAllowedModes(f.modesW);
		network.getLinks().get(f.linkIds[11]).setAllowedModes(f.modesW);
		Node node9 = nf.createNode(f.nodeIds[9], new Coord(700, 100));
		Node node10 = nf.createNode(f.nodeIds[10], new Coord(700, 0));
		network.addNode(node9);
		network.addNode(node10);
		network.addLink(nf.createLink(f.linkIds[12], node9, node10));
		network.addLink(nf.createLink(f.linkIds[13], node10, node9));
		network.getLinks().get(f.linkIds[12]).setAllowedModes(f.modesW);
		network.getLinks().get(f.linkIds[13]).setAllowedModes(f.modesT);
		Assertions.assertEquals(10, network.getNodes().size());
		Assertions.assertEquals(12, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assertions.assertEquals(8, network.getNodes().size());
		Assertions.assertEquals(9, network.getLinks().size());
		Assertions.assertNull(network.getLinks().get(f.linkIds[10]));
		Assertions.assertNull(network.getLinks().get(f.linkIds[11]));
		Assertions.assertNull(network.getLinks().get(f.linkIds[12]));
		Assertions.assertNotNull(network.getLinks().get(f.linkIds[13]));
	}

	/**
	 * Creates a simple, multi-modal network as the basis for tests.
	 * <pre>
	 *
	 *   (1)------1------->(2)-------2------>(3)
	 *    ^       c        | ^       w        |
	 *    |                | |                |
	 *    |               c4 |                |
	 *    3c               | |                6w
	 *    |                | 5w               |
	 *    |                | |                |
	 *    |                v |                v
	 *   (4)<------7-------(5)<-------8------(6)
	 *             c                  w
	 *
	 * Legend: c = car, w = walk
	 *
	 * </pre>
	 *
	 * @author mrieser
	 */
	private static class Fixture {

		/*package*/ final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		/*package*/ final Id<Node>[] nodeIds = new Id[21];
		/*package*/ final Id<Link>[] linkIds = new Id[21];
		/*package*/ final Set<String> modesC = createHashSet(TransportMode.car);
		/*package*/ final Set<String> modesW = createHashSet(TransportMode.walk);
		/*package*/ final Set<String> modesCW = createHashSet(TransportMode.car, TransportMode.walk);

		/*package*/ Fixture() {
			for (int i = 0; i < this.nodeIds.length; i++) {
				this.nodeIds[i] = Id.create(i, Node.class);
			}
			for (int i = 0; i < this.linkIds.length; i++) {
				this.linkIds[i] = Id.create(i, Link.class);
			}

			Network network = this.scenario.getNetwork();
			NetworkFactory factory = network.getFactory();
			Node node1 = factory.createNode(this.nodeIds[1], new Coord(0, 100));
			Node node2 = factory.createNode(this.nodeIds[2], new Coord(100, 100));
			Node node3 = factory.createNode(this.nodeIds[3], new Coord(200, 100));
			Node node4 = factory.createNode(this.nodeIds[4], new Coord(0, 100));
			Node node5 = factory.createNode(this.nodeIds[5], new Coord(100, 0));
			Node node6 = factory.createNode(this.nodeIds[6], new Coord(200, 0));
			network.addNode(node1);
			network.addNode(node2);
			network.addNode(node3);
			network.addNode(node4);
			network.addNode(node5);
			network.addNode(node6);
			network.addLink(factory.createLink(this.linkIds[1], node1, node2));
			network.addLink(factory.createLink(this.linkIds[2], node2, node3));
			network.addLink(factory.createLink(this.linkIds[3], node4, node1));
			network.addLink(factory.createLink(this.linkIds[4], node2, node5));
			network.addLink(factory.createLink(this.linkIds[5], node5, node2));
			network.addLink(factory.createLink(this.linkIds[6], node3, node6));
			network.addLink(factory.createLink(this.linkIds[7], node5, node4));
			network.addLink(factory.createLink(this.linkIds[8], node6, node5));
			network.getLinks().get(this.linkIds[1]).setAllowedModes(this.modesC);
			network.getLinks().get(this.linkIds[2]).setAllowedModes(this.modesW);
			network.getLinks().get(this.linkIds[3]).setAllowedModes(this.modesC);
			network.getLinks().get(this.linkIds[4]).setAllowedModes(this.modesC);
			network.getLinks().get(this.linkIds[5]).setAllowedModes(this.modesW);
			network.getLinks().get(this.linkIds[6]).setAllowedModes(this.modesW);
			network.getLinks().get(this.linkIds[7]).setAllowedModes(this.modesC);
			network.getLinks().get(this.linkIds[8]).setAllowedModes(this.modesW);
		}
	}

	/**
	 * Extends Fixture to create the following network for multiple modes tests:
 	 * <pre>
	 *
	 *   (1)------1------->(2)-------2------>(3)
	 *    ^       c        | ^       w        |
	 *    |                | |                |
	 *    |               c4 |                |
	 *    3c               | |                6w
	 *    |                | 5w               |
	 *    |                | |                |
	 *    |                v |                v
	 *   (4)<------7-------(5)<-------8------(6)
	 *    ^        c                  w       |
	 *    |                                   |
	 *    |                                   |
	 *    9c                                 10w
	 *    |                                   |
	 *    |                                   |
	 *    |                                   v
	 *   (7)<-----11-------(8)<------12------(9)
	 *             c                  w
	 *
	 * Legend: c = car, w = walk
	 *
	 * </pre>
	 *
	 * @author mrieser
	 */
	private static class MultimodeFixture extends Fixture {
		/*package*/ MultimodeFixture() {
			super();
			Network network = this.scenario.getNetwork();
			NetworkFactory factory = network.getFactory();
			Node node4 = network.getNodes().get(this.nodeIds[4]);
			Node node6 = network.getNodes().get(this.nodeIds[6]);
			double y2 = -100;
			Node node7 = factory.createNode(this.nodeIds[7], new Coord(0, y2));
			double y1 = -100;
			Node node8 = factory.createNode(this.nodeIds[8], new Coord(100, y1));
			double y = -100;
			Node node9 = factory.createNode(this.nodeIds[9], new Coord(200, y));
			network.addNode(node7);
			network.addNode(node8);
			network.addNode(node9);

			network.addLink(factory.createLink(this.linkIds[ 9], node7, node4));
			network.addLink(factory.createLink(this.linkIds[10], node6, node9));
			network.addLink(factory.createLink(this.linkIds[11], node8, node7));
			network.addLink(factory.createLink(this.linkIds[12], node9, node8));
			network.getLinks().get(this.linkIds[ 9]).setAllowedModes(this.modesC);
			network.getLinks().get(this.linkIds[10]).setAllowedModes(this.modesW);
			network.getLinks().get(this.linkIds[11]).setAllowedModes(this.modesC);
			network.getLinks().get(this.linkIds[12]).setAllowedModes(this.modesW);
		}
	}

	/**
	 * Creates a simple test network with links with different modes
	 * connected by links with other modes.
	 * <pre>
	 *
	 *  (1)------1------>(2)-----------7----------->(4)
	 *   ^      wt      /              t           ^ |
	 *   |            /                           /  |
	 *   |          /                           /    |
	 *   |        /                           /      |
	 *   3wt    2w                          6w       4wt
	 *   |    /                           /          |
	 *   |  /                           /            |
	 *   | /                          /              |
	 *   |v            t             /       wt      v
	 *  (3)<-----------8-----------(6)<------5------(5)
	 *
	 *
	 * Legend: w = walk, t = transit
	 * </pre>
	 *
	 * @author mrieser
	 */
	private static class MultimodalFixture2 {
		/*package*/ final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		/*package*/ final Id<Node>[] nodeIds = new Id[21];
		/*package*/ final Id<Link>[] linkIds = new Id[21];
		/*package*/ final Set<String> modesT = createHashSet(TransportMode.pt);
		/*package*/ final Set<String> modesW = createHashSet(TransportMode.walk);
		/*package*/ final Set<String> modesWT = createHashSet(TransportMode.pt, TransportMode.walk);

		/*package*/ MultimodalFixture2() {
			for (int i = 0; i < this.nodeIds.length; i++) {
				this.nodeIds[i] = Id.create(i, Node.class);
			}
			for (int i = 0; i < this.linkIds.length; i++) {
				this.linkIds[i] = Id.create(i, Link.class);
			}

			Network network = this.scenario.getNetwork();
			NetworkFactory factory = network.getFactory();
			Node node1 = factory.createNode(this.nodeIds[1], new Coord(0, 100));
			Node node2 = factory.createNode(this.nodeIds[2], new Coord(100, 100));
			Node node3 = factory.createNode(this.nodeIds[3], new Coord(0, 0));
			Node node4 = factory.createNode(this.nodeIds[4], new Coord(400, 100));
			Node node5 = factory.createNode(this.nodeIds[5], new Coord(400, 0));
			Node node6 = factory.createNode(this.nodeIds[6], new Coord(300, 0));
			network.addNode(node1);
			network.addNode(node2);
			network.addNode(node3);
			network.addNode(node4);
			network.addNode(node5);
			network.addNode(node6);
			network.addLink(factory.createLink(this.linkIds[1], node1, node2));
			network.addLink(factory.createLink(this.linkIds[2], node2, node3));
			network.addLink(factory.createLink(this.linkIds[3], node3, node1));
			network.addLink(factory.createLink(this.linkIds[4], node4, node5));
			network.addLink(factory.createLink(this.linkIds[5], node5, node6));
			network.addLink(factory.createLink(this.linkIds[6], node6, node4));
			network.addLink(factory.createLink(this.linkIds[7], node2, node4));
			network.addLink(factory.createLink(this.linkIds[8], node6, node3));
			network.getLinks().get(this.linkIds[1]).setAllowedModes(this.modesWT);
			network.getLinks().get(this.linkIds[2]).setAllowedModes(this.modesW);
			network.getLinks().get(this.linkIds[3]).setAllowedModes(this.modesWT);
			network.getLinks().get(this.linkIds[4]).setAllowedModes(this.modesWT);
			network.getLinks().get(this.linkIds[5]).setAllowedModes(this.modesWT);
			network.getLinks().get(this.linkIds[6]).setAllowedModes(this.modesW);
			network.getLinks().get(this.linkIds[7]).setAllowedModes(this.modesT);
			network.getLinks().get(this.linkIds[8]).setAllowedModes(this.modesT);
		}
	}

	private static Set<String> createHashSet(String... mode) {
		HashSet<String> set = new HashSet<String>();
        Collections.addAll(set, mode);
		return set;
	}
}
