/* *********************************************************************** *
 * project: org.matsim.*
 * CompressedRouteTest.java
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
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkIdComparator;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.algorithms.SubsequentLinksAnalyzer;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author mrieser
 */
public class CompressedNetworkRouteTest extends AbstractNetworkRouteTest {

	@Override
	public NetworkRouteWRefs getNetworkRouteInstance(final Link fromLink, final Link toLink, final NetworkLayer network) {
		SubsequentLinksAnalyzer subsequent = new SubsequentLinksAnalyzer(network);
		return new CompressedNetworkRouteImpl(fromLink, toLink, subsequent.getSubsequentLinks());
	}

	/**
	 * Tests that setting some links results in the same links returned.
	 * This is usually not tested as it is assumed, that just storing some
	 * links and returning the same ones should not be error-prone, but it's
	 * different in this case where we do not actually store the links.
	 */
	public void testGetLinks_setLinks() {
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLink(new IdImpl("1"));
		Link link22 = network.getLink(new IdImpl("22"));
		Link link12 = network.getLink(new IdImpl("12"));
		Link link13 = network.getLink(new IdImpl("13"));
		Link linkM24 = network.getLink(new IdImpl("-24"));
		Link link4 = network.getLink(new IdImpl("4"));

		List<Link> links = new ArrayList<Link>(5);
		links.add(link22);
		links.add(link12);
		links.add(link13);
		links.add(linkM24);
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
		route.setLinks(link1, links, link4);

		List<Link> links2 = route.getLinks();
		assertEquals("wrong number of links.", links.size(), links2.size());
		for (int i = 0, n = links.size(); i < n; i++) {
			assertEquals("different link at position " + i, links.get(i), links2.get(i));
		}
	}

	public void testGetLinks_onlySubsequentLinks() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link1 = network.getLink(new IdImpl("1"));
		Link link2 = network.getLink(new IdImpl("2"));
		Link link3 = network.getLink(new IdImpl("3"));
		Link link4 = network.getLink(new IdImpl("4"));

		List<Link> links = new ArrayList<Link>(4);
		links.add(link1);
		links.add(link2);
		links.add(link3);

		Map<Link, Link> subsequentLinks = new TreeMap<Link, Link>(new LinkIdComparator());
		subsequentLinks.put(link0, link1);
		subsequentLinks.put(link1, link2);
		subsequentLinks.put(link2, link3);
		subsequentLinks.put(link3, link4);

		NetworkRouteWRefs route = new CompressedNetworkRouteImpl(link0, link4, subsequentLinks);
		route.setLinks(link0, links, link4);

		List<Link> links2 = route.getLinks();
		assertEquals("wrong number of links.", links.size(), links2.size());
		for (int i = 0, n = links.size(); i < n; i++) {
			assertEquals("different link at position " + i, links.get(i), links2.get(i));
		}
	}

	/**
	 * Tests that {@link CompressedNetworkRouteImpl#getLinks()} doesn't crash or
	 * hang when a route object is not correctly initialized.
	 */
	public void testGetLinks_incompleteInitialization() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link1 = network.getLink(new IdImpl("1"));
		Link link2 = network.getLink(new IdImpl("2"));
		Link link3 = network.getLink(new IdImpl("3"));
		Link link4 = network.getLink(new IdImpl("4"));

		Map<Link, Link> subsequentLinks = new TreeMap<Link, Link>(new LinkIdComparator());
		subsequentLinks.put(link0, link1);
		subsequentLinks.put(link1, link2);
		subsequentLinks.put(link2, link3);
		subsequentLinks.put(link3, link4);

		NetworkRouteWRefs route = new CompressedNetworkRouteImpl(link0, link4, subsequentLinks);
		// NO route.setLinks() here!

		assertEquals("expected 0 links.", 0, route.getLinks().size());
		assertEquals("expected 0 link ids.", 0, route.getLinkIds().size());
	}

	/**
	 * Tests that {@link CompressedNetworkRouteImpl#getNodes()} doesn't crash or
	 * hang when a route object is not correctly initialized.
	 */
	public void testGetNodes_incompleteInitialization() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link1 = network.getLink(new IdImpl("1"));
		Link link2 = network.getLink(new IdImpl("2"));
		Link link3 = network.getLink(new IdImpl("3"));
		Link link4 = network.getLink(new IdImpl("4"));

		Map<Link, Link> subsequentLinks = new TreeMap<Link, Link>(new LinkIdComparator());
		subsequentLinks.put(link0, link1);
		subsequentLinks.put(link1, link2);
		subsequentLinks.put(link2, link3);
		subsequentLinks.put(link3, link4);

		NetworkRouteWRefs route = new CompressedNetworkRouteImpl(link0, link4, subsequentLinks);
		// NO route.setLinks() here!

		assertEquals("expected 0 links.", 0, route.getNodes().size());
	}

	public void testClone() {
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		Id id4 = new IdImpl(4);
		Id id5 = new IdImpl(5);
		Id id6 = new IdImpl(6);

		Network network = new NetworkLayer();
		NetworkFactory builder = network.getFactory();

		Node node1 = builder.createNode(id1, new CoordImpl(0, 1000));
		Node node2 = builder.createNode(id2, new CoordImpl(0, 2000));
		Node node3 = builder.createNode(id3, new CoordImpl(0, 3000));
		Node node4 = builder.createNode(id4, new CoordImpl(0, 4000));
		Node node5 = builder.createNode(id5, new CoordImpl(0, 5000));
		Node node6 = builder.createNode(id6, new CoordImpl(0, 6000));

		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addNode(node6);

		Link startLink = builder.createLink(id1, node1.getId(), node2.getId());
		Link link3 = builder.createLink(id3, node2.getId(), node3.getId());
		Link link4 = builder.createLink(id4, node3.getId(), node4.getId());
		Link link5 = builder.createLink(id5, node4.getId(), node5.getId());
		Link endLink = builder.createLink(id2, node5.getId(), node6.getId());

		network.addLink(startLink);
		network.addLink(link3);
		network.addLink(link4);
		network.addLink(link5);
		network.addLink(endLink);

		Map<Link, Link> subsequentLinks = new TreeMap<Link, Link>(new LinkIdComparator());
		subsequentLinks.put(startLink, link3);
		subsequentLinks.put(link3, link4);
		subsequentLinks.put(link4, link5);
		subsequentLinks.put(link5, endLink);

		CompressedNetworkRouteImpl route1 = new CompressedNetworkRouteImpl(startLink, endLink, subsequentLinks);
		ArrayList<Link> srcRoute = new ArrayList<Link>();
		srcRoute.add(link3);
		srcRoute.add(link4);
		route1.setLinks(startLink, srcRoute, link5);
		assertEquals(2, route1.getLinks().size());

		CompressedNetworkRouteImpl route2 = route1.clone();

		srcRoute.add(link5);
		route1.setLinks(startLink, srcRoute, endLink);

		assertEquals(3, route1.getLinks().size());
		assertEquals(2, route2.getLinks().size());
	}

}
