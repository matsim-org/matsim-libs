/* *********************************************************************** *
 * project: org.matsim.*
 * LinkRouteTest.java
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
import java.util.List;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.LinkNetworkRoute;

/**
 * @author mrieser
 */
public class LinkCarRouteTest extends AbstractNetworkRouteTest {

	@Override
	public NetworkRoute getCarRouteInstance(final Link fromLink, final Link toLink, Network network) {
		return new LinkNetworkRoute(fromLink, toLink);
	}

	public void testGetNodes_subsequentLinks_setLinks() {
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLink(new IdImpl("1"));
		Link link2 = network.getLink(new IdImpl("2"));
		Node node2 = network.getNode(new IdImpl("2"));

		NetworkRoute route = new LinkNetworkRoute(link1, link2);
		route.setLinks(link1, null, link2);
		assertEquals("number of links.", 0, route.getLinks().size());
		assertEquals("number of nodes.", 1, route.getNodes().size());
		assertEquals("wrong node.", node2, route.getNodes().get(0));
	}

	public void testGetNodes_subsequentLinks_setNodes() {
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLink(new IdImpl("1"));
		Link link2 = network.getLink(new IdImpl("2"));
		Node node2 = network.getNode(new IdImpl("2"));
		List<Node> nodes = new ArrayList<Node>();
		nodes.add(node2);

		NetworkRoute route = new LinkNetworkRoute(link1, link2);
		route.setNodes(link1, nodes, link2);
		assertEquals("number of links.", 0, route.getLinks().size());
		assertEquals("number of nodes.", 1, route.getNodes().size());
		assertEquals("wrong node.", node2, route.getNodes().get(0));
	}

}
