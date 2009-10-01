/* *********************************************************************** *
 * project: org.matsim.*
 * RouteImplTest.java
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

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests several methods of {@link NetworkRouteWRefs}.
 * Classes inheriting from RouteImpl should be able to inherit from this
 * test, too, and only overwrite the method getRouteInstance().
 *
 * @author mrieser
 */
public abstract class AbstractNetworkRouteTest extends MatsimTestCase {

	static private final Logger log = Logger.getLogger(AbstractNetworkRouteTest.class);

	abstract protected NetworkRouteWRefs getNetworkRouteInstance(final Link fromLink, final Link toLink, final NetworkLayer network);

	public void testSetNodes_asList() {
		NetworkLayer network = createTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, "12 13 3 4");
		final Link link11 = network.getLink(new IdImpl("11"));
		final Link link4 = network.getLink(new IdImpl("4"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link11, link4, network);
		route.setNodes(link11, nodes, link4);

		List<Link> links = route.getLinks();
		assertEquals("number of links in route.", 3, links.size());
		assertEquals(network.getLink(new IdImpl("12")), links.get(0));
		assertEquals(network.getLink(new IdImpl("-23")), links.get(1));
		assertEquals(network.getLink(new IdImpl("3")), links.get(2));
	}

	public void testSetLinks() {
		NetworkLayer network = createTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, "-22 2 3 24 14");
		final Link link11 = network.getLink(new IdImpl("11"));
		final Link link15 = network.getLink(new IdImpl("15"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link11, link15, network);
		route.setLinks(link11, links, link15);

		List<Node> nodes = route.getNodes();
		assertEquals("number of nodes in route.", 6, nodes.size());
		assertEquals(network.getNode(new IdImpl("12")), nodes.get(0));
		assertEquals(network.getNode(new IdImpl("2")), nodes.get(1));
		assertEquals(network.getNode(new IdImpl("3")), nodes.get(2));
		assertEquals(network.getNode(new IdImpl("4")), nodes.get(3));
		assertEquals(network.getNode(new IdImpl("14")), nodes.get(4));
		assertEquals(network.getNode(new IdImpl("15")), nodes.get(5));
	}

	public void testSetLinks_linksNull() {
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLink(new IdImpl("1"));
		Link link4 = network.getLink(new IdImpl("4"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
		route.setNodes(link1, NetworkUtils.getNodes(network, "2 12 13 3 4"), link4);
		List<Node> nodes = route.getNodes();
		assertEquals("number of nodes in route.", 5, nodes.size());

		Link link2 = network.getLink(new IdImpl("2"));
		route.setLinks(link1, null, link2);
		nodes = route.getNodes();
		assertEquals("number of nodes in route.", 1, nodes.size());
		assertEquals("wrong node in route.", network.getNode(new IdImpl("2")), nodes.get(0));
	}

	public void testSetLinks_AllNull() {
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLink(new IdImpl("1"));
		Link link4 = network.getLink(new IdImpl("4"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
		route.setNodes(link1, NetworkUtils.getNodes(network, "2 12 13 3 4"), link4);
		List<Node> nodes = route.getNodes();
		assertEquals("number of nodes in route.", 5, nodes.size());

		route.setLinks(null, null, null);
		nodes = route.getNodes();
		assertEquals("number of nodes in route.", 0, nodes.size());
	}

	/**
	 * Tests that the several methods for setting a route overwrite the previous
	 * route information and not only extend the route.
	 */
	public void testSetNodesSetLinksOverwrites() {
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLink(new IdImpl("1"));
		Link link4 = network.getLink(new IdImpl("4"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
		route.setNodes(link1, NetworkUtils.getNodes(network, "2 12 13 3 4"), link4);
		assertEquals("number of nodes in route.", 5, route.getNodes().size());

		route.setEndLink(network.getLink(new IdImpl("13")));
		route.setNodes(NetworkUtils.getNodes(network, "2 12 13"));
		assertEquals("setRoute(List<Node>) does likely not clear existing route.", 3, route.getNodes().size());

		List<Link> links = new ArrayList<Link>();
		links.add(network.getLink(new IdImpl("-22")));
		links.add(network.getLink(new IdImpl("2")));
		links.add(network.getLink(new IdImpl("3")));
		links.add(network.getLink(new IdImpl("24")));
		links.add(network.getLink(new IdImpl("14")));
		route.setLinks(network.getLink(new IdImpl("11")), links, network.getLink(new IdImpl("15")));
		assertEquals("setLinkRoute(List<Link>) does likely not clear existing route.", 6, route.getNodes().size());
	}

	public void testGetDist() {
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLink(new IdImpl("1"));
		Link link4 = network.getLink(new IdImpl("4"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
		route.setNodes(link1, NetworkUtils.getNodes(network, "2 12 13 3 4"), link4);

		assertEquals("different distance calculated.", 4000.0, route.getDistance(), EPSILON);
	}

	public void testGetLinkIds() {
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLink(new IdImpl("1"));
		Link link4 = network.getLink(new IdImpl("4"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
		route.setNodes(link1, NetworkUtils.getNodes(network, "2 12 13 3 4"), link4);

		List<Id> ids = route.getLinkIds();
		assertEquals("number of links in route.", 4, ids.size());
		assertEquals(new IdImpl("22"), ids.get(0));
		assertEquals(new IdImpl("12"), ids.get(1));
		assertEquals(new IdImpl("-23"), ids.get(2));
		assertEquals(new IdImpl("3"), ids.get(3));
	}

	public void testGetSubRoute() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link15 = network.getLink(new IdImpl("15"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setNodes(link0, NetworkUtils.getNodes(network, "1 2 12 13 3 4 14 15"), link15);

		NetworkRouteWRefs subRoute = route.getSubRoute(network.getNode(new IdImpl("12")), network.getNode(new IdImpl("4")));
		List<Node> nodes = subRoute.getNodes();
		assertEquals("number of nodes in subRoute.", 4, nodes.size());
		assertEquals(network.getNode(new IdImpl("12")), nodes.get(0));
		assertEquals(network.getNode(new IdImpl("13")), nodes.get(1));
		assertEquals(network.getNode(new IdImpl("3")), nodes.get(2));
		assertEquals(network.getNode(new IdImpl("4")), nodes.get(3));
		List<Link> links = subRoute.getLinks();
		assertEquals("number of links in subRoute.", 3, links.size());
		assertEquals(network.getLink(new IdImpl("12")), links.get(0));
		assertEquals(network.getLink(new IdImpl("-23")), links.get(1));
		assertEquals(network.getLink(new IdImpl("3")), links.get(2));
		assertEquals("wrong start link.", network.getLink(new IdImpl("22")), subRoute.getStartLink());
		assertEquals("wrong end link.", network.getLink(new IdImpl("24")), subRoute.getEndLink());
	}

	public void testGetSubRoute_fromStart() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link15 = network.getLink(new IdImpl("15"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setNodes(link0, NetworkUtils.getNodes(network, "1 2 12 13 3 4 14 15"), link15);

		NetworkRouteWRefs subRoute = route.getSubRoute(network.getNode(new IdImpl("1")), network.getNode(new IdImpl("3")));
		List<Node> nodes = subRoute.getNodes();
		assertEquals("number of nodes in subRoute.", 5, nodes.size());
		assertEquals(network.getNode(new IdImpl("1")), nodes.get(0));
		assertEquals(network.getNode(new IdImpl("2")), nodes.get(1));
		assertEquals(network.getNode(new IdImpl("12")), nodes.get(2));
		assertEquals(network.getNode(new IdImpl("13")), nodes.get(3));
		assertEquals(network.getNode(new IdImpl("3")), nodes.get(4));
		List<Link> links = subRoute.getLinks();
		assertEquals("number of links in subRoute.", 4, links.size());
		assertEquals(network.getLink(new IdImpl("1")), links.get(0));
		assertEquals(network.getLink(new IdImpl("22")), links.get(1));
		assertEquals(network.getLink(new IdImpl("12")), links.get(2));
		assertEquals(network.getLink(new IdImpl("-23")), links.get(3));
		assertEquals("wrong start link.", network.getLink(new IdImpl("0")), subRoute.getStartLink());
		assertEquals("wrong end link.", network.getLink(new IdImpl("3")), subRoute.getEndLink());
	}

	public void testGetSubRoute_toEnd() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link15 = network.getLink(new IdImpl("15"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setNodes(link0, NetworkUtils.getNodes(network, "1 2 12 13 3 4 14 15"), link15);

		NetworkRouteWRefs subRoute = route.getSubRoute(network.getNode(new IdImpl("4")), network.getNode(new IdImpl("15")));
		List<Node> nodes = subRoute.getNodes();
		assertEquals("number of nodes in subRoute.", 3, nodes.size());
		assertEquals(network.getNode(new IdImpl("4")), nodes.get(0));
		assertEquals(network.getNode(new IdImpl("14")), nodes.get(1));
		assertEquals(network.getNode(new IdImpl("15")), nodes.get(2));
		List<Link> links = subRoute.getLinks();
		assertEquals("number of links in subRoute.", 2, links.size());
		assertEquals(network.getLink(new IdImpl("24")), links.get(0));
		assertEquals(network.getLink(new IdImpl("14")), links.get(1));
		assertEquals("wrong start link.", network.getLink(new IdImpl("3")), subRoute.getStartLink());
		assertEquals("wrong end link.", network.getLink(new IdImpl("15")), subRoute.getEndLink());
	}

	public void testGetSubRoute_startOnly() {
		NetworkLayer network = createTestNetwork();
		Link link1 = network.getLink(new IdImpl("1"));
		Link link15 = network.getLink(new IdImpl("15"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link15, network);
		route.setNodes(link1, NetworkUtils.getNodes(network, "2 12 13 3 4 14 15"), link15);

		Node node2 = network.getNode(new IdImpl("2"));
		NetworkRouteWRefs subRoute = route.getSubRoute(node2, node2);
		List<Node> nodes = subRoute.getNodes();
		assertEquals("number of nodes in subRoute.", 1, nodes.size());
		assertEquals(node2, nodes.get(0));
		List<Link> links = subRoute.getLinks();
		assertEquals("number of links in subRoute.", 0, links.size());
		assertEquals("wrong start link.", network.getLink(new IdImpl("1")), subRoute.getStartLink());
		assertEquals("wrong end link.", network.getLink(new IdImpl("22")), subRoute.getEndLink());
	}

	public void testGetSubRoute_endOnly() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link15 = network.getLink(new IdImpl("15"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setNodes(link0, NetworkUtils.getNodes(network, "1 2 12 13 3 4 14 15"), link15);

		Node node15 = network.getNode(new IdImpl("15"));
		NetworkRouteWRefs subRoute = route.getSubRoute(node15, node15);
		List<Node> nodes = subRoute.getNodes();
		assertEquals("number of nodes in subRoute.", 1, nodes.size());
		assertEquals(node15, nodes.get(0));
		List<Link> links = subRoute.getLinks();
		assertEquals("number of links in subRoute.", 0, links.size());
		assertEquals("wrong start link.", network.getLink(new IdImpl("14")), subRoute.getStartLink());
		assertEquals("wrong end link.", network.getLink(new IdImpl("15")), subRoute.getEndLink());
	}

	public void testGetSubRoute_wrongStart() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link15 = network.getLink(new IdImpl("15"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setNodes(link0, NetworkUtils.getNodes(network, "1 2 12 13 3 4 14 15"), link15);

		try {
			route.getSubRoute(
					network.createAndAddNode(new IdImpl("99"), new CoordImpl(-100, -100)),
					network.getNode(new IdImpl("15")));
			fail("expected IllegalArgumentException, but it did not happen.");
		} catch (IllegalArgumentException expected) {
			log.info("catched expected exception: " + expected.getMessage());
		}
	}

	public void testGetSubRoute_wrongEnd() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link15 = network.getLink(new IdImpl("15"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setNodes(link0, NetworkUtils.getNodes(network, "1 2 12 13 3 4 14 15"), link15);

		try {
			route.getSubRoute(
					network.getNode(new IdImpl("15")),
					network.createAndAddNode(new IdImpl("99"), new CoordImpl(-100, -100)));
			fail("expected IllegalArgumentException, but it did not happen.");
		} catch (IllegalArgumentException expected) {
			log.info("catched expected exception: " + expected.getMessage());
		}
	}

	public void testGetSubRoute_sameNodes() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link15 = network.getLink(new IdImpl("15"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setNodes(link0, NetworkUtils.getNodes(network, "1 2 12 13 3 4 14 15"), link15);

		Node node = network.getNode(new IdImpl("3"));
		NetworkRouteWRefs subRoute = route.getSubRoute(node, node);
		List<Node> nodes = subRoute.getNodes();
		assertEquals("number of nodes in subRoute.", 1, nodes.size());
		assertEquals(node, nodes.get(0));
		List<Link> links = subRoute.getLinks();
		assertEquals("number of links in subRoute.", 0, links.size());
		assertEquals("wrong start link.", network.getLink(new IdImpl("-23")), subRoute.getStartLink());
		assertEquals("wrong end link.", network.getLink(new IdImpl("3")), subRoute.getEndLink());
	}

	public void testGetSubRoute_sameNodesInOneNodeRoute() {
		NetworkLayer network = createTestNetwork();
		Link link11 = network.getLink(new IdImpl("11"));
		Link link12 = network.getLink(new IdImpl("12"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link11, link12, network);
		route.setNodes(link11, NetworkUtils.getNodes(network, "12"), link12);

		Node node = network.getNode(new IdImpl("12"));
		NetworkRouteWRefs subRoute = route.getSubRoute(node, node);
		List<Node> nodes = subRoute.getNodes();
		assertEquals("number of nodes in subRoute.", 1, nodes.size());
		assertEquals(node, nodes.get(0));
		List<Link> links = subRoute.getLinks();
		assertEquals("number of links in subRoute.", 0, links.size());
		assertEquals("wrong start link.", network.getLink(new IdImpl("11")), subRoute.getStartLink());
		assertEquals("wrong end link.", network.getLink(new IdImpl("12")), subRoute.getEndLink());
	}

	public void testStartAndEndOnSameLinks_setNodes() {
		NetworkLayer network = createTestNetwork();
		Link link2 = network.getLink(new IdImpl("2"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link2, link2, network);
		route.setNodes(link2, new ArrayList<Node>(0), link2);
		assertEquals(0, route.getNodes().size());
		assertEquals(0, route.getLinks().size());
	}

	public void testStartAndEndOnSameLinks_setLinks() {
		NetworkLayer network = createTestNetwork();
		Link link = network.getLink(new IdImpl("3"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link, link, network);
		route.setLinks(link, new ArrayList<Link>(0), link);
		assertEquals(0, route.getNodes().size());
		assertEquals(0, route.getLinks().size());
	}

	public void testStartAndEndOnSubsequentLinks_setNodes() {
		NetworkLayer network = createTestNetwork();
		Link link12 = network.getLink(new IdImpl("12"));
		Link link13 = network.getLink(new IdImpl("13"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link12, link13, network);
		route.setNodes(link12, NetworkUtils.getNodes(network, "13"), link13);
		assertEquals(1, route.getNodes().size());
		assertEquals(0, route.getLinks().size());
	}

	public void testStartAndEndOnSubsequentLinks_setLinks() {
		NetworkLayer network = createTestNetwork();
		final Link link13 = network.getLink(new IdImpl("13"));
		final Link link14 = network.getLink(new IdImpl("14"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link13, link14, network);
		route.setLinks(link13, new ArrayList<Link>(0), link14);
		assertEquals(1, route.getNodes().size());
		assertEquals(0, route.getLinks().size());
		assertEquals(network.getNode(new IdImpl(14)), route.getNodes().get(0));
	}

	public void testVehicleId() {
		NetworkLayer network = createTestNetwork();
		Link link0 = network.getLink(new IdImpl("0"));
		Link link15 = network.getLink(new IdImpl("15"));
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		assertNull(route.getVehicleId());
		Id id = new IdImpl("8134");
		route.setVehicleId(id);
		assertEquals(id, route.getVehicleId());
		route.setVehicleId(null);
		assertNull(route.getVehicleId());
	}

	protected NetworkLayer createTestNetwork() {
		/*
		 *  (11)----11---->(12)----12---->(13)----13---->(14)----14---->(15)----15---->(16)
		 *                  |^             |^             |^
		 *                  ||             ||             ||
		 *  ( 0)            |22            |23            |24
		 *    |0          -22|           -23|           -24|
		 *    v             v|             v|             v|
		 *  ( 1)-----1---->( 2)-----2---->( 3)-----3---->( 4)-----4---->( 5)
		 */
		NetworkLayer network = new NetworkLayer();
		NodeImpl node0 = network.createAndAddNode(new IdImpl("0"), new CoordImpl(   0, 500));
		NodeImpl node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(   0, 0));
		NodeImpl node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(1000, 0));
		NodeImpl node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(2000, 0));
		NodeImpl node4 = network.createAndAddNode(new IdImpl("4"), new CoordImpl(3000, 0));
		NodeImpl node5 = network.createAndAddNode(new IdImpl("5"), new CoordImpl(4000, 0));
		NodeImpl node11 = network.createAndAddNode(new IdImpl("11"), new CoordImpl(   0, 1000));
		NodeImpl node12 = network.createAndAddNode(new IdImpl("12"), new CoordImpl(1000, 1000));
		NodeImpl node13 = network.createAndAddNode(new IdImpl("13"), new CoordImpl(2000, 1000));
		NodeImpl node14 = network.createAndAddNode(new IdImpl("14"), new CoordImpl(3000, 1000));
		NodeImpl node15 = network.createAndAddNode(new IdImpl("15"), new CoordImpl(4000, 1000));
		NodeImpl node16 = network.createAndAddNode(new IdImpl("16"), new CoordImpl(5000, 1000));

		network.createAndAddLink(new IdImpl("0"), node0, node1,  500.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("1"), node1, node2, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("2"), node2, node3, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("3"), node3, node4, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("4"), node4, node5, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("11"), node11, node12, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("12"), node12, node13, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("13"), node13, node14, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("14"), node14, node15, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("15"), node15, node16, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("22"), node2, node12, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("23"), node3, node13, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("24"), node4, node14, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("-22"), node12, node2, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("-23"), node13, node3, 1000.0, 100.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("-24"), node14, node4, 1000.0, 100.0, 3600.0, 1);

		Gbl.createWorld().setNetworkLayer(network);

		return network;
	}

}
