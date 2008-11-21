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

package org.matsim.population;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

/**
 * Tests several methods of {@link RouteImpl}.
 * Classes inheriting from RouteImpl should be able to inherit from this
 * test, too, and only overwrite the method getRouteInstance().
 *
 * @author mrieser
 */
public class RouteImplTest extends MatsimTestCase {

	static private final Logger log = Logger.getLogger(RouteImplTest.class);

	public Route getRouteInstance() {
		return new RouteImpl();
	}

	public void testSetRoute_asList() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		List<Node> nodes = new ArrayList<Node>();
		nodes.add(network.getNode(new IdImpl("12")));
		nodes.add(network.getNode(new IdImpl("13")));
		nodes.add(network.getNode(new IdImpl("3")));
		nodes.add(network.getNode(new IdImpl("4")));
		route.setRoute(nodes);

		Link[] links = route.getLinkRoute();
		assertEquals("number of links in route.", 3, links.length);
		assertEquals(network.getLink(new IdImpl("12")), links[0]);
		assertEquals(network.getLink(new IdImpl("-23")), links[1]);
		assertEquals(network.getLink(new IdImpl("3")), links[2]);
	}

	public void testSetRoute_asString() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		route.setRoute("2 12 13 3 4");

		Link[] links = route.getLinkRoute();
		assertEquals("number of links in route.", 4, links.length);
		assertEquals(network.getLink(new IdImpl("22")), links[0]);
		assertEquals(network.getLink(new IdImpl("12")), links[1]);
		assertEquals(network.getLink(new IdImpl("-23")), links[2]);
		assertEquals(network.getLink(new IdImpl("3")), links[3]);
	}

	public void testSetLinkRoute() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		List<Link> links = new ArrayList<Link>();
		links.add(network.getLink(new IdImpl("-22")));
		links.add(network.getLink(new IdImpl("2")));
		links.add(network.getLink(new IdImpl("3")));
		links.add(network.getLink(new IdImpl("24")));
		links.add(network.getLink(new IdImpl("14")));
		route.setLinkRoute(links);

		List<Node> nodes = route.getRoute();
		assertEquals("number of nodes in route.", 6, nodes.size());
		assertEquals(network.getNode(new IdImpl("12")), nodes.get(0));
		assertEquals(network.getNode(new IdImpl("2")), nodes.get(1));
		assertEquals(network.getNode(new IdImpl("3")), nodes.get(2));
		assertEquals(network.getNode(new IdImpl("4")), nodes.get(3));
		assertEquals(network.getNode(new IdImpl("14")), nodes.get(4));
		assertEquals(network.getNode(new IdImpl("15")), nodes.get(5));
	}

	public void testSetLinkRoute_Null() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		route.setRoute("2 12 13 3 4");
		List<Node> nodes = route.getRoute();
		assertEquals("number of nodes in route.", 5, nodes.size());

		route.setLinkRoute(null);
		nodes = route.getRoute();
		assertEquals("number of nodes in route.", 0, nodes.size());
	}

	/**
	 * Tests that the several methods for setting a route overwrite the previous
	 * route information and not only extend the route.
	 */
	public void testSetRouteOverwrites() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		route.setRoute("2 12 13 3 4");
		assertEquals("number of nodes in route.", 5, route.getRoute().size());

		route.setRoute("2 12 13");
		assertEquals("setRoute(String) does likely not clear existing route.", 3, route.getRoute().size());

		List<Node> nodes = new ArrayList<Node>();
		nodes.add(network.getNode(new IdImpl("12")));
		nodes.add(network.getNode(new IdImpl("13")));
		nodes.add(network.getNode(new IdImpl("3")));
		nodes.add(network.getNode(new IdImpl("4")));
		route.setRoute(nodes);
		assertEquals("setRoute(List<Node>) does likely not clear existing route.", 4, route.getRoute().size());

		List<Link> links = new ArrayList<Link>();
		links.add(network.getLink(new IdImpl("-22")));
		links.add(network.getLink(new IdImpl("2")));
		links.add(network.getLink(new IdImpl("3")));
		links.add(network.getLink(new IdImpl("24")));
		links.add(network.getLink(new IdImpl("14")));
		route.setLinkRoute(links);
		assertEquals("setLinkRoute(List<Link>) does likely not clear existing route.", 6, route.getRoute().size());
	}

	public void testGetDist() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		route.setRoute("2 12 13 3 4");

		assertEquals("different distance calculated.", 4000.0, route.getDist(), EPSILON);
	}

	public void testGetLinkIds() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		route.setRoute("2 12 13 3 4");

		List<Id> ids = route.getLinkIds();
		assertEquals("number of links in route.", 4, ids.size());
		assertEquals(new IdImpl("22"), ids.get(0));
		assertEquals(new IdImpl("12"), ids.get(1));
		assertEquals(new IdImpl("-23"), ids.get(2));
		assertEquals(new IdImpl("3"), ids.get(3));
	}

	public void testGetSubRoute() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		route.setRoute("1 2 12 13 3 4 14 15");

		Route subRoute = route.getSubRoute(network.getNode(new IdImpl("12")), network.getNode(new IdImpl("4")));
		List<Node> nodes = subRoute.getRoute();
		assertEquals("number of nodes in subRoute.", 4, nodes.size());
		assertEquals(network.getNode(new IdImpl("12")), nodes.get(0));
		assertEquals(network.getNode(new IdImpl("13")), nodes.get(1));
		assertEquals(network.getNode(new IdImpl("3")), nodes.get(2));
		assertEquals(network.getNode(new IdImpl("4")), nodes.get(3));
		Link[] links = subRoute.getLinkRoute();
		assertEquals("number of links in subRoute.", 3, links.length);
		assertEquals(network.getLink(new IdImpl("12")), links[0]);
		assertEquals(network.getLink(new IdImpl("-23")), links[1]);
		assertEquals(network.getLink(new IdImpl("3")), links[2]);
	}

	public void testGetSubRoute_fromStart() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		route.setRoute("1 2 12 13 3 4 14 15");

		Route subRoute = route.getSubRoute(network.getNode(new IdImpl("1")), network.getNode(new IdImpl("3")));
		List<Node> nodes = subRoute.getRoute();
		assertEquals("number of nodes in subRoute.", 5, nodes.size());
		assertEquals(network.getNode(new IdImpl("1")), nodes.get(0));
		assertEquals(network.getNode(new IdImpl("2")), nodes.get(1));
		assertEquals(network.getNode(new IdImpl("12")), nodes.get(2));
		assertEquals(network.getNode(new IdImpl("13")), nodes.get(3));
		assertEquals(network.getNode(new IdImpl("3")), nodes.get(4));
		Link[] links = subRoute.getLinkRoute();
		assertEquals("number of links in subRoute.", 4, links.length);
		assertEquals(network.getLink(new IdImpl("1")), links[0]);
		assertEquals(network.getLink(new IdImpl("22")), links[1]);
		assertEquals(network.getLink(new IdImpl("12")), links[2]);
		assertEquals(network.getLink(new IdImpl("-23")), links[3]);
	}

	public void testGetSubRoute_toEnd() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		route.setRoute("1 2 12 13 3 4 14 15");

		Route subRoute = route.getSubRoute(network.getNode(new IdImpl("4")), network.getNode(new IdImpl("15")));
		List<Node> nodes = subRoute.getRoute();
		assertEquals("number of nodes in subRoute.", 3, nodes.size());
		assertEquals(network.getNode(new IdImpl("4")), nodes.get(0));
		assertEquals(network.getNode(new IdImpl("14")), nodes.get(1));
		assertEquals(network.getNode(new IdImpl("15")), nodes.get(2));
		Link[] links = subRoute.getLinkRoute();
		assertEquals("number of links in subRoute.", 2, links.length);
		assertEquals(network.getLink(new IdImpl("24")), links[0]);
		assertEquals(network.getLink(new IdImpl("14")), links[1]);
	}

	public void testGetSubRoute_wrongStart() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		route.setRoute("1 2 12 13 3 4 14 15");

		try {
			route.getSubRoute(
					network.createNode(new IdImpl("99"), new CoordImpl(-100, -100)),
					network.getNode(new IdImpl("15")));
			fail("expected IllegalArgumentException, but it did not happen.");
		} catch (IllegalArgumentException expected) {
			log.info("catched expected exception: " + expected.getMessage());
		}
	}

	public void testGetSubRoute_wrongEnd() {
		NetworkLayer network = createTestNetwork();
		Route route = getRouteInstance();
		route.setRoute("1 2 12 13 3 4 14 15");

		try {
			route.getSubRoute(
					network.getNode(new IdImpl("15")),
					network.createNode(new IdImpl("99"), new CoordImpl(-100, -100)));
			fail("expected IllegalArgumentException, but it did not happen.");
		} catch (IllegalArgumentException expected) {
			log.info("catched expected exception: " + expected.getMessage());
		}
	}

	private NetworkLayer createTestNetwork() {
		/*
		 *  (11)----11---->(12)----12---->(13)----13---->(14)----14---->(15)
		 *                  |^             |^             |^
		 *                  ||             ||             ||
		 *                  |22            |23            |24
		 *                -22|           -23|           -24|
		 *                  v|             v|             v|
		 *  ( 1)-----1---->( 2)-----2---->( 3)-----3---->( 4)-----4---->( 5)
		 */
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(   0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(1000, 0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(2000, 0));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(3000, 0));
		Node node5 = network.createNode(new IdImpl("5"), new CoordImpl(4000, 0));
		Node node11 = network.createNode(new IdImpl("11"), new CoordImpl(   0, 1000));
		Node node12 = network.createNode(new IdImpl("12"), new CoordImpl(1000, 1000));
		Node node13 = network.createNode(new IdImpl("13"), new CoordImpl(2000, 1000));
		Node node14 = network.createNode(new IdImpl("14"), new CoordImpl(3000, 1000));
		Node node15 = network.createNode(new IdImpl("15"), new CoordImpl(4000, 1000));

		network.createLink(new IdImpl("1"), node1, node2, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("2"), node2, node3, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("3"), node3, node4, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("4"), node4, node5, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("11"), node11, node12, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("12"), node12, node13, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("13"), node13, node14, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("14"), node14, node15, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("22"), node2, node12, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("23"), node3, node13, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("24"), node4, node14, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("-22"), node12, node2, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("-23"), node13, node3, 1000.0, 100.0, 3600.0, 1);
		network.createLink(new IdImpl("-24"), node14, node4, 1000.0, 100.0, 3600.0, 1);

		Gbl.createWorld().setNetworkLayer(network);

		return network;
	}

}
