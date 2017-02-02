/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package contrib.baseline.counts;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Testing NetworkAdapter.
 *
 * @author boescpa
 */
public class TestNetworkAdapter {

	@Ignore
	@Test
	public void testNoCutting() {
		Network network = getNetwork();
		NetworkAdapter adapter = new NetworkAdapter(network, 0.5, 0.1);
		Map<String, Integer> expectedCounts = new HashMap<>();
		expectedCounts.put("link_count_1", 10);
		expectedCounts.put("link_count_2", 20);
		Map<String, Integer> observedCounts = new HashMap<>();
		observedCounts.put("link_count_1", 15);
		observedCounts.put("link_count_2", 15);
		List<NetworkChangeEvent> networkChanges = adapter.identifyNetworkChanges(expectedCounts, observedCounts);

		double netChange_link_1 = 0;
		for (NetworkChangeEvent event : networkChanges) {
			if (event.getLinks().contains(network.getLinks().get(Id.createLinkId("link_1")))) {
				netChange_link_1 = event.getFlowCapacityChange().getValue();
			}
		}
		Assert.assertTrue("Observed value: " + netChange_link_1,
				netChange_link_1 == 1.125);
	}

	private Network getNetwork() {
		Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		//
		//	link_1									link_3
		//		  \								   /
		//			link_count_1  --  link_count_2
		//		  /								   \
		//	link_2									link_4
		//
		NetworkFactory factory = network.getFactory();
		Node node_1 = factory.createNode(Id.createNodeId("node_1"), new Coord(0, 0));
		Node node_2 = factory.createNode(Id.createNodeId("node_2"), new Coord(2, 0));
		Node node_3 = factory.createNode(Id.createNodeId("node_3"), new Coord(1, 1));
		Node node_4 = factory.createNode(Id.createNodeId("node_4"), new Coord(1, 2));
		Node node_5 = factory.createNode(Id.createNodeId("node_5"), new Coord(1, 3));
		Node node_6 = factory.createNode(Id.createNodeId("node_6"), new Coord(0, 4));
		Node node_7 = factory.createNode(Id.createNodeId("node_7"), new Coord(2, 4));
		Link link_1 = factory.createLink(Id.createLinkId("link_1"), node_1, node_3);
		Link link_2 = factory.createLink(Id.createLinkId("link_2"), node_2, node_3);
		Link link_count_1 = factory.createLink(Id.createLinkId("link_count_1"), node_3, node_4);
		Link link_count_2 = factory.createLink(Id.createLinkId("link_count_2"), node_4, node_5);
		Link link_3 = factory.createLink(Id.createLinkId("link_3"), node_5, node_6);
		Link link_4 = factory.createLink(Id.createLinkId("link_4"), node_5, node_7);
		network.addNode(node_1);
		network.addNode(node_2);
		network.addNode(node_3);
		network.addNode(node_4);
		network.addNode(node_5);
		network.addNode(node_6);
		network.addNode(node_7);
		network.addLink(link_1);
		network.addLink(link_2);
		network.addLink(link_count_1);
		network.addLink(link_count_2);
		network.addLink(link_3);
		network.addLink(link_4);
		return network;
	}
}
