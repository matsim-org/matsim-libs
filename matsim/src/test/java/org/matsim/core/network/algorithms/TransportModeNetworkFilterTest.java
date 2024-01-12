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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.TimeDependentNetwork;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser
 */
public class TransportModeNetworkFilterTest {

	@Test
	void testFilter_SingleMode() {
		final Fixture f = new Fixture();
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(f.scenario.getNetwork());

		Network subNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		filter.filter(subNetwork, createHashSet(TransportMode.car));
		Assertions.assertEquals(13, subNetwork.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(14, subNetwork.getLinks().size(), "wrong number of links");
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[1]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[2]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[3]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[4]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[5]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[6]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[7]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[8]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[9]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[10]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[11]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[12]));
		Assertions.assertFalse(subNetwork.getLinks().containsKey(f.linkIds[13]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[14]));
		Assertions.assertFalse(subNetwork.getLinks().containsKey(f.linkIds[15]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[16]));
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[9]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[10]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[11]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[12]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[14]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[16]).getAllowedModes());
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[4]).getInLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[4]).getInLinks().get(f.linkIds[3]));
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[7]).getOutLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[7]).getOutLinks().get(f.linkIds[7]));

		subNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		filter.filter(subNetwork, createHashSet(TransportMode.bike));
		Assertions.assertEquals(9, subNetwork.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(8, subNetwork.getLinks().size(), "wrong number of links");
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[13]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[4]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[5]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[6]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[7]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[8]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[9]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[16]));
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[13]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[9]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[16]).getAllowedModes());
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[4]).getInLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[4]).getInLinks().get(f.linkIds[13]));
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[4]).getOutLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[4]).getOutLinks().get(f.linkIds[4]));

		subNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		filter.filter(subNetwork, createHashSet(TransportMode.walk));
		Assertions.assertEquals(5, subNetwork.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(4, subNetwork.getLinks().size(), "wrong number of links");
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[13]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[14]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[15]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[16]));
		Assertions.assertTrue(subNetwork.getNodes().containsKey(f.nodeIds[1]));
		Assertions.assertTrue(subNetwork.getNodes().containsKey(f.nodeIds[4]));
		Assertions.assertTrue(subNetwork.getNodes().containsKey(f.nodeIds[7]));
		Assertions.assertTrue(subNetwork.getNodes().containsKey(f.nodeIds[10]));
		Assertions.assertTrue(subNetwork.getNodes().containsKey(f.nodeIds[13]));
		Assertions.assertEquals(f.modesW, subNetwork.getLinks().get(f.linkIds[13]).getAllowedModes());
		Assertions.assertEquals(f.modesW, subNetwork.getLinks().get(f.linkIds[14]).getAllowedModes());
		Assertions.assertEquals(f.modesW, subNetwork.getLinks().get(f.linkIds[15]).getAllowedModes());
		Assertions.assertEquals(f.modesW, subNetwork.getLinks().get(f.linkIds[16]).getAllowedModes());
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[4]).getInLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[4]).getInLinks().get(f.linkIds[13]));
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[4]).getOutLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[4]).getOutLinks().get(f.linkIds[14]));
	}

	@Test
	void testFilter_MultipleModes() {
		final Fixture f = new Fixture();
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(f.scenario.getNetwork());

		Network subNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		filter.filter(subNetwork, createHashSet(TransportMode.car, TransportMode.bike));
		Assertions.assertEquals(13, subNetwork.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(15, subNetwork.getLinks().size(), "wrong number of links");
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[1]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[2]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[3]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[4]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[5]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[6]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[7]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[8]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[9]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[10]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[11]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[12]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[13]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[14]));
		Assertions.assertFalse(subNetwork.getLinks().containsKey(f.linkIds[15]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[16]));
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[1]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[2]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[3]).getAllowedModes());
		Assertions.assertEquals(f.modesCB, subNetwork.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesCB, subNetwork.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesCB, subNetwork.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesCB, subNetwork.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesCB, subNetwork.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesCB, subNetwork.getLinks().get(f.linkIds[9]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[10]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[11]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[12]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[13]).getAllowedModes());
		Assertions.assertEquals(f.modesC, subNetwork.getLinks().get(f.linkIds[14]).getAllowedModes());
		Assertions.assertEquals(f.modesCB, subNetwork.getLinks().get(f.linkIds[16]).getAllowedModes());
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[7]).getOutLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[7]).getOutLinks().get(f.linkIds[7]));
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[10]).getInLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[10]).getInLinks().get(f.linkIds[9]));

		subNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		filter.filter(subNetwork, createHashSet(TransportMode.bike, TransportMode.walk));
		Assertions.assertEquals(9, subNetwork.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(10, subNetwork.getLinks().size(), "wrong number of links");
		Assertions.assertFalse(subNetwork.getLinks().containsKey(f.linkIds[1]));
		Assertions.assertFalse(subNetwork.getLinks().containsKey(f.linkIds[2]));
		Assertions.assertFalse(subNetwork.getLinks().containsKey(f.linkIds[3]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[4]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[5]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[6]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[7]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[8]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[9]));
		Assertions.assertFalse(subNetwork.getLinks().containsKey(f.linkIds[10]));
		Assertions.assertFalse(subNetwork.getLinks().containsKey(f.linkIds[11]));
		Assertions.assertFalse(subNetwork.getLinks().containsKey(f.linkIds[12]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[13]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[14]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[15]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[16]));
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[4]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[5]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[6]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[7]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[8]).getAllowedModes());
		Assertions.assertEquals(f.modesB, subNetwork.getLinks().get(f.linkIds[9]).getAllowedModes());
		Assertions.assertEquals(f.modesWB, subNetwork.getLinks().get(f.linkIds[13]).getAllowedModes());
		Assertions.assertEquals(f.modesW, subNetwork.getLinks().get(f.linkIds[14]).getAllowedModes());
		Assertions.assertEquals(f.modesW, subNetwork.getLinks().get(f.linkIds[15]).getAllowedModes());
		Assertions.assertEquals(f.modesWB, subNetwork.getLinks().get(f.linkIds[16]).getAllowedModes());
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[4]).getInLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[4]).getInLinks().get(f.linkIds[13]));
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[10]).getOutLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[10]).getOutLinks().get(f.linkIds[16]));
	}

	@Test
	void testFilter_NoModes() {
		final Fixture f = new Fixture();
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(f.scenario.getNetwork());

		Network subNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		filter.filter(subNetwork, new HashSet<String>());
		Assertions.assertEquals(0, subNetwork.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(0, subNetwork.getLinks().size(), "wrong number of links");
	}

	@Test
	void testFilter_AdditionalModes() {
		final Fixture f = new Fixture();
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(f.scenario.getNetwork());

		Network subNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		filter.filter(subNetwork, createHashSet(TransportMode.walk, TransportMode.pt, "motorbike"));
		Assertions.assertEquals(5, subNetwork.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(4, subNetwork.getLinks().size(), "wrong number of links");
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[13]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[14]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[15]));
		Assertions.assertTrue(subNetwork.getLinks().containsKey(f.linkIds[16]));
		Assertions.assertTrue(subNetwork.getNodes().containsKey(f.nodeIds[1]));
		Assertions.assertTrue(subNetwork.getNodes().containsKey(f.nodeIds[4]));
		Assertions.assertTrue(subNetwork.getNodes().containsKey(f.nodeIds[7]));
		Assertions.assertTrue(subNetwork.getNodes().containsKey(f.nodeIds[10]));
		Assertions.assertTrue(subNetwork.getNodes().containsKey(f.nodeIds[13]));
		Assertions.assertEquals(f.modesW, subNetwork.getLinks().get(f.linkIds[13]).getAllowedModes());
		Assertions.assertEquals(f.modesW, subNetwork.getLinks().get(f.linkIds[14]).getAllowedModes());
		Assertions.assertEquals(f.modesW, subNetwork.getLinks().get(f.linkIds[15]).getAllowedModes());
		Assertions.assertEquals(f.modesW, subNetwork.getLinks().get(f.linkIds[16]).getAllowedModes());
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[4]).getInLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[4]).getInLinks().get(f.linkIds[13]));
		Assertions.assertEquals(1, subNetwork.getNodes().get(f.nodeIds[4]).getOutLinks().size());
		Assertions.assertNotNull(subNetwork.getNodes().get(f.nodeIds[4]).getOutLinks().get(f.linkIds[14]));
	}

	@Test
	void testFilter_NoCommonModes() {
		final Fixture f = new Fixture();
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(f.scenario.getNetwork());

		Network subNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		filter.filter(subNetwork, createHashSet(TransportMode.pt, "motorbike"));
		Assertions.assertEquals(0, subNetwork.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(0, subNetwork.getLinks().size(), "wrong number of links");
	}

	/**
	 * Tests the algorithm for the case the network contains direct loops, i.e.
	 * links with the same from and to node.
	 *
	 * <code>Issue #178</code> - http://sourceforge.net/apps/trac/matsim/ticket/178
	 *
	 * The problem seems only to happen when the loop link is (accidentally / randomly)
	 * chosen as start link for the algorithm, as otherwise the node already exists.
	 * Thus cannot extend existing Fixture to test this, but have to create test
	 * scenario from scratch.
	 */
	@Test
	void testFilter_SingleMode_loop() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		final NetworkFactory factory = network.getFactory();

		Node node1 = factory.createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		network.addNode(node1);
		Link link1 = factory.createLink(Id.create(1, Link.class), node1, node1);
		link1.setAllowedModes(createHashSet(TransportMode.car));
		network.addLink(link1);

		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);

		Network subNetwork = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		filter.filter(subNetwork, createHashSet(TransportMode.car));
		Assertions.assertEquals(1, subNetwork.getNodes().size(), "wrong number of nodes.");
		Assertions.assertEquals(1, subNetwork.getLinks().size(), "wrong number of links");
		Assertions.assertTrue(subNetwork.getLinks().containsKey(Id.create(1, Link.class)));
	}

	/**
	 * Tests that tiem-varying information is converted
	 */
	@Test
	void testFilter_timeVariant() {
		Config config = ConfigUtils.createConfig();
		config.network().setTimeVariantNetwork(true);

		Network sourceNetwork = NetworkUtils.createNetwork(config);
		Node node1 = sourceNetwork.getFactory().createNode(Id.createNodeId("1"), new Coord(0.0, 0.0));
		Node node2 = sourceNetwork.getFactory().createNode(Id.createNodeId("2"), new Coord(0.0, 0.0));
		sourceNetwork.addNode(node1);
		sourceNetwork.addNode(node2);

		Link sourceLink = sourceNetwork.getFactory().createLink(Id.createLinkId("link"), node1, node2);
		sourceLink.setFreespeed(50.0);
		sourceLink.setAllowedModes(Collections.singleton("car"));
		sourceNetwork.addLink(sourceLink);

		NetworkChangeEvent changeEvent = new NetworkChangeEvent(120.0);
		changeEvent.setFreespeedChange(new ChangeValue(ChangeType.FACTOR, 2.0));
		changeEvent.addLink(sourceLink);
		((TimeDependentNetwork) sourceNetwork).addNetworkChangeEvent(changeEvent);

		Assertions.assertEquals(50.0, sourceLink.getFreespeed(), 1e-3);
		Assertions.assertEquals(50.0, sourceLink.getFreespeed(0.0), 1e-3);
		Assertions.assertEquals(100.0, sourceLink.getFreespeed(120.0), 1e-3);


		Network targetNetwork = NetworkUtils.createNetwork(config);
		new TransportModeNetworkFilter(sourceNetwork).filter(targetNetwork, Collections.singleton("car"));
		Link targetLink = targetNetwork.getLinks().get(sourceLink.getId());

		Assertions.assertEquals(50.0, targetLink.getFreespeed(), 1e-3);
		Assertions.assertEquals(50.0, targetLink.getFreespeed(0.0), 1e-3);
		Assertions.assertEquals(100.0, targetLink.getFreespeed(120.0), 1e-3);
	}

	/**
	 * Creates a simple, multi-modal network for tests.
	 * <pre>
	 *
	 *                    cb                        c
	 *              (5)----5-----(6)         (11)---11----(12)
	 *               |            |            |            |
	 *               |            |            |            |
	 *               4cb          6cb         10c          12c
	 *               |            |            |            |
	 *        wb     |     cw     |     w      |     cwb    |
	 * (1)----13----(4)-----14---(7)----15---(10)----16---(13)
	 *  |            |            |            |
	 *  |            |            |            |
	 *  1c           3c           7cb          9cb
	 *  |            |            |            |
	 *  |            |            |            |
	 * (2)-----2----(3)          (8)----8-----(9)
	 *        c                         cb
	 *
	 * Legend: c = car, w = walk, b = bike
	 *
	 * </pre>
	 *
	 * @author mrieser
	 */
	private static class Fixture {
		/*package*/ final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		/*package*/ final Id<Node>[] nodeIds = new Id[17];
		/*package*/ final Id<Link>[] linkIds = new Id[17];
		/*package*/ final Set<String> modesC = createHashSet(TransportMode.car);
		/*package*/ final Set<String> modesCB = createHashSet(TransportMode.car, TransportMode.bike);
		/*package*/ final Set<String> modesCBW = createHashSet(TransportMode.car, TransportMode.bike, TransportMode.walk);
		/*package*/ final Set<String> modesCW = createHashSet(TransportMode.car, TransportMode.walk);
		/*package*/ final Set<String> modesW = createHashSet(TransportMode.walk);
		/*package*/ final Set<String> modesWB = createHashSet(TransportMode.walk, TransportMode.bike);
		/*package*/ final Set<String> modesB = createHashSet(TransportMode.bike);

		/*package*/ Fixture() {
			for (int i = 0; i < nodeIds.length; i++) {
				this.nodeIds[i] = Id.create(i, Node.class);
			}
			for (int i = 0; i < linkIds.length; i++) {
				this.linkIds[i] = Id.create(i, Link.class);
			}

			final Network network = this.scenario.getNetwork();
			final NetworkFactory factory = network.getFactory();

			network.addNode(factory.createNode(this.nodeIds[ 1], new Coord((double) 0, (double) 100)));
			network.addNode(factory.createNode(this.nodeIds[ 2], new Coord((double) 0, (double) 0)));
			network.addNode(factory.createNode(this.nodeIds[ 3], new Coord((double) 100, (double) 0)));
			network.addNode(factory.createNode(this.nodeIds[ 4], new Coord((double) 100, (double) 100)));
			network.addNode(factory.createNode(this.nodeIds[ 5], new Coord((double) 100, (double) 200)));
			network.addNode(factory.createNode(this.nodeIds[ 6], new Coord((double) 200, (double) 200)));
			network.addNode(factory.createNode(this.nodeIds[ 7], new Coord((double) 200, (double) 100)));
			network.addNode(factory.createNode(this.nodeIds[ 8], new Coord((double) 200, (double) 0)));
			network.addNode(factory.createNode(this.nodeIds[ 9], new Coord((double) 300, (double) 0)));
			network.addNode(factory.createNode(this.nodeIds[10], new Coord((double) 300, (double) 100)));
			network.addNode(factory.createNode(this.nodeIds[11], new Coord((double) 300, (double) 200)));
			network.addNode(factory.createNode(this.nodeIds[12], new Coord((double) 400, (double) 200)));
			network.addNode(factory.createNode(this.nodeIds[13], new Coord((double) 400, (double) 100)));

			network.addLink(createLink(network, this.linkIds[ 1], this.nodeIds[ 1], this.nodeIds[ 2], this.modesC));
			network.addLink(createLink(network, this.linkIds[ 2], this.nodeIds[ 2], this.nodeIds[ 3], this.modesC));
			network.addLink(createLink(network, this.linkIds[ 3], this.nodeIds[ 3], this.nodeIds[ 4], this.modesC));
			network.addLink(createLink(network, this.linkIds[ 4], this.nodeIds[ 4], this.nodeIds[ 5], this.modesCB));
			network.addLink(createLink(network, this.linkIds[ 5], this.nodeIds[ 5], this.nodeIds[ 6], this.modesCB));
			network.addLink(createLink(network, this.linkIds[ 6], this.nodeIds[ 6], this.nodeIds[ 7], this.modesCB));
			network.addLink(createLink(network, this.linkIds[ 7], this.nodeIds[ 7], this.nodeIds[ 8], this.modesCB));
			network.addLink(createLink(network, this.linkIds[ 8], this.nodeIds[ 8], this.nodeIds[ 9], this.modesCB));
			network.addLink(createLink(network, this.linkIds[ 9], this.nodeIds[ 9], this.nodeIds[10], this.modesCB));
			network.addLink(createLink(network, this.linkIds[10], this.nodeIds[10], this.nodeIds[11], this.modesC));
			network.addLink(createLink(network, this.linkIds[11], this.nodeIds[11], this.nodeIds[12], this.modesC));
			network.addLink(createLink(network, this.linkIds[12], this.nodeIds[12], this.nodeIds[13], this.modesC));
			network.addLink(createLink(network, this.linkIds[13], this.nodeIds[ 1], this.nodeIds[ 4], this.modesWB));
			network.addLink(createLink(network, this.linkIds[14], this.nodeIds[ 4], this.nodeIds[ 7], this.modesCW));
			network.addLink(createLink(network, this.linkIds[15], this.nodeIds[ 7], this.nodeIds[10], this.modesW));
			network.addLink(createLink(network, this.linkIds[16], this.nodeIds[10], this.nodeIds[13], this.modesCBW));
		}

	}

	private static Link createLink(final Network network, final Id<Link> id, final Id<Node> fromNodeId, final Id<Node> toNodeId, final Set<String> modes) {
		Link link = network.getFactory().createLink(id, network.getNodes().get(fromNodeId), network.getNodes().get(toNodeId));
		link.setAllowedModes(modes);
		link.setCapacity(2000.0);
		link.setFreespeed(10.0);
		link.setLength(100.0);
		link.setNumberOfLanes(1);
		return link;
	}

	public static Set<String> createHashSet(String... modes) {
		Set<String> set = new HashSet<String>();
        Collections.addAll(set, modes);
		return set;
	}

}
