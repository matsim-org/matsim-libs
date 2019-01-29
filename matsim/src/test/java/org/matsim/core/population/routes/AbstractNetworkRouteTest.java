/* *****inki****************************************************************** *
 * projecnkt: org.matsim.*
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
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;

/**
 * Tests several methods of {@link NetworkRoute}.
 * Classes inheriting from RouteImpl should be able to inherit from this
 * test, too, and only overwrite the method getRouteInstance().
 *
 * @author mrieser
 */
public abstract class AbstractNetworkRouteTest {

	static private final Logger log = Logger.getLogger(AbstractNetworkRouteTest.class);

	abstract protected NetworkRoute getNetworkRouteInstance(final Id<Link> fromLinkId, final Id<Link> toLinkId, final Network network);

	@Test
	public void testSetLinkIds() {
		Network network = createTestNetwork();
		List<Id<Link>> links = NetworkUtils.getLinkIds("-22 2 3 24 14");
		final Id<Link> link11 = Id.create(11, Link.class);
		final Id<Link> link15 = Id.create(15, Link.class);
		NetworkRoute route = getNetworkRouteInstance(link11, link15, network);
		route.setLinkIds(link11, links, link15);

		List<Id<Link>> linkIds = route.getLinkIds();
		Assert.assertEquals("number of links in route.", 5, linkIds.size());
		Assert.assertEquals(Id.create("-22", Link.class), linkIds.get(0));
		Assert.assertEquals(Id.create("2", Link.class), linkIds.get(1));
		Assert.assertEquals(Id.create("3", Link.class), linkIds.get(2));
		Assert.assertEquals(Id.create("24", Link.class), linkIds.get(3));
		Assert.assertEquals(Id.create("14", Link.class), linkIds.get(4));
	}

	@Test
	public void testSetLinks_linksNull() {
		Network network = createTestNetwork();
		Id<Link> link1 = Id.create("1", Link.class);
		Id<Link> link4 = Id.create("4", Link.class);
		NetworkRoute route = getNetworkRouteInstance(link1, link4, network);
		{
			//		route.setLinkIds(link1, NetworkUtils.getLinkIds("22 12 -23 3"), link4);
			//		List<Id<Link>> linkIds = route.getLinkIds();
			//		Assert.assertEquals("number of links in route.", 4, linkIds.size());
			// setting link ids twice is not allowed any more.  kai, sep'17
		}
		{
			Id<Link> link2 = Id.create("2", Link.class);
			route.setLinkIds(link1, null, link2);
			List<Id<Link>> linkIds2 = route.getLinkIds();
			Assert.assertEquals("number of links in route.", 0, linkIds2.size());
		}
	}

	@Test
	public void testSetLinks_AllNull() {
		Network network = createTestNetwork();
		Id<Link> link1 = Id.create("1", Link.class);
		Id<Link> link4 = Id.create("4", Link.class);
		NetworkRoute route = getNetworkRouteInstance(link1, link4, network);
		{
//			route.setLinkIds(link1, NetworkUtils.getLinkIds("22 12 -23 3"), link4);
//			List<Id<Link>> linkIds = route.getLinkIds();
//			Assert.assertEquals("number of links in route.", 4, linkIds.size());
			// setting link ids twice is not allowed any more.  kai, sep'17
		}
		{
			route.setLinkIds(null, null, null);
			List<Id<Link>> linkIds = route.getLinkIds();
			Assert.assertEquals("number of nodes in route.", 0, linkIds.size());
		}
	}

	@Test
	public void testGetDistance() {
		Network network = createTestNetwork();
		Id<Link> link1 = Id.create("1", Link.class);
		Id<Link> link4 = Id.create("4", Link.class);
		NetworkRoute route = getNetworkRouteInstance(link1, link4, network);
		route.setLinkIds(link1, NetworkUtils.getLinkIds("22 12 -23 3"), link4);
		route.setDistance(1234.5);

		Assert.assertEquals("wrong difference.", 1234.5, route.getDistance(), MatsimTestCase.EPSILON);
	}

	@Test
	public void testGetLinkIds() {
		Network network = createTestNetwork();
		Id<Link> link1 = Id.create("1", Link.class);
		Id<Link> link4 = Id.create("4", Link.class);
		NetworkRoute route = getNetworkRouteInstance(link1, link4, network);
		route.setLinkIds(link1, NetworkUtils.getLinkIds("22 12 -23 3"), link4);

		List<Id<Link>> ids = route.getLinkIds();
		Assert.assertEquals("number of links in route.", 4, ids.size());
		Assert.assertEquals(Id.create("22", Link.class), ids.get(0));
		Assert.assertEquals(Id.create("12", Link.class), ids.get(1));
		Assert.assertEquals(Id.create("-23", Link.class), ids.get(2));
		Assert.assertEquals(Id.create("3", Link.class), ids.get(3));
	}

	@Test
	public void testGetSubRoute() {
		Network network = createTestNetwork();
		Id<Link> id0 = Id.create("0", Link.class);
		Id<Link> id3 = Id.create("3", Link.class);
		Id<Link> id12 = Id.create("12", Link.class);
		Id<Link> id15 = Id.create("15", Link.class);
		Id<Link> id23m = Id.create("-23", Link.class);
		Id<Link> id24 = Id.create("24", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id0, id15, network);
		route.setLinkIds(id0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), id15);

		NetworkRoute subRoute = route.getSubRoute(id12, id24);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 2, linkIds.size());
		Assert.assertEquals(id23m, linkIds.get(0));
		Assert.assertEquals(id3, linkIds.get(1));
		Assert.assertEquals("wrong start link.", id12, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id24, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_fromStart() {
		Network network = createTestNetwork();
		Id<Link> id0 = Id.create("0", Link.class);
		Id<Link> id1 = Id.create("1", Link.class);
		Id<Link> id3 = Id.create("3", Link.class);
		Id<Link> id12 = Id.create("12", Link.class);
		Id<Link> id15 = Id.create("15", Link.class);
		Id<Link> id22 = Id.create("22", Link.class);
		Id<Link> id23m = Id.create("-23", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id0, id15, network);
		route.setLinkIds(id0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), id15);

		NetworkRoute subRoute = route.getSubRoute(id0, id3);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 4, linkIds.size());
		Assert.assertEquals(id1, linkIds.get(0));
		Assert.assertEquals(id22, linkIds.get(1));
		Assert.assertEquals(id12, linkIds.get(2));
		Assert.assertEquals(id23m, linkIds.get(3));
		Assert.assertEquals("wrong start link.", id0, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id3, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_toEnd() {
		Network network = createTestNetwork();
		Id<Link> id0 = Id.create("0", Link.class);
		Id<Link> id3 = Id.create("3", Link.class);
		Id<Link> id15 = Id.create("15", Link.class);
		Id<Link> id23m = Id.create("-23", Link.class);
		Id<Link> id24 = Id.create("24", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id0, id15, network);
		route.setLinkIds(id0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), id15);

		NetworkRoute subRoute = route.getSubRoute(id23m, id15);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 3, linkIds.size());
		Assert.assertEquals(id3, linkIds.get(0));
		Assert.assertEquals(id24, linkIds.get(1));
		Assert.assertEquals("wrong start link.", id23m, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id15, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_startOnly() {
		Network network = createTestNetwork();
		Id<Link> id0 = Id.create("0", Link.class);
		Id<Link> id15 = Id.create("15", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id0, id15, network);
		route.setLinkIds(id0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), id15);

		NetworkRoute subRoute = route.getSubRoute(id0, id0);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", id0, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id0, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_endOnly() {
		Network network = createTestNetwork();
		Id<Link> id0 = Id.create("0", Link.class);
		Id<Link> id15 = Id.create("15", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id0, id15, network);
		route.setLinkIds(id0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), id15);

		NetworkRoute subRoute = route.getSubRoute(id15, id15);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", id15, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id15, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_wrongStart() {
		Network network = createTestNetwork();
		Id<Link> id0 = Id.create("0", Link.class);
		Id<Link> id1 = Id.create("1", Link.class);
		Id<Link> id15 = Id.create("15", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id1, id15, network);
		route.setLinkIds(id1, NetworkUtils.getLinkIds("22 12 -23 3 24 14"), id15);

		try {
			route.getSubRoute(id0, id15);
			Assert.fail("expected IllegalArgumentException, but it did not happen.");
		} catch (IllegalArgumentException expected) {
			log.info("catched expected exception: " + expected.getMessage());
		}
	}

	@Test
	public void testGetSubRoute_wrongEnd() {
		Network network = createTestNetwork();
		Id<Link> id1 = Id.create("1", Link.class);
		Id<Link> id14 = Id.create("14", Link.class);
		Id<Link> id15 = Id.create("15", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id1, id14, network);
		route.setLinkIds(id1, NetworkUtils.getLinkIds("22 12 -23 3 24"), id14);

		try {
			route.getSubRoute(id1, id15);
			Assert.fail("expected IllegalArgumentException, but it did not happen.");
		} catch (IllegalArgumentException expected) {
			log.info("catched expected exception: " + expected.getMessage());
		}
	}

	@Test
	public void testGetSubRoute_sameLinks() {
		Network network = createTestNetwork();
		Id<Link> id1 = Id.create("1", Link.class);
		Id<Link> id12 = Id.create("12", Link.class);
		Id<Link> id14 = Id.create("14", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id1, id14, network);
		route.setLinkIds(id1, NetworkUtils.getLinkIds("22 12 -23 3 24"), id14);

		NetworkRoute subRoute = route.getSubRoute(id12, id12);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", id12, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id12, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_sameLinks_emptyRoute1() {
		Network network = createTestNetwork();
		Id<Link> id1 = Id.create("1", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id1, id1, network);
		route.setLinkIds(id1, null, id1);

		NetworkRoute subRoute = route.getSubRoute(id1, id1);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", id1, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id1, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_sameLinks_emptyRoute2() {
		Network network = createTestNetwork();
		Id<Link> id1 = Id.create("1", Link.class);
		Id<Link> id2 = Id.create("2", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id1, id2, network);
		route.setLinkIds(id1, null, id2);

		NetworkRoute subRoute = route.getSubRoute(id1, id1);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", id1, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id1, subRoute.getEndLinkId());

		NetworkRoute subRoute2 = route.getSubRoute(id2, id2);
		List<Id<Link>> linkIds2 = subRoute2.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds2.size());
		Assert.assertEquals("wrong start link.", id2, subRoute2.getStartLinkId());
		Assert.assertEquals("wrong end link.", id2, subRoute2.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_fullRoute() {
		Network network = createTestNetwork();
		Id<Link> id0 = Id.create("0", Link.class);
		Id<Link> id4 = Id.create("4", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id0, id4, network);
		route.setLinkIds(id0, NetworkUtils.getLinkIds("1 22 12 -23 3"), id4);

		NetworkRoute subRoute = route.getSubRoute(id0, id4);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 5, linkIds.size());
		Assert.assertEquals("wrong link.", Id.create("1", Link.class), subRoute.getLinkIds().get(0));
		Assert.assertEquals("wrong link.", Id.create("22", Link.class), subRoute.getLinkIds().get(1));
		Assert.assertEquals("wrong link.", Id.create("12", Link.class), subRoute.getLinkIds().get(2));
		Assert.assertEquals("wrong link.", Id.create("-23", Link.class), subRoute.getLinkIds().get(3));
		Assert.assertEquals("wrong link.", Id.create("3", Link.class), subRoute.getLinkIds().get(4));
		Assert.assertEquals("wrong start link.", id0, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id4, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_circleInRoute() {
		Network network = createTestNetwork();
		NetworkUtils.createAndAddLink(network,Id.create(-3, Link.class), network.getNodes().get(Id.create(4, Node.class)), network.getNodes().get(Id.create(3, Node.class)), 1000.0, 100.0, 3600.0, (double) 1 );
		Id<Link> id11 = Id.create("11", Link.class);
		Id<Link> id15 = Id.create("15", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id11, id15, network);
		route.setLinkIds(id11, NetworkUtils.getLinkIds("12 13 -24 -3 23 13 14"), id15);

		Id<Link> id12 = Id.create("12", Link.class);
		Id<Link> id14 = Id.create("14", Link.class);
		NetworkRoute subRoute = route.getSubRoute(id12, id14);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 5, linkIds.size());
		Assert.assertEquals("wrong link.", Id.create("13", Link.class), subRoute.getLinkIds().get(0));
		Assert.assertEquals("wrong link.", Id.create("-24", Link.class), subRoute.getLinkIds().get(1));
		Assert.assertEquals("wrong link.", Id.create("-3", Link.class), subRoute.getLinkIds().get(2));
		Assert.assertEquals("wrong link.", Id.create("23", Link.class), subRoute.getLinkIds().get(3));
		Assert.assertEquals("wrong link.", Id.create("13", Link.class), subRoute.getLinkIds().get(4));
		Assert.assertEquals("wrong start link.", id12, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id14, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_startInCircle() {
		Network network = createTestNetwork();
		NetworkUtils.createAndAddLink(network,Id.create(-3, Link.class), network.getNodes().get(Id.create(4, Node.class)), network.getNodes().get(Id.create(3, Node.class)), 1000.0, 100.0, 3600.0, (double) 1 );
		Id<Link> id11 = Id.create("11", Link.class);
		Id<Link> id15 = Id.create("15", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id11, id15, network);
		route.setLinkIds(id11, NetworkUtils.getLinkIds("12 13 -24 -3 23 13 14"), id15);

		Id<Link> id13 = Id.create("13", Link.class);
		Id<Link> id23 = Id.create("23", Link.class);
		NetworkRoute subRoute = route.getSubRoute(id13, id23);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 2, linkIds.size());
		Assert.assertEquals("wrong link.", Id.create("-24", Link.class), subRoute.getLinkIds().get(0));
		Assert.assertEquals("wrong link.", Id.create("-3", Link.class), subRoute.getLinkIds().get(1));
		Assert.assertEquals("wrong start link.", id13, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id23, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_startInCircle_CircleInEnd() {
		Network network = createTestNetwork();
		NetworkUtils.createAndAddLink(network,Id.create(-3, Link.class), network.getNodes().get(Id.create(4, Node.class)), network.getNodes().get(Id.create(3, Node.class)), 1000.0, 100.0, 3600.0, (double) 1 );
		Id<Link> id11 = Id.create("11", Link.class);
		Id<Link> id13 = Id.create("13", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id11, id13, network);
		route.setLinkIds(id11, NetworkUtils.getLinkIds("12 13 -24 -3 23"), id13);

		Id<Link> id_24 = Id.create("-24", Link.class);
		NetworkRoute subRoute = route.getSubRoute(id_24, id13);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 2, linkIds.size());
		Assert.assertEquals("wrong link.", Id.create("-3", Link.class), subRoute.getLinkIds().get(0));
		Assert.assertEquals("wrong link.", Id.create("23", Link.class), subRoute.getLinkIds().get(1));
		Assert.assertEquals("wrong start link.", id_24, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id13, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_CircleAtStart() {
		Network network = createTestNetwork();
		NetworkUtils.createAndAddLink(network,Id.create(-3, Link.class), network.getNodes().get(Id.create(4, Node.class)), network.getNodes().get(Id.create(3, Node.class)), 1000.0, 100.0, 3600.0, (double) 1 );
		Id<Link> id13 = Id.create("13", Link.class);
		Id<Link> id15 = Id.create("15", Link.class);
		NetworkRoute route = getNetworkRouteInstance(id13, id15, network);
		route.setLinkIds(id13, NetworkUtils.getLinkIds("-24 -3 23 13 14"), id15);

		NetworkRoute subRoute = route.getSubRoute(id13, id15);
		List<Id<Link>> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 1, linkIds.size());
		Assert.assertEquals("wrong link.", Id.create("14", Link.class), subRoute.getLinkIds().get(0));
		Assert.assertEquals("wrong start link.", id13, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id15, subRoute.getEndLinkId());
	}

	@Test
	public void testStartAndEndOnSameLinks_setLinks() {
		Network network = createTestNetwork();
		Id<Link> link = Id.create("3", Link.class);
		NetworkRoute route = getNetworkRouteInstance(link, link, network);
		route.setLinkIds(link, new ArrayList<Id<Link>>(0), link);
		Assert.assertEquals(0, route.getLinkIds().size());
	}

	@Test
	public void testStartAndEndOnSubsequentLinks_setLinks() {
		Network network = createTestNetwork();
		final Id<Link> link13 = Id.create("13", Link.class);
		final Id<Link> link14 = Id.create("14", Link.class);
		NetworkRoute route = getNetworkRouteInstance(link13, link14, network);
		route.setLinkIds(link13, new ArrayList<Id<Link>>(0), link14);
		Assert.assertEquals(0, route.getLinkIds().size());
	}

	@Test
	public void testVehicleId() {
		Network network = createTestNetwork();
		Id<Link> link0 = Id.create("0", Link.class);
		Id<Link> link15 = Id.create("15", Link.class);
		NetworkRoute route = getNetworkRouteInstance(link0, link15, network);
		Assert.assertNull(route.getVehicleId());
		Id<Vehicle> id = Id.create("8134", Vehicle.class);
		route.setVehicleId(id);
		Assert.assertEquals(id, route.getVehicleId());
		route.setVehicleId(null);
		Assert.assertNull(route.getVehicleId());
	}

	protected Network createTestNetwork() {
		/*
		 *  (11)----11---->(12)----12---->(13)----13---->(14)----14---->(15)----15---->(16)
		 *                  |^             |^             |^
		 *                  ||             ||             ||
		 *  ( 0)            |22            |23            |24
		 *    |0          -22|           -23|           -24|
		 *    v             v|             v|             v|
		 *  ( 1)-----1---->( 2)-----2---->( 3)-----3---->( 4)-----4---->( 5)
		 */
		Network network = NetworkUtils.createNetwork();
		Node node0 = NetworkUtils.createAndAddNode(network, Id.create("0", Node.class), new Coord((double) 0, (double) 500));
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 3000, (double) 0));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord((double) 4000, (double) 0));
		Node node11 = NetworkUtils.createAndAddNode(network, Id.create("11", Node.class), new Coord((double) 0, (double) 1000));
		Node node12 = NetworkUtils.createAndAddNode(network, Id.create("12", Node.class), new Coord((double) 1000, (double) 1000));
		Node node13 = NetworkUtils.createAndAddNode(network, Id.create("13", Node.class), new Coord((double) 2000, (double) 1000));
		Node node14 = NetworkUtils.createAndAddNode(network, Id.create("14", Node.class), new Coord((double) 3000, (double) 1000));
		Node node15 = NetworkUtils.createAndAddNode(network, Id.create("15", Node.class), new Coord((double) 4000, (double) 1000));
		Node node16 = NetworkUtils.createAndAddNode(network, Id.create("16", Node.class), new Coord((double) 5000, (double) 1000));
		final Node fromNode = node0;
		final Node toNode = node1;

		NetworkUtils.createAndAddLink(network,Id.create("0", Link.class), fromNode, toNode, 500.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode1 = node1;
		final Node toNode1 = node2;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode1, toNode1, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode2 = node2;
		final Node toNode2 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode2, toNode2, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode3 = node3;
		final Node toNode3 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode3, toNode3, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode4 = node4;
		final Node toNode4 = node5;
		NetworkUtils.createAndAddLink(network,Id.create("4", Link.class), fromNode4, toNode4, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode5 = node11;
		final Node toNode5 = node12;
		NetworkUtils.createAndAddLink(network,Id.create("11", Link.class), fromNode5, toNode5, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode6 = node12;
		final Node toNode6 = node13;
		NetworkUtils.createAndAddLink(network,Id.create("12", Link.class), fromNode6, toNode6, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode7 = node13;
		final Node toNode7 = node14;
		NetworkUtils.createAndAddLink(network,Id.create("13", Link.class), fromNode7, toNode7, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode8 = node14;
		final Node toNode8 = node15;
		NetworkUtils.createAndAddLink(network,Id.create("14", Link.class), fromNode8, toNode8, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode9 = node15;
		final Node toNode9 = node16;
		NetworkUtils.createAndAddLink(network,Id.create("15", Link.class), fromNode9, toNode9, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode10 = node2;
		final Node toNode10 = node12;
		NetworkUtils.createAndAddLink(network,Id.create("22", Link.class), fromNode10, toNode10, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode11 = node3;
		final Node toNode11 = node13;
		NetworkUtils.createAndAddLink(network,Id.create("23", Link.class), fromNode11, toNode11, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode12 = node4;
		final Node toNode12 = node14;
		NetworkUtils.createAndAddLink(network,Id.create("24", Link.class), fromNode12, toNode12, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode13 = node12;
		final Node toNode13 = node2;
		NetworkUtils.createAndAddLink(network,Id.create("-22", Link.class), fromNode13, toNode13, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode14 = node13;
		final Node toNode14 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("-23", Link.class), fromNode14, toNode14, 1000.0, 100.0, 3600.0, (double) 1 );
		final Node fromNode15 = node14;
		final Node toNode15 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("-24", Link.class), fromNode15, toNode15, 1000.0, 100.0, 3600.0, (double) 1 );

		return network;
	}

}
