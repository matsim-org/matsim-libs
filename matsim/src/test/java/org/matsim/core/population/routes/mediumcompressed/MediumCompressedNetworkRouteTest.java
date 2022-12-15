/* *********************************************************************** *
 * project: org.matsim.*
 * CompressedRouteTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.core.population.routes.mediumcompressed;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.AbstractNetworkRouteTest;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author mrieser
 */
public class MediumCompressedNetworkRouteTest extends AbstractNetworkRouteTest {

	@Override
	public NetworkRoute getNetworkRouteInstance(final Id<Link> fromLinkId, final Id<Link> toLinkId, final Network network) {
		MediumCompressedNetworkRouteFactory factory = new MediumCompressedNetworkRouteFactory();
		return factory.createRoute(fromLinkId, toLinkId);
	}

	/**
	 * Tests that setting some links results in the same links returned.
	 * This is usually not tested as it is assumed, that just storing some
	 * links and returning the same ones should not be error-prone, but it's
	 * different in this case where we do not actually store the links.
	 */
	@Test
	public void testGetLinks_setLinks() {
		Network network = createTestNetwork();
		Link link1 = network.getLinks().get(Id.create("1", Link.class));
		Link link22 = network.getLinks().get(Id.create("22", Link.class));
		Link link12 = network.getLinks().get(Id.create("12", Link.class));
		Link link13 = network.getLinks().get(Id.create("13", Link.class));
		Link linkM24 = network.getLinks().get(Id.create("-24", Link.class));
		Link link4 = network.getLinks().get(Id.create("4", Link.class));

		List<Id<Link>> linkIds = List.of(link22.getId(), link12.getId(), link13.getId(), linkM24.getId());
		NetworkRoute route = getNetworkRouteInstance(link1.getId(), link4.getId(), network);
		route.setLinkIds(link1.getId(), linkIds, link4.getId());

		List<Id<Link>> linksId2 = route.getLinkIds();
		Assert.assertEquals("wrong number of links.", linkIds.size(), linksId2.size());
		for (int i = 0, n = linkIds.size(); i < n; i++) {
			Assert.assertEquals("different link at position " + i, linkIds.get(i), linksId2.get(i));
		}
	}

	@Test
	public void testGetLinks_onlySubsequentLinks() {
		Network network = createTestNetwork();
		Link link0 = network.getLinks().get(Id.create("0", Link.class));
		Link link1 = network.getLinks().get(Id.create("1", Link.class));
		Link link2 = network.getLinks().get(Id.create("2", Link.class));
		Link link3 = network.getLinks().get(Id.create("3", Link.class));
		Link link4 = network.getLinks().get(Id.create("4", Link.class));

		List<Id<Link>> linkIds = List.of(link1.getId(), link2.getId(), link3.getId());

		NetworkRoute route = new MediumCompressedNetworkRoute(link0.getId(), link4.getId());
		route.setLinkIds(link0.getId(), linkIds, link4.getId());

		List<Id<Link>> linksId2 = route.getLinkIds();
		Assert.assertEquals("wrong number of links.", linkIds.size(), linksId2.size());
		for (int i = 0, n = linkIds.size(); i < n; i++) {
			Assert.assertEquals("different link at position " + i, linkIds.get(i), linksId2.get(i));
		}
	}

	/**
	 * Tests that {@link MediumCompressedNetworkRoute#getLinkIds()} doesn't crash or
	 * hang when a route object is not correctly initialized.
	 */
	@Test
	public void testGetLinkIds_incompleteInitialization() {
		Network network = createTestNetwork();
		Link link0 = network.getLinks().get(Id.create("0", Link.class));
		Link link4 = network.getLinks().get(Id.create("4", Link.class));

		NetworkRoute route = new MediumCompressedNetworkRoute(link0.getId(), link4.getId());
		// NO route.setLinks() here!

		Assert.assertEquals("expected 0 links.", 0, route.getLinkIds().size());
		Assert.assertEquals("expected 0 link ids.", 0, route.getLinkIds().size());
	}

	@Test
	public void testClone() {
		Network network = NetworkUtils.createNetwork();
        NetworkFactory builder = network.getFactory();

		Node node1 = builder.createNode(Id.create(1, Node.class), new Coord(0, 1000));
		Node node2 = builder.createNode(Id.create(2, Node.class), new Coord(0, 2000));
		Node node3 = builder.createNode(Id.create(3, Node.class), new Coord(0, 3000));
		Node node4 = builder.createNode(Id.create(4, Node.class), new Coord(0, 4000));
		Node node5 = builder.createNode(Id.create(5, Node.class), new Coord(0, 5000));
		Node node6 = builder.createNode(Id.create(6, Node.class), new Coord(0, 6000));

		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addNode(node6);

		Link startLink = builder.createLink(Id.create(1, Link.class), node1, node2);
		Link link3 = builder.createLink(Id.create(3, Link.class), node2, node3);
		Link link4 = builder.createLink(Id.create(4, Link.class), node3, node4);
		Link link5 = builder.createLink(Id.create(5, Link.class), node4, node5);
		Link endLink = builder.createLink(Id.create(2, Link.class), node5, node6);

		network.addLink(startLink);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);
		network.addLink(endLink);

		MediumCompressedNetworkRoute route1 = new MediumCompressedNetworkRoute(startLink.getId(), endLink.getId());
		ArrayList<Id<Link>> srcRoute = new ArrayList<>(5);
		Collections.addAll(srcRoute, link3.getId(), link4.getId());
		route1.setLinkIds(startLink.getId(), srcRoute, link5.getId());
		Assert.assertEquals(2, route1.getLinkIds().size());

		MediumCompressedNetworkRoute route2 = route1.clone();

		srcRoute.add(link5.getId());
		route2.setLinkIds(startLink.getId(), srcRoute, endLink.getId());

		Assert.assertEquals(2, route1.getLinkIds().size());
		Assert.assertEquals(3, route2.getLinkIds().size());
	}

	@Test
	public void testGetLinks_setLinks_alternative() {
		Network network = createTestNetwork();
		Link link1 = network.getLinks().get(Id.create("1", Link.class));
		Link link22 = network.getLinks().get(Id.create("22", Link.class));
		Link link12 = network.getLinks().get(Id.create("12", Link.class));
		Link linkM23 = network.getLinks().get(Id.create("-23", Link.class));
		Link link3 = network.getLinks().get(Id.create("3", Link.class));
		Link link4 = network.getLinks().get(Id.create("4", Link.class));

		List<Id<Link>> linkIds = List.of(link22.getId(), link12.getId(), linkM23.getId(), link3.getId());
		NetworkRoute route = getNetworkRouteInstance(link1.getId(), link4.getId(), network);
		route.setLinkIds(link1.getId(), linkIds, link4.getId());

		List<Id<Link>> linksId2 = route.getLinkIds();
		Assert.assertEquals("wrong number of links.", linkIds.size(), linksId2.size());
		for (int i = 0, n = linkIds.size(); i < n; i++) {
			Assert.assertEquals("different link at position " + i, linkIds.get(i), linksId2.get(i));
		}
	}

	@Test
	public void testGetLinks_setLinks_endLoopLink() {
		Network network = createTestNetwork();

		final Node node5 = network.getNodes().get(Id.create("5", Node.class));
		NetworkUtils.createAndAddLink(network,Id.create("loop5", Link.class), node5, node5, 1000.0, 100.0, 3600.0, 1);

		Link link1 = network.getLinks().get(Id.create("1", Link.class));
		Link link22 = network.getLinks().get(Id.create("22", Link.class));
		Link link12 = network.getLinks().get(Id.create("12", Link.class));
		Link linkM23 = network.getLinks().get(Id.create("-23", Link.class));
		Link link3 = network.getLinks().get(Id.create("3", Link.class));
		Link link4 = network.getLinks().get(Id.create("4", Link.class));
		Link linkLoop5 = network.getLinks().get(Id.create("loop5", Link.class));

		List<Id<Link>> linkIds = List.of(link22.getId(), link12.getId(), linkM23.getId(), link3.getId(), link4.getId());
		NetworkRoute route = getNetworkRouteInstance(link1.getId(), linkLoop5.getId(), network);
		route.setLinkIds(link1.getId(), linkIds, linkLoop5.getId());

		List<Id<Link>> linksId2 = route.getLinkIds();
		Assert.assertEquals("wrong number of links.", linkIds.size(), linksId2.size());
		for (int i = 0, n = linkIds.size(); i < n; i++) {
			Assert.assertEquals("different link at position " + i, linkIds.get(i), linksId2.get(i));
		}
	}

	@Test
	public void testGetLinks_setLinks_containsLargeLoop() {
		Network network = createTestNetwork();

		final Node node13 = network.getNodes().get(Id.create("13", Node.class));
		final Node node12 = network.getNodes().get(Id.create("12", Node.class));
		NetworkUtils.createAndAddLink(network,Id.create("-12", Link.class), node13, node12, 1000.0, 100.0, 3600.0, 1);

		Link link1 = network.getLinks().get(Id.create("1", Link.class));
		Link link2 = network.getLinks().get(Id.create("2", Link.class));
		Link link23 = network.getLinks().get(Id.create("23", Link.class));
		Link linkM12 = network.getLinks().get(Id.create("-12", Link.class));
		Link linkM22 = network.getLinks().get(Id.create("-22", Link.class));
		Link link3 = network.getLinks().get(Id.create("3", Link.class));
		Link link4 = network.getLinks().get(Id.create("4", Link.class));

		List<Id<Link>> linkIds = List.of(link2.getId(), link23.getId(), linkM12.getId(), linkM22.getId(), link2.getId(), link3.getId());

		NetworkRoute route = getNetworkRouteInstance(link1.getId(), link4.getId(), network);
		route.setLinkIds(link1.getId(), linkIds, link4.getId());

		List<Id<Link>> linksId2 = route.getLinkIds();
		Assert.assertEquals("wrong number of links.", linkIds.size(), linksId2.size());
		for (int i = 0, n = linkIds.size(); i < n; i++) {
			Assert.assertEquals("different link at position " + i, linkIds.get(i), linksId2.get(i));
		}
	}

	@Test
	public void testGetLinks_setLinks_containsLargeLoop_alternative() {
		Network network = createTestNetwork();

		network.removeLink(Id.create("4", Link.class));
		network.removeNode(Id.create("5", Node.class));
		network.removeLink(Id.create("14", Link.class));
		network.removeLink(Id.create("15", Link.class));
		network.removeNode(Id.create("15", Node.class));
		network.removeNode(Id.create("16", Node.class));

		final Node node14 = network.getNodes().get(Id.create("14", Node.class));
		final Node node13 = network.getNodes().get(Id.create("13", Node.class));
		final Node node12 = network.getNodes().get(Id.create("12", Node.class));
		NetworkUtils.createAndAddLink(network,Id.create("-12", Link.class), node13, node12, 1000.0, 100.0, 3600.0, 1);
		NetworkUtils.createAndAddLink(network,Id.create("-13", Link.class), node14, node13, 1000.0, 100.0, 3600.0, 1);

		Link link1 = network.getLinks().get(Id.create("1", Link.class));
		Link link2 = network.getLinks().get(Id.create("2", Link.class));
		Link link3 = network.getLinks().get(Id.create("3", Link.class));
		Link link24 = network.getLinks().get(Id.create("24", Link.class));
		Link linkM13 = network.getLinks().get(Id.create("-13", Link.class));
		Link linkM12 = network.getLinks().get(Id.create("-12", Link.class));
		Link linkM22 = network.getLinks().get(Id.create("-22", Link.class));
		Link link23 = network.getLinks().get(Id.create("23", Link.class));

		List<Id<Link>> linkIds = List.of(link2.getId(), link3.getId(), link24.getId(), linkM13.getId(), linkM12.getId(), linkM22.getId(), link2.getId());

		NetworkRoute route = getNetworkRouteInstance(link1.getId(), link23.getId(), network);
		route.setLinkIds(link1.getId(), linkIds, link23.getId());

		List<Id<Link>> linksId2 = route.getLinkIds();
		Assert.assertEquals("wrong number of links.", linkIds.size(), linksId2.size());
		for (int i = 0, n = linkIds.size(); i < n; i++) {
			Assert.assertEquals("different link at position " + i, linkIds.get(i), linksId2.get(i));
		}
	}

	@Test
	public void testGetLinks_setLinks_isLargeLoop() {
		Network network = createTestNetwork();

		final Node node14 = network.getNodes().get(Id.create("14", Node.class));
		final Node node13 = network.getNodes().get(Id.create("13", Node.class));
		final Node node12 = network.getNodes().get(Id.create("12", Node.class));
		NetworkUtils.createAndAddLink(network,Id.create("-12", Link.class), node13, node12, 1000.0, 100.0, 3600.0, 1);
		NetworkUtils.createAndAddLink(network,Id.create("-13", Link.class), node14, node13, 1000.0, 100.0, 3600.0, 1);

		Link link2 = network.getLinks().get(Id.create("2", Link.class));
		Link link3 = network.getLinks().get(Id.create("3", Link.class));
		Link link24 = network.getLinks().get(Id.create("24", Link.class));
		Link linkM13 = network.getLinks().get(Id.create("-13", Link.class));
		Link linkM12 = network.getLinks().get(Id.create("-12", Link.class));
		Link linkM22 = network.getLinks().get(Id.create("-22", Link.class));

		List<Id<Link>> linkIds = List.of(link3.getId(), link24.getId(), linkM13.getId(), linkM12.getId(), linkM22.getId());

		NetworkRoute route = getNetworkRouteInstance(link2.getId(), link2.getId(), network);
		route.setLinkIds(link2.getId(), linkIds, link2.getId());

		List<Id<Link>> linksId2 = route.getLinkIds();
		Assert.assertEquals("wrong number of links.", linkIds.size(), linksId2.size());
		for (int i = 0, n = linkIds.size(); i < n; i++) {
			Assert.assertEquals("different link at position " + i, linkIds.get(i), linksId2.get(i));
		}
	}

}
