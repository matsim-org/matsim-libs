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

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.testcases.fakes.FakeNode;

/**
 * @author mrieser
 */
public class NodeNetworkRouteTest extends AbstractNetworkRouteTest {

	@Override
	public NetworkRouteWRefs getNetworkRouteInstance(final Link fromLink, final Link toLink, final NetworkLayer network) {
		return new NodeNetworkRouteImpl(fromLink, toLink);
	}

	public void testClone() {
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		Id id4 = new IdImpl(4);
		Id id5 = new IdImpl(5);
		Link startLink = new FakeLink(id1);
		Link endLink = new FakeLink(id2);
		Node node3 = new FakeNode(id3);
		Node node4 = new FakeNode(id4);
		Node node5 = new FakeNode(id5);
		NodeNetworkRouteImpl route1 = new NodeNetworkRouteImpl(startLink, endLink);
		ArrayList<Node> srcRoute = new ArrayList<Node>();
		srcRoute.add(node3);
		srcRoute.add(node4);
		route1.setNodes(startLink, srcRoute, endLink);
		assertEquals(2, route1.getNodes().size());

		NodeNetworkRouteImpl route2 = route1.clone();

		srcRoute.add(node5);
		route1.setNodes(startLink, srcRoute, endLink);

		assertEquals(3, route1.getNodes().size());
		assertEquals(2, route2.getNodes().size());
	}

}
