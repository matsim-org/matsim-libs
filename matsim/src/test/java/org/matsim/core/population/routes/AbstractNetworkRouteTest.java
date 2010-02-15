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
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
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
public abstract class AbstractNetworkRouteTest {

	static private final Logger log = Logger.getLogger(AbstractNetworkRouteTest.class);

	abstract protected NetworkRouteWRefs getNetworkRouteInstance(final Id fromLinkId, final Id toLinkId, final NetworkLayer network);

	@Test
	public void testSetLinkIds() {
		NetworkLayer network = createTestNetwork();
		List<Id> links = NetworkUtils.getLinkIds("-22 2 3 24 14");
		final Id link11 = new IdImpl("11");
		final Id link15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(link11, link15, network);
		route.setLinkIds(link11, links, link15);

		List<Id> linkIds = route.getLinkIds();
		Assert.assertEquals("number of links in route.", 5, linkIds.size());
		Assert.assertEquals(new IdImpl("-22"), linkIds.get(0));
		Assert.assertEquals(new IdImpl("2"), linkIds.get(1));
		Assert.assertEquals(new IdImpl("3"), linkIds.get(2));
		Assert.assertEquals(new IdImpl("24"), linkIds.get(3));
		Assert.assertEquals(new IdImpl("14"), linkIds.get(4));
	}

	@Test
	public void testSetLinks_linksNull() {
		NetworkLayer network = createTestNetwork();
		Id link1 = new IdImpl("1");
		Id link4 = new IdImpl("4");
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
		route.setLinkIds(link1, NetworkUtils.getLinkIds("22 12 -23 3"), link4);
		List<Id> linkIds = route.getLinkIds();
		Assert.assertEquals("number of links in route.", 4, linkIds.size());

		Id link2 = new IdImpl("2");
		route.setLinkIds(link1, null, link2);
		linkIds = route.getLinkIds();
		Assert.assertEquals("number of links in route.", 0, linkIds.size());
	}

	@Test
	public void testSetLinks_AllNull() {
		NetworkLayer network = createTestNetwork();
		Id link1 = new IdImpl("1");
		Id link4 = new IdImpl("4");
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
		route.setLinkIds(link1, NetworkUtils.getLinkIds("22 12 -23 3"), link4);
		List<Id> linkIds = route.getLinkIds();
		Assert.assertEquals("number of links in route.", 4, linkIds.size());

		route.setLinkIds(null, null, null);
		linkIds = route.getLinkIds();
		Assert.assertEquals("number of nodes in route.", 0, linkIds.size());
	}

	@Test
	public void testGetDist() {
		NetworkLayer network = createTestNetwork();
		Id link1 = new IdImpl("1");
		Id link4 = new IdImpl("4");
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
		route.setLinkIds(link1, NetworkUtils.getLinkIds("22 12 -23 3"), link4);

		Assert.assertEquals("different distance calculated.", 4000.0, route.getDistance(), MatsimTestCase.EPSILON);
	}

	@Test
	public void testGetLinkIds() {
		NetworkLayer network = createTestNetwork();
		Id link1 = new IdImpl("1");
		Id link4 = new IdImpl("4");
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link4, network);
		route.setLinkIds(link1, NetworkUtils.getLinkIds("22 12 -23 3"), link4);

		List<Id> ids = route.getLinkIds();
		Assert.assertEquals("number of links in route.", 4, ids.size());
		Assert.assertEquals(new IdImpl("22"), ids.get(0));
		Assert.assertEquals(new IdImpl("12"), ids.get(1));
		Assert.assertEquals(new IdImpl("-23"), ids.get(2));
		Assert.assertEquals(new IdImpl("3"), ids.get(3));
	}

	@Test
	public void testGetSubRoute_old() {
		NetworkLayer network = createTestNetwork();
		Id link0 = new IdImpl("0");
		Id link15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setLinkIds(link0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), link15);

		NetworkRouteWRefs subRoute = route.getSubRoute(network.getNodes().get(new IdImpl("12")), network.getNodes().get(new IdImpl("4")));
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 3, linkIds.size());
		Assert.assertEquals(network.getLinks().get(new IdImpl("12")).getId(), linkIds.get(0));
		Assert.assertEquals(network.getLinks().get(new IdImpl("-23")).getId(), linkIds.get(1));
		Assert.assertEquals(network.getLinks().get(new IdImpl("3")).getId(), linkIds.get(2));
		Assert.assertEquals("wrong start link.", network.getLinks().get(new IdImpl("22")).getId(), subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", network.getLinks().get(new IdImpl("24")).getId(), subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute() {
		NetworkLayer network = createTestNetwork();
		Id id0 = new IdImpl("0");
		Id id3 = new IdImpl("3");
		Id id12 = new IdImpl("12");
		Id id15 = new IdImpl("15");
		Id id23m = new IdImpl("-23");
		Id id24 = new IdImpl("24");
		NetworkRouteWRefs route = getNetworkRouteInstance(id0, id15, network);
		route.setLinkIds(id0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), id15);

		NetworkRouteWRefs subRoute = route.getSubRoute(id12, id24);
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 2, linkIds.size());
		Assert.assertEquals(id23m, linkIds.get(0));
		Assert.assertEquals(id3, linkIds.get(1));
		Assert.assertEquals("wrong start link.", id12, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id24, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_fromStart_old() {
		NetworkLayer network = createTestNetwork();
		Id link0 = new IdImpl("0");
		Id link15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setLinkIds(link0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), link15);

		NetworkRouteWRefs subRoute = route.getSubRoute(network.getNodes().get(new IdImpl("1")), network.getNodes().get(new IdImpl("3")));
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 4, linkIds.size());
		Assert.assertEquals(network.getLinks().get(new IdImpl("1")).getId(), linkIds.get(0));
		Assert.assertEquals(network.getLinks().get(new IdImpl("22")).getId(), linkIds.get(1));
		Assert.assertEquals(network.getLinks().get(new IdImpl("12")).getId(), linkIds.get(2));
		Assert.assertEquals(network.getLinks().get(new IdImpl("-23")).getId(), linkIds.get(3));
		Assert.assertEquals("wrong start link.", network.getLinks().get(new IdImpl("0")).getId(), subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", network.getLinks().get(new IdImpl("3")).getId(), subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_fromStart() {
		NetworkLayer network = createTestNetwork();
		Id id0 = new IdImpl("0");
		Id id1 = new IdImpl("1");
		Id id3 = new IdImpl("3");
		Id id12 = new IdImpl("12");
		Id id15 = new IdImpl("15");
		Id id22 = new IdImpl("22");
		Id id23m = new IdImpl("-23");
		NetworkRouteWRefs route = getNetworkRouteInstance(id0, id15, network);
		route.setLinkIds(id0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), id15);

		NetworkRouteWRefs subRoute = route.getSubRoute(id0, id3);
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 4, linkIds.size());
		Assert.assertEquals(id1, linkIds.get(0));
		Assert.assertEquals(id22, linkIds.get(1));
		Assert.assertEquals(id12, linkIds.get(2));
		Assert.assertEquals(id23m, linkIds.get(3));
		Assert.assertEquals("wrong start link.", id0, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id3, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_toEnd_old() {
		NetworkLayer network = createTestNetwork();
		Id link0 = new IdImpl("0");
		Id link15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setLinkIds(link0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), link15);

		NetworkRouteWRefs subRoute = route.getSubRoute(network.getNodes().get(new IdImpl("4")), network.getNodes().get(new IdImpl("15")));
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 2, linkIds.size());
		Assert.assertEquals(network.getLinks().get(new IdImpl("24")).getId(), linkIds.get(0));
		Assert.assertEquals(network.getLinks().get(new IdImpl("14")).getId(), linkIds.get(1));
		Assert.assertEquals("wrong start link.", network.getLinks().get(new IdImpl("3")).getId(), subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", network.getLinks().get(new IdImpl("15")).getId(), subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_toEnd() {
		NetworkLayer network = createTestNetwork();
		Id id0 = new IdImpl("0");
		Id id3 = new IdImpl("3");
		Id id15 = new IdImpl("15");
		Id id23m = new IdImpl("-23");
		Id id24 = new IdImpl("24");
		NetworkRouteWRefs route = getNetworkRouteInstance(id0, id15, network);
		route.setLinkIds(id0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), id15);

		NetworkRouteWRefs subRoute = route.getSubRoute(id23m, id15);
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 3, linkIds.size());
		Assert.assertEquals(id3, linkIds.get(0));
		Assert.assertEquals(id24, linkIds.get(1));
		Assert.assertEquals("wrong start link.", id23m, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id15, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_startOnly_old() {
		NetworkLayer network = createTestNetwork();
		Id link1 = new IdImpl("1");
		Id link15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(link1, link15, network);
		route.setLinkIds(link1, NetworkUtils.getLinkIds("22 12 -23 3 24 14"), link15);

		Node node2 = network.getNodes().get(new IdImpl("2"));
		NetworkRouteWRefs subRoute = route.getSubRoute(node2, node2);
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", network.getLinks().get(new IdImpl("1")).getId(), subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", network.getLinks().get(new IdImpl("22")).getId(), subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_startOnly() {
		NetworkLayer network = createTestNetwork();
		Id id0 = new IdImpl("0");
		Id id15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(id0, id15, network);
		route.setLinkIds(id0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), id15);

		NetworkRouteWRefs subRoute = route.getSubRoute(id0, id0);
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", id0, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id0, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_endOnly_old() {
		NetworkLayer network = createTestNetwork();
		Id link0 = new IdImpl("0");
		Id link15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setLinkIds(link0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), link15);

		Node node15 = network.getNodes().get(new IdImpl("15"));
		NetworkRouteWRefs subRoute = route.getSubRoute(node15, node15);
		Assert.assertEquals("number of links in subRoute.", 0, subRoute.getLinkIds().size());
		Assert.assertEquals("wrong start link.", network.getLinks().get(new IdImpl("14")).getId(), subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", network.getLinks().get(new IdImpl("15")).getId(), subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_endOnly() {
		NetworkLayer network = createTestNetwork();
		Id id0 = new IdImpl("0");
		Id id15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(id0, id15, network);
		route.setLinkIds(id0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), id15);

		NetworkRouteWRefs subRoute = route.getSubRoute(id15, id15);
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", id15, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id15, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_wrongStart_old() {
		NetworkLayer network = createTestNetwork();
		Id link0 = new IdImpl("0");
		Id link15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setLinkIds(link0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), link15);

		try {
			route.getSubRoute(
					network.createAndAddNode(new IdImpl("99"), new CoordImpl(-100, -100)),
					network.getNodes().get(new IdImpl("15")));
			Assert.fail("expected IllegalArgumentException, but it did not happen.");
		} catch (IllegalArgumentException expected) {
			log.info("catched expected exception: " + expected.getMessage());
		}
	}

	@Test
	public void testGetSubRoute_wrongStart() {
		NetworkLayer network = createTestNetwork();
		Id id0 = new IdImpl("0");
		Id id1 = new IdImpl("1");
		Id id15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(id1, id15, network);
		route.setLinkIds(id1, NetworkUtils.getLinkIds("22 12 -23 3 24 14"), id15);

		try {
			route.getSubRoute(id0, id15);
			Assert.fail("expected IllegalArgumentException, but it did not happen.");
		} catch (IllegalArgumentException expected) {
			log.info("catched expected exception: " + expected.getMessage());
		}
	}

	@Test
	public void testGetSubRoute_wrongEnd_old() {
		NetworkLayer network = createTestNetwork();
		Id link0 = new IdImpl("0");
		Id link15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setLinkIds(link0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), link15);

		try {
			route.getSubRoute(
					network.getNodes().get(new IdImpl("15")),
					network.createAndAddNode(new IdImpl("99"), new CoordImpl(-100, -100)));
			Assert.fail("expected IllegalArgumentException, but it did not happen.");
		} catch (IllegalArgumentException expected) {
			log.info("catched expected exception: " + expected.getMessage());
		}
	}

	@Test
	public void testGetSubRoute_wrongEnd() {
		NetworkLayer network = createTestNetwork();
		Id id1 = new IdImpl("1");
		Id id14 = new IdImpl("14");
		Id id15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(id1, id14, network);
		route.setLinkIds(id1, NetworkUtils.getLinkIds("22 12 -23 3 24"), id14);

		try {
			route.getSubRoute(id1, id15);
			Assert.fail("expected IllegalArgumentException, but it did not happen.");
		} catch (IllegalArgumentException expected) {
			log.info("catched expected exception: " + expected.getMessage());
		}
	}

	@Test
	public void testGetSubRoute_sameNodes_old() {
		NetworkLayer network = createTestNetwork();
		Id link0 = new IdImpl("0");
		Id link15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		route.setLinkIds(link0, NetworkUtils.getLinkIds("1 22 12 -23 3 24 14"), link15);

		Node node = network.getNodes().get(new IdImpl("3"));
		NetworkRouteWRefs subRoute = route.getSubRoute(node, node);
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", network.getLinks().get(new IdImpl("-23")).getId(), subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", network.getLinks().get(new IdImpl("3")).getId(), subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_sameLinks() {
		NetworkLayer network = createTestNetwork();
		Id id1 = new IdImpl("1");
		Id id12 = new IdImpl("12");
		Id id14 = new IdImpl("14");
		NetworkRouteWRefs route = getNetworkRouteInstance(id1, id14, network);
		route.setLinkIds(id1, NetworkUtils.getLinkIds("22 12 -23 3 24"), id14);

		NetworkRouteWRefs subRoute = route.getSubRoute(id12, id12);
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", id12, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id12, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_sameNodesInOneNodeRoute_old() {
		NetworkLayer network = createTestNetwork();
		Id link11 = new IdImpl("11");
		Id link12 = new IdImpl("12");
		NetworkRouteWRefs route = getNetworkRouteInstance(link11, link12, network);
		route.setLinkIds(link11, NetworkUtils.getLinkIds(""), link12);

		Node node = network.getNodes().get(new IdImpl("12"));
		NetworkRouteWRefs subRoute = route.getSubRoute(node, node);
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", network.getLinks().get(new IdImpl("11")).getId(), subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", network.getLinks().get(new IdImpl("12")).getId(), subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_sameLinks_emptyRoute1() {
		NetworkLayer network = createTestNetwork();
		Id id1 = new IdImpl("1");
		NetworkRouteWRefs route = getNetworkRouteInstance(id1, id1, network);
		route.setLinkIds(id1, null, id1);

		NetworkRouteWRefs subRoute = route.getSubRoute(id1, id1);
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", id1, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id1, subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_sameLinks_emptyRoute2() {
		NetworkLayer network = createTestNetwork();
		Id id1 = new IdImpl("1");
		Id id2 = new IdImpl("2");
		NetworkRouteWRefs route = getNetworkRouteInstance(id1, id2, network);
		route.setLinkIds(id1, null, id2);

		NetworkRouteWRefs subRoute = route.getSubRoute(id1, id1);
		List<Id> linkIds = subRoute.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds.size());
		Assert.assertEquals("wrong start link.", id1, subRoute.getStartLinkId());
		Assert.assertEquals("wrong end link.", id1, subRoute.getEndLinkId());

		NetworkRouteWRefs subRoute2 = route.getSubRoute(id2, id2);
		List<Id> linkIds2 = subRoute2.getLinkIds();
		Assert.assertEquals("number of links in subRoute.", 0, linkIds2.size());
		Assert.assertEquals("wrong start link.", id2, subRoute2.getStartLinkId());
		Assert.assertEquals("wrong end link.", id2, subRoute2.getEndLinkId());
	}

	@Test
	public void testStartAndEndOnSameLinks_setLinks() {
		NetworkLayer network = createTestNetwork();
		Id link = new IdImpl("3");
		NetworkRouteWRefs route = getNetworkRouteInstance(link, link, network);
		route.setLinkIds(link, new ArrayList<Id>(0), link);
		Assert.assertEquals(0, route.getLinkIds().size());
	}

	@Test
	public void testStartAndEndOnSubsequentLinks_setLinks() {
		NetworkLayer network = createTestNetwork();
		final Id link13 = new IdImpl("13");
		final Id link14 = new IdImpl("14");
		NetworkRouteWRefs route = getNetworkRouteInstance(link13, link14, network);
		route.setLinkIds(link13, new ArrayList<Id>(0), link14);
		Assert.assertEquals(0, route.getLinkIds().size());
	}

	@Test
	public void testVehicleId() {
		NetworkLayer network = createTestNetwork();
		Id link0 = new IdImpl("0");
		Id link15 = new IdImpl("15");
		NetworkRouteWRefs route = getNetworkRouteInstance(link0, link15, network);
		Assert.assertNull(route.getVehicleId());
		Id id = new IdImpl("8134");
		route.setVehicleId(id);
		Assert.assertEquals(id, route.getVehicleId());
		route.setVehicleId(null);
		Assert.assertNull(route.getVehicleId());
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
		Node node0 = network.createAndAddNode(new IdImpl("0"), new CoordImpl(   0, 500));
		Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(   0, 0));
		Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(1000, 0));
		Node node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(2000, 0));
		Node node4 = network.createAndAddNode(new IdImpl("4"), new CoordImpl(3000, 0));
		Node node5 = network.createAndAddNode(new IdImpl("5"), new CoordImpl(4000, 0));
		Node node11 = network.createAndAddNode(new IdImpl("11"), new CoordImpl(   0, 1000));
		Node node12 = network.createAndAddNode(new IdImpl("12"), new CoordImpl(1000, 1000));
		Node node13 = network.createAndAddNode(new IdImpl("13"), new CoordImpl(2000, 1000));
		Node node14 = network.createAndAddNode(new IdImpl("14"), new CoordImpl(3000, 1000));
		Node node15 = network.createAndAddNode(new IdImpl("15"), new CoordImpl(4000, 1000));
		Node node16 = network.createAndAddNode(new IdImpl("16"), new CoordImpl(5000, 1000));

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

		return network;
	}

}
