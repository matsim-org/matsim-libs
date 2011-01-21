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

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;

/**
 * @author mrieser
 */
public class MultimodalNetworkCleanerTest {

	@Test
	public void testRun_singleMode() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
	}

	@Test
	public void testRun_singleMode_separateLink() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		Node node1 = network.getNodes().get(f.ids[1]);
		Node node3 = network.getNodes().get(f.ids[3]);
		Node node4 = network.getNodes().get(f.ids[4]);
		Node node6 = network.getNodes().get(f.ids[6]);
		network.addLink(network.getFactory().createLink(f.ids[10], node1, node4));
		network.addLink(network.getFactory().createLink(f.ids[11], node6, node3));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesW);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesC);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 9, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[11]));

		cleaner.run(createHashSet(TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[10]));
		Assert.assertNull(network.getLinks().get(f.ids[11]));
	}

	@Test
	public void testRun_singleInexistantMode() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet("other"));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
	}

	@Test
	public void testRun_singleMode_singleSink() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		Node node1 = network.getNodes().get(f.ids[1]);
		Node node3 = network.getNodes().get(f.ids[3]);
		Node node10 = network.getFactory().createNode(f.ids[10], f.scenario.createCoord(0, 200));
		Node node11 = network.getFactory().createNode(f.ids[11], f.scenario.createCoord(200, 200));
		network.addNode(node10);
		network.addNode(node11);
		network.addLink(network.getFactory().createLink(f.ids[10], node1, node10));
		network.addLink(network.getFactory().createLink(f.ids[11], node3, node11));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assert.assertEquals("wrong number of links.", 10, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 8, network.getNodes().size());

		cleaner.run(createHashSet(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 9, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 7, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[10]));

		Assert.assertEquals("wrong number of links.", 9, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 7, network.getNodes().size());

		cleaner.run(createHashSet(TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[11]));

		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
	}

	@Test
	public void testRun_singleMode_singleSinkIntegrated() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		network.getLinks().get(f.ids[1]).setAllowedModes(f.modesCW); // integrate the sinks into the existing network
		network.getLinks().get(f.ids[8]).setAllowedModes(f.modesCW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());

		cleaner.run(createHashSet(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesCW, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes()); // only remove mode, not link!

		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());

		cleaner.run(createHashSet(TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes()); // only remove mode, not link!
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());

		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
	}

	@Test
	public void testRun_singleMode_doubleSink() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		Node node1 = network.getNodes().get(f.ids[1]);
		Node node2 = network.getNodes().get(f.ids[2]);
		Node node3 = network.getNodes().get(f.ids[3]);
		Node node10 = network.getFactory().createNode(f.ids[10], f.scenario.createCoord(0, 200));
		Node node11 = network.getFactory().createNode(f.ids[11], f.scenario.createCoord(200, 200));
		network.addNode(node10);
		network.addNode(node11);
		network.addLink(network.getFactory().createLink(f.ids[10], node1, node10));
		network.addLink(network.getFactory().createLink(f.ids[11], node2, node10));
		network.addLink(network.getFactory().createLink(f.ids[12], node2, node11));
		network.addLink(network.getFactory().createLink(f.ids[13], node3, node11));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[12]).setAllowedModes(f.modesW);
		network.getLinks().get(f.ids[13]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assert.assertEquals("wrong number of links.", 12, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 8, network.getNodes().size());

		cleaner.run(createHashSet(TransportMode.walk));

		Assert.assertEquals("wrong number of links.", 10, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 7, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[12]));
		Assert.assertNull(network.getLinks().get(f.ids[13]));

		cleaner.run(createHashSet(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[10]));
		Assert.assertNull(network.getLinks().get(f.ids[11]));
	}


	@Test
	public void testRun_singleMode_singleSource() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		Node node1 = network.getNodes().get(f.ids[1]);
		Node node3 = network.getNodes().get(f.ids[3]);
		Node node10 = network.getFactory().createNode(f.ids[10], f.scenario.createCoord(0, 200));
		Node node11 = network.getFactory().createNode(f.ids[11], f.scenario.createCoord(200, 200));
		network.addNode(node10);
		network.addNode(node11);
		network.addLink(network.getFactory().createLink(f.ids[10], node10, node1));
		network.addLink(network.getFactory().createLink(f.ids[11], node11, node3));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assert.assertEquals("wrong number of links.", 10, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 8, network.getNodes().size());

		cleaner.run(createHashSet(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 9, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 7, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[10]));

		Assert.assertEquals("wrong number of links.", 9, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 7, network.getNodes().size());

		cleaner.run(createHashSet(TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[11]));

		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
	}

	@Test
	public void testRemoveNodesWithoutLinks() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		network.addNode(network.getFactory().createNode(f.ids[10], f.scenario.createCoord(300, 300)));
		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 7, network.getNodes().size());

		cleaner.run(createHashSet(TransportMode.walk));

		Assert.assertEquals("wrong number of links after cleaning.", 8, network.getLinks().size());
		Assert.assertEquals("empty node should not be removed by cleaning.", 7, network.getNodes().size());

		cleaner.removeNodesWithoutLinks();

		Assert.assertEquals("wrong number of links after cleaning.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes after cleaning.", 6, network.getNodes().size());
	}

	@Test
	public void testRun_singleMode_doubleSource() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();
		Node node1 = network.getNodes().get(f.ids[1]);
		Node node2 = network.getNodes().get(f.ids[2]);
		Node node3 = network.getNodes().get(f.ids[3]);
		Node node10 = network.getFactory().createNode(f.ids[10], f.scenario.createCoord(0, 200));
		Node node11 = network.getFactory().createNode(f.ids[11], f.scenario.createCoord(200, 200));
		network.addNode(node10);
		network.addNode(node11);
		network.addLink(network.getFactory().createLink(f.ids[10], node10, node1));
		network.addLink(network.getFactory().createLink(f.ids[11], node10, node2));
		network.addLink(network.getFactory().createLink(f.ids[12], node11, node2));
		network.addLink(network.getFactory().createLink(f.ids[13], node11, node3));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesC);
		network.getLinks().get(f.ids[12]).setAllowedModes(f.modesW);
		network.getLinks().get(f.ids[13]).setAllowedModes(f.modesW);

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);
		Assert.assertEquals("wrong number of links.", 12, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 8, network.getNodes().size());

		cleaner.run(createHashSet(TransportMode.walk));

		Assert.assertEquals("wrong number of links.", 10, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 7, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[12]));
		Assert.assertNull(network.getLinks().get(f.ids[13]));

		cleaner.run(createHashSet(TransportMode.car));
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertNull(network.getLinks().get(f.ids[10]));
		Assert.assertNull(network.getLinks().get(f.ids[11]));
	}

	@Test
	public void testRun_multipleModes() {
		Fixture f = new MultimodeFixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet(TransportMode.car, TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 12, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 9, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[9]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[12]).getAllowedModes());
	}

	@Test
	public void testRun_multipleModes_doubleSink() {
		Fixture f = new MultimodeFixture();
		Network network = f.scenario.getNetwork();

		Node node2 = network.getNodes().get(f.ids[2]);
		Node node3 = network.getNodes().get(f.ids[3]);
		Node node10 = network.getFactory().createNode(f.ids[10], f.scenario.createCoord(200, 200));
		network.addNode(node10);
		network.addLink(network.getFactory().createLink(f.ids[18], node2, node10));
		network.addLink(network.getFactory().createLink(f.ids[19], node3, node10));

		Assert.assertEquals("wrong number of links.", 14, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 10, network.getNodes().size());

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet(TransportMode.car, TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 12, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 9, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[9]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[12]).getAllowedModes());
	}

	@Test
	public void testRun_multipleModes_doubleSource() {
		Fixture f = new MultimodeFixture();
		Network network = f.scenario.getNetwork();

		Node node2 = network.getNodes().get(f.ids[2]);
		Node node3 = network.getNodes().get(f.ids[3]);
		Node node10 = network.getFactory().createNode(f.ids[10], f.scenario.createCoord(200, 200));
		network.addNode(node10);
		network.addLink(network.getFactory().createLink(f.ids[18], node10, node2));
		network.addLink(network.getFactory().createLink(f.ids[19], node10, node3));

		Assert.assertEquals("wrong number of links.", 14, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 10, network.getNodes().size());

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(createHashSet(TransportMode.car, TransportMode.walk));
		Assert.assertEquals("wrong number of links.", 12, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 9, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[9]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[10]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[11]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[12]).getAllowedModes());
	}

	@Test
	public void testRun_emptyModes() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(new HashSet<String>());
		// nothing should have changed from the initialization
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
	}

	@Test
	public void testRun_unknownMode() {
		Fixture f = new Fixture();
		Network network = f.scenario.getNetwork();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(network);

		cleaner.run(Collections.singleton(TransportMode.pt));
		// nothing should have changed from the initialization
		Assert.assertEquals("wrong number of links.", 8, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 6, network.getNodes().size());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[1]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[2]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[3]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[4]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[5]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[6]).getAllowedModes());
		Assert.assertEquals(f.modesC, network.getLinks().get(f.ids[7]).getAllowedModes());
		Assert.assertEquals(f.modesW, network.getLinks().get(f.ids[8]).getAllowedModes());
	}

	@Test
	public void testRun_singleLinkNetwork() {
		Scenario scenario = new ScenarioImpl();
		Network network = scenario.getNetwork();
		NetworkFactory factory = network.getFactory();

		Id id1 = scenario.createId("1");
		Id id2 = scenario.createId("2");

		Node node1 = factory.createNode(id1, scenario.createCoord(  0, 100));
		Node node2 = factory.createNode(id2, scenario.createCoord(100, 100));
		network.addNode(node1);
		network.addNode(node2);
		network.addLink(factory.createLink(id1, node1, node2));
		network.getLinks().get(id1).setAllowedModes(Collections.singleton(TransportMode.car));

		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.car));
		/* a single link is no complete network, as the link's
		 * from-node cannot be reached by the link's to-node
		 * */
		Assert.assertEquals("wrong number of links.", 0, network.getLinks().size());
		Assert.assertEquals("wrong number of nodes.", 0, network.getNodes().size());
	}

	@Test
	public void testRun_singleModeWithConnectivity() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		Assert.assertEquals(6, network.getNodes().size());
		Assert.assertEquals(8, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assert.assertEquals(6, network.getNodes().size());
		Assert.assertEquals(8, network.getLinks().size());
	}

	@Test
	public void testRun_withConnectivity_connectedSource() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node node4 = network.getNodes().get(f.ids[4]);
		Node node5 = network.getNodes().get(f.ids[5]);
		Node node7 = nf.createNode(f.ids[7], f.scenario.createCoord(600, 100));
		network.addNode(node7);
		network.addLink(nf.createLink(f.ids[10], node4, node7));
		network.addLink(nf.createLink(f.ids[11], node7, node5));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesT);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesW);
		Assert.assertEquals(7, network.getNodes().size());
		Assert.assertEquals(10, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assert.assertEquals(7, network.getNodes().size());
		Assert.assertEquals(10, network.getLinks().size());
	}

	@Test
	public void testRun_withConnectivity_connectedSink() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node node4 = network.getNodes().get(f.ids[4]);
		Node node5 = network.getNodes().get(f.ids[5]);
		Node node7 = nf.createNode(f.ids[7], f.scenario.createCoord(600, 100));
		network.addNode(node7);
		network.addLink(nf.createLink(f.ids[10], node4, node7));
		network.addLink(nf.createLink(f.ids[11], node7, node5));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesW);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesT);
		Assert.assertEquals(7, network.getNodes().size());
		Assert.assertEquals(10, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assert.assertEquals(7, network.getNodes().size());
		Assert.assertEquals(10, network.getLinks().size());
	}

	@Test
	public void testRun_withConnectivity_unconnectedSource() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node node4 = network.getNodes().get(f.ids[4]);
		Node node5 = network.getNodes().get(f.ids[5]);
		Node node7 = nf.createNode(f.ids[7], f.scenario.createCoord(600, 100));
		network.addNode(node7);
		network.addLink(nf.createLink(f.ids[10], node4, node7));
		network.addLink(nf.createLink(f.ids[11], node7, node5));
		network.getLinks().get(f.ids[10]).setAllowedModes(Collections.singleton("bike"));
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesW);
		Assert.assertEquals(7, network.getNodes().size());
		Assert.assertEquals(10, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assert.assertEquals(7, network.getNodes().size());
		Assert.assertEquals(9, network.getLinks().size());
		Assert.assertNull(network.getLinks().get(f.ids[11]));
	}

	@Test
	public void testRun_withConnectivity_unconnectedSink() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node node4 = network.getNodes().get(f.ids[4]);
		Node node5 = network.getNodes().get(f.ids[5]);
		Node node7 = nf.createNode(f.ids[7], f.scenario.createCoord(600, 100));
		network.addNode(node7);
		network.addLink(nf.createLink(f.ids[10], node4, node7));
		network.addLink(nf.createLink(f.ids[11], node7, node5));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesW);
		network.getLinks().get(f.ids[11]).setAllowedModes(Collections.singleton("bike"));
		Assert.assertEquals(7, network.getNodes().size());
		Assert.assertEquals(10, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assert.assertEquals(7, network.getNodes().size());
		Assert.assertEquals(9, network.getLinks().size());
		Assert.assertNull(network.getLinks().get(f.ids[10]));
	}

	@Test
	public void testRun_withConnectivity_unconnectedLink() {
		MultimodalFixture2 f = new MultimodalFixture2();
		Network network = f.scenario.getNetwork();
		NetworkFactory nf = network.getFactory();
		Node node7 = nf.createNode(f.ids[7], f.scenario.createCoord(600, 100));
		Node node8 = nf.createNode(f.ids[8], f.scenario.createCoord(600, 000));
		network.addNode(node7);
		network.addNode(node8);
		network.addLink(nf.createLink(f.ids[10], node7, node8));
		network.addLink(nf.createLink(f.ids[11], node8, node7));
		network.getLinks().get(f.ids[10]).setAllowedModes(f.modesW);
		network.getLinks().get(f.ids[11]).setAllowedModes(f.modesW);
		Node node9 = nf.createNode(f.ids[9], f.scenario.createCoord(700, 100));
		Node node10 = nf.createNode(f.ids[10], f.scenario.createCoord(700, 000));
		network.addNode(node9);
		network.addNode(node10);
		network.addLink(nf.createLink(f.ids[12], node9, node10));
		network.addLink(nf.createLink(f.ids[13], node10, node9));
		network.getLinks().get(f.ids[12]).setAllowedModes(f.modesW);
		network.getLinks().get(f.ids[13]).setAllowedModes(f.modesT);
		Assert.assertEquals(10, network.getNodes().size());
		Assert.assertEquals(12, network.getLinks().size());
		new MultimodalNetworkCleaner(network).run(Collections.singleton(TransportMode.walk), Collections.singleton(TransportMode.pt));
		Assert.assertEquals(8, network.getNodes().size());
		Assert.assertEquals(9, network.getLinks().size());
		Assert.assertNull(network.getLinks().get(f.ids[10]));
		Assert.assertNull(network.getLinks().get(f.ids[11]));
		Assert.assertNull(network.getLinks().get(f.ids[12]));
		Assert.assertNotNull(network.getLinks().get(f.ids[13]));
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

		/*package*/ final Scenario scenario = new ScenarioImpl();
		/*package*/ final Id[] ids = new Id[21];
		/*package*/ final Set<String> modesC = createHashSet(TransportMode.car);
		/*package*/ final Set<String> modesW = createHashSet(TransportMode.walk);
		/*package*/ final Set<String> modesCW = createHashSet(TransportMode.car, TransportMode.walk);

		/*package*/ Fixture() {
			for (int i = 0; i < this.ids.length; i++) {
				this.ids[i] = this.scenario.createId(Integer.toString(i));
			}

			Network network = this.scenario.getNetwork();
			NetworkFactory factory = network.getFactory();
			Node node1 = factory.createNode(this.ids[1], this.scenario.createCoord(  0, 100));
			Node node2 = factory.createNode(this.ids[2], this.scenario.createCoord(100, 100));
			Node node3 = factory.createNode(this.ids[3], this.scenario.createCoord(200, 100));
			Node node4 = factory.createNode(this.ids[4], this.scenario.createCoord(  0, 100));
			Node node5 = factory.createNode(this.ids[5], this.scenario.createCoord(100,   0));
			Node node6 = factory.createNode(this.ids[6], this.scenario.createCoord(200,   0));
			network.addNode(node1);
			network.addNode(node2);
			network.addNode(node3);
			network.addNode(node4);
			network.addNode(node5);
			network.addNode(node6);
			network.addLink(factory.createLink(this.ids[1], node1, node2));
			network.addLink(factory.createLink(this.ids[2], node2, node3));
			network.addLink(factory.createLink(this.ids[3], node4, node1));
			network.addLink(factory.createLink(this.ids[4], node2, node5));
			network.addLink(factory.createLink(this.ids[5], node5, node2));
			network.addLink(factory.createLink(this.ids[6], node3, node6));
			network.addLink(factory.createLink(this.ids[7], node5, node4));
			network.addLink(factory.createLink(this.ids[8], node6, node5));
			network.getLinks().get(this.ids[1]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[2]).setAllowedModes(this.modesW);
			network.getLinks().get(this.ids[3]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[4]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[5]).setAllowedModes(this.modesW);
			network.getLinks().get(this.ids[6]).setAllowedModes(this.modesW);
			network.getLinks().get(this.ids[7]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[8]).setAllowedModes(this.modesW);
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
			Node node4 = network.getNodes().get(this.ids[4]);
			Node node6 = network.getNodes().get(this.ids[6]);
			Node node7 = factory.createNode(this.ids[7], this.scenario.createCoord(  0, -100));
			Node node8 = factory.createNode(this.ids[8], this.scenario.createCoord(100, -100));
			Node node9 = factory.createNode(this.ids[9], this.scenario.createCoord(200, -100));
			network.addNode(node7);
			network.addNode(node8);
			network.addNode(node9);

			network.addLink(factory.createLink(this.ids[ 9], node7, node4));
			network.addLink(factory.createLink(this.ids[10], node6, node9));
			network.addLink(factory.createLink(this.ids[11], node8, node7));
			network.addLink(factory.createLink(this.ids[12], node9, node8));
			network.getLinks().get(this.ids[ 9]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[10]).setAllowedModes(this.modesW);
			network.getLinks().get(this.ids[11]).setAllowedModes(this.modesC);
			network.getLinks().get(this.ids[12]).setAllowedModes(this.modesW);
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
		/*package*/ final Scenario scenario = new ScenarioImpl();
		/*package*/ final Id[] ids = new Id[21];
		/*package*/ final Set<String> modesT = createHashSet(TransportMode.pt);
		/*package*/ final Set<String> modesW = createHashSet(TransportMode.walk);
		/*package*/ final Set<String> modesWT = createHashSet(TransportMode.pt, TransportMode.walk);

		/*package*/ MultimodalFixture2() {
			for (int i = 0; i < this.ids.length; i++) {
				this.ids[i] = this.scenario.createId(Integer.toString(i));
			}

			Network network = this.scenario.getNetwork();
			NetworkFactory factory = network.getFactory();
			Node node1 = factory.createNode(this.ids[1], this.scenario.createCoord(  0, 100));
			Node node2 = factory.createNode(this.ids[2], this.scenario.createCoord(100, 100));
			Node node3 = factory.createNode(this.ids[3], this.scenario.createCoord(  0,   0));
			Node node4 = factory.createNode(this.ids[4], this.scenario.createCoord(400, 100));
			Node node5 = factory.createNode(this.ids[5], this.scenario.createCoord(400,   0));
			Node node6 = factory.createNode(this.ids[6], this.scenario.createCoord(300,   0));
			network.addNode(node1);
			network.addNode(node2);
			network.addNode(node3);
			network.addNode(node4);
			network.addNode(node5);
			network.addNode(node6);
			network.addLink(factory.createLink(this.ids[1], node1, node2));
			network.addLink(factory.createLink(this.ids[2], node2, node3));
			network.addLink(factory.createLink(this.ids[3], node3, node1));
			network.addLink(factory.createLink(this.ids[4], node4, node5));
			network.addLink(factory.createLink(this.ids[5], node5, node6));
			network.addLink(factory.createLink(this.ids[6], node6, node4));
			network.addLink(factory.createLink(this.ids[7], node2, node4));
			network.addLink(factory.createLink(this.ids[8], node6, node3));
			network.getLinks().get(this.ids[1]).setAllowedModes(this.modesWT);
			network.getLinks().get(this.ids[2]).setAllowedModes(this.modesW);
			network.getLinks().get(this.ids[3]).setAllowedModes(this.modesWT);
			network.getLinks().get(this.ids[4]).setAllowedModes(this.modesWT);
			network.getLinks().get(this.ids[5]).setAllowedModes(this.modesWT);
			network.getLinks().get(this.ids[6]).setAllowedModes(this.modesW);
			network.getLinks().get(this.ids[7]).setAllowedModes(this.modesT);
			network.getLinks().get(this.ids[8]).setAllowedModes(this.modesT);
		}
	}

	private static Set<String> createHashSet(String... mode) {
		HashSet<String> set = new HashSet<String>();
		for (String m : mode) {
			set.add(m);
		}
		return set;
	}
}
