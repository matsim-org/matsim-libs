
/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkFilterManagerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.network.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.collect.ImmutableSet;

/**
 * @author mstraub Austrian Institute of Technology
 */
public class NetworkFilterManagerTest {

	private static final double DELTA = 0.00001;

	private static final double CAPACITY = 12.34;
	private static final double FREESPEED = 13.888;
	private static final double LENGTH = 777;
	private static final int NR_OF_LANES = 2;
	private static final String ATTRIBUTE_KEY = "key";
	private static final String ATTRIBUTE_VALUE = "value";
	private static final String ATTRIBUTE_KEY2 = "key2";
	private static final int ATTRIBUTE_VALUE2 = 2;

	private Network filterNetwork;

	@BeforeEach
	public void prepareTestAllowedModes() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();

		Node a = network.getFactory().createNode(Id.create("a", Node.class), new Coord(0, 0));
		Node b = network.getFactory().createNode(Id.create("b", Node.class), new Coord(0, LENGTH));
		Node c = network.getFactory().createNode(Id.create("c", Node.class), new Coord(LENGTH, LENGTH));
		Link ab = network.getFactory().createLink(Id.create("ab", Link.class), a, b);
		Link ac = network.getFactory().createLink(Id.create("ac", Link.class), a, c);

		enrichLink(ab);
		enrichLink(ac);

		network.addNode(a);
		network.addNode(b);
		network.addNode(c);
		network.addLink(ab);
		network.addLink(ac);

		filterNetwork = network;
	}

	private static void enrichLink(Link link) {
		link.setAllowedModes(ImmutableSet.of("car"));
		link.setCapacity(CAPACITY);
		link.setFreespeed(FREESPEED);
		link.setLength(LENGTH);
		link.setNumberOfLanes(NR_OF_LANES);
		link.getAttributes().putAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
		link.getAttributes().putAttribute(ATTRIBUTE_KEY2, ATTRIBUTE_VALUE2);
	}

	@Test
	void filterTest() {
		NetworkFilterManager networkFilterManager = new NetworkFilterManager(filterNetwork, new NetworkConfigGroup());
		networkFilterManager.addNodeFilter(new NetworkNodeFilter() {
			@Override
			public boolean judgeNode(Node n) {
				if (n.getId().toString().equals("a"))
					return true;
				return false;
			}
		});
		networkFilterManager.addLinkFilter(new NetworkLinkFilter() {
			@Override
			public boolean judgeLink(Link l) {
				if (l.getId().toString().equals("ac"))
					return false;
				return true;
			}
		});

		Network filteredNetwork = networkFilterManager.applyFilters();

		Assertions.assertEquals(2, filteredNetwork.getNodes().size());
		Assertions.assertTrue(filteredNetwork.getNodes().containsKey(Id.createNodeId("a")), "must be added by nodefilter");
		Assertions.assertTrue(filteredNetwork.getNodes().containsKey(Id.createNodeId("b")), "must be added for ab link");

		Assertions.assertEquals(1, filteredNetwork.getLinks().size());

		Link ab = filteredNetwork.getLinks().get(Id.createLinkId("ab"));
		Assertions.assertEquals(CAPACITY, ab.getCapacity(), DELTA);
		Assertions.assertEquals(FREESPEED, ab.getFreespeed(), DELTA);
		Assertions.assertEquals(LENGTH, ab.getLength(), DELTA);
		Assertions.assertEquals(NR_OF_LANES, ab.getNumberOfLanes(), DELTA);
		Assertions.assertEquals(ATTRIBUTE_VALUE, ab.getAttributes().getAttribute(ATTRIBUTE_KEY));
		Assertions.assertEquals(ATTRIBUTE_VALUE2, ab.getAttributes().getAttribute(ATTRIBUTE_KEY2));
	}

}
