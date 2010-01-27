/* *********************************************************************** *
 * project: org.matsim.*
 * NodeCarRouteTest.java
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

package org.matsim.core.population.routes;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

/**
 * @author mrieser
 */
public class NodeNetworkRouteTest extends AbstractNetworkRouteTest {

	@Override
	public NetworkRouteWRefs getNetworkRouteInstance(final Link fromLink, final Link toLink, final NetworkLayer network) {
		return new NodeNetworkRouteImpl(fromLink, toLink);
	}

	@Test
	public void testClone() {
		Scenario scenario = new ScenarioImpl();
		Id id1 = scenario.createId("1");
		Id id2 = scenario.createId("2");
		Id id3 = scenario.createId("3");
		Id id4 = scenario.createId("4");
		Id id5 = scenario.createId("5");
		Network network = scenario.getNetwork();
		Node node3 = network.getFactory().createNode(id3, scenario.createCoord(0, 0));
		Node node4 = network.getFactory().createNode(id4, scenario.createCoord(100, 0));
		Node node5 = network.getFactory().createNode(id5, scenario.createCoord(200, 0));
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		Link startLink = network.getFactory().createLink(id1, id3, id4);
		Link endLink = network.getFactory().createLink(id2, id4, id5);
		network.addLink(startLink);
		network.addLink(endLink);

		NodeNetworkRouteImpl route1 = new NodeNetworkRouteImpl(startLink, endLink);
		ArrayList<Node> srcRoute = new ArrayList<Node>();
		srcRoute.add(node3);
		srcRoute.add(node4);
		route1.setNodes(startLink, srcRoute, endLink);
		Assert.assertEquals(1, route1.getLinkIds().size());

		NodeNetworkRouteImpl route2 = route1.clone();
		Assert.assertNotSame(route1, route2);

		srcRoute.add(node5);
		route1.setNodes(startLink, srcRoute, endLink);

		Assert.assertEquals(2, route1.getLinkIds().size());
		Assert.assertEquals(1, route2.getLinkIds().size());
	}

}
