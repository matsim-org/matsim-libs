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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.testcases.fakes.FakeLink;

/**
 * @author mrieser
 */
public class LinkNetworkRouteTest extends AbstractNetworkRouteTest {

	@Override
	public NetworkRouteWRefs getNetworkRouteInstance(final Link fromLink, final Link toLink, final NetworkLayer network) {
		return new LinkNetworkRouteImpl(fromLink, toLink);
	}

	@Test
	public void testSetNodes_subsequentLinks() {
		NetworkLayer network = createTestNetwork();
		LinkImpl link1 = network.getLinks().get(new IdImpl("1"));
		LinkImpl link2 = network.getLinks().get(new IdImpl("2"));
		NodeImpl node2 = network.getNodes().get(new IdImpl("2"));
		List<Node> nodes = new ArrayList<Node>();
		nodes.add(node2);

		NetworkRouteWRefs route = new LinkNetworkRouteImpl(link1, link2);
		route.setNodes(link1, nodes, link2);
		Assert.assertEquals("number of links.", 0, route.getLinkIds().size());
	}

	@Test
	public void testClone() {
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		Id id4 = new IdImpl(4);
		Id id5 = new IdImpl(5);
		Link startLink = new FakeLink(id1);
		Link endLink = new FakeLink(id2);
		Link link3 = new FakeLink(id3);
		Link link4 = new FakeLink(id4);
		Link link5 = new FakeLink(id5);
		LinkNetworkRouteImpl route1 = new LinkNetworkRouteImpl(startLink, endLink);
		ArrayList<Link> srcRoute = new ArrayList<Link>();
		srcRoute.add(link3);
		srcRoute.add(link4);
		route1.setLinks(startLink, srcRoute, endLink);
		Assert.assertEquals(2, route1.getLinkIds().size());

		LinkNetworkRouteImpl route2 = route1.clone();

		srcRoute.add(link5);
		route1.setLinks(startLink, srcRoute, endLink);

		Assert.assertEquals(3, route1.getLinkIds().size());
		Assert.assertEquals(2, route2.getLinkIds().size());
	}

}
