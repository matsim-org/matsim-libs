/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class RouteUtilsTest {
	
	@Test
	public void testCalculateCoverage() {
		Fixture f = new Fixture();
		f.network.getLinks().get(f.linkIds[0]).setLength(100.0);
		f.network.getLinks().get(f.linkIds[1]).setLength(200.0);
		f.network.getLinks().get(f.linkIds[2]).setLength(300.0);
		f.network.getLinks().get(f.linkIds[3]).setLength(400.0);
		f.network.getLinks().get(f.linkIds[4]).setLength(500.0);
		f.network.getLinks().get(f.linkIds[5]).setLength(600.0);
		
		NetworkRoute route, route2, route3 ;
		{
			Link startLink = f.network.getLinks().get(f.linkIds[0]);
			Link endLink = f.network.getLinks().get(f.linkIds[5]);
			List<Id<Link>> linkIds = new ArrayList<Id<Link>>(4);
			Collections.addAll(linkIds, f.linkIds[1], f.linkIds[2], f.linkIds[3], f.linkIds[4]);
			route = new LinkNetworkRouteImpl(startLink.getId(), endLink.getId());
			route.setLinkIds(startLink.getId(), linkIds, endLink.getId());
		}
		{
			Link startLink = f.network.getLinks().get(f.linkIds[0]);
			Link endLink = f.network.getLinks().get(f.linkIds[5]);
			List<Id<Link>> linkIds = new ArrayList<Id<Link>>(4);
			Collections.addAll(linkIds, f.linkIds[1], f.linkIds[2], f.linkIds[3], f.linkIds[4]);
			route2 = new LinkNetworkRouteImpl(startLink.getId(), endLink.getId());
			route2.setLinkIds(startLink.getId(), linkIds, endLink.getId());
		}
		{
			Link startLink = f.network.getLinks().get(f.linkIds[0]);
			Link endLink = f.network.getLinks().get(f.linkIds[5]);
			List<Id<Link>> linkIds = new ArrayList<Id<Link>>(3);
			Collections.addAll(linkIds, f.linkIds[1],  f.linkIds[3], f.linkIds[4]);
			// (inconsistent route, but I don't think anything cares at this level here)
			route3 = new LinkNetworkRouteImpl(startLink.getId(), endLink.getId());
			route3.setLinkIds(startLink.getId(), linkIds, endLink.getId());
		}
		
		Assert.assertEquals( 1. , RouteUtils.calculateCoverage( route, route2, f.network ), 0.0001 );
		Assert.assertEquals( (200.+400.+500.)/(200.+300.+400.+500.) , RouteUtils.calculateCoverage( route, route3, f.network ), 0.0001 );
		Assert.assertEquals( 1. , RouteUtils.calculateCoverage( route3, route, f.network ), 0.0001 );
		
	}

	@Test
	public void testGetNodes() {
		Fixture f = new Fixture();
		Link startLink = f.network.getLinks().get(f.linkIds[0]);
		Link endLink = f.network.getLinks().get(f.linkIds[5]);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>(4);
		Collections.addAll(linkIds, f.linkIds[1], f.linkIds[2], f.linkIds[3], f.linkIds[4]);
		NetworkRoute route = new LinkNetworkRouteImpl(startLink.getId(), endLink.getId());
		route.setLinkIds(startLink.getId(), linkIds, endLink.getId());

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(5, nodes.size());
		Assert.assertEquals(f.nodeIds[1], nodes.get(0).getId());
		Assert.assertEquals(f.nodeIds[2], nodes.get(1).getId());
		Assert.assertEquals(f.nodeIds[3], nodes.get(2).getId());
		Assert.assertEquals(f.nodeIds[4], nodes.get(3).getId());
		Assert.assertEquals(f.nodeIds[5], nodes.get(4).getId());
	}

	@Test
	public void testGetNodes_SameStartEndLink() {
		Fixture f = new Fixture();
		Link startLink = f.network.getLinks().get(f.linkIds[2]);
		Link endLink = f.network.getLinks().get(f.linkIds[2]);
		List<Id<Link>> links = new ArrayList<Id<Link>>(0);
		NetworkRoute route = new LinkNetworkRouteImpl(startLink.getId(), endLink.getId());
		route.setLinkIds(startLink.getId(), links, endLink.getId());

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(0, nodes.size());
	}

	@Test
	public void testGetNodes_NoLinksBetween() {
		Fixture f = new Fixture();
		Id<Link> startLinkId = f.linkIds[3];
		Id<Link> endLinkId = f.linkIds[4];
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>(0);
		NetworkRoute route = new LinkNetworkRouteImpl(startLinkId, endLinkId);
		route.setLinkIds(startLinkId, linkIds, endLinkId);

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(1, nodes.size());
		Assert.assertEquals(f.nodeIds[4], nodes.get(0).getId());
	}

	@Test
	public void testGetNodes_CircularRoute() {
		Fixture f = new Fixture();
		Id<Link> id99 = Id.create("99", Link.class);
		f.network.addLink(f.network.getFactory().createLink(id99, f.network.getNodes().get(f.nodeIds[6]), f.network.getNodes().get(f.nodeIds[0])));
		Link startLink = f.network.getLinks().get(f.linkIds[3]);
		Link endLink = f.network.getLinks().get(f.linkIds[3]);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>(6);
		Collections.addAll(linkIds, f.linkIds[4], f.linkIds[5], id99, f.linkIds[0], f.linkIds[1], f.linkIds[2]);

		NetworkRoute route = new LinkNetworkRouteImpl(startLink.getId(), endLink.getId());
		route.setLinkIds(startLink.getId(), linkIds, endLink.getId());

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(7, nodes.size());
		Assert.assertEquals(f.nodeIds[4], nodes.get(0).getId());
		Assert.assertEquals(f.nodeIds[5], nodes.get(1).getId());
		Assert.assertEquals(f.nodeIds[6], nodes.get(2).getId());
		Assert.assertEquals(f.nodeIds[0], nodes.get(3).getId());
		Assert.assertEquals(f.nodeIds[1], nodes.get(4).getId());
		Assert.assertEquals(f.nodeIds[2], nodes.get(5).getId());
		Assert.assertEquals(f.nodeIds[3], nodes.get(6).getId());
	}

	@Test
	public void testGetLinksFromNodes() {
		Fixture f = new Fixture();
		ArrayList<Node> nodes = new ArrayList<Node>();
		List<Link> links = RouteUtils.getLinksFromNodes(nodes);

		Assert.assertEquals(0, links.size());

		nodes.add(f.network.getNodes().get(f.nodeIds[3]));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(0, links.size());

		nodes.add(f.network.getNodes().get(f.nodeIds[4]));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(1, links.size());
		Assert.assertEquals(f.linkIds[3], links.get(0).getId());

		nodes.add(f.network.getNodes().get(f.nodeIds[5]));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(2, links.size());
		Assert.assertEquals(f.linkIds[3], links.get(0).getId());
		Assert.assertEquals(f.linkIds[4], links.get(1).getId());

		nodes.add(0, f.network.getNodes().get(f.nodeIds[2]));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(3, links.size());
		Assert.assertEquals(f.linkIds[2], links.get(0).getId());
		Assert.assertEquals(f.linkIds[3], links.get(1).getId());
		Assert.assertEquals(f.linkIds[4], links.get(2).getId());
	}

	@Test
	public void testGetSubRoute() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.linkIds[0], f.linkIds[5]);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		Collections.addAll(linkIds, f.linkIds[1], f.linkIds[2], f.linkIds[3], f.linkIds[4]);
		route.setLinkIds(f.linkIds[0], linkIds, f.linkIds[5]);

		NetworkRoute subRoute = RouteUtils.getSubRoute(route, f.network.getNodes().get(f.nodeIds[3]), f.network.getNodes().get(f.nodeIds[5]), f.network);
		Assert.assertEquals(2, subRoute.getLinkIds().size());
		Assert.assertEquals(f.linkIds[2], subRoute.getStartLinkId());
		Assert.assertEquals(f.linkIds[3], subRoute.getLinkIds().get(0));
		Assert.assertEquals(f.linkIds[4], subRoute.getLinkIds().get(1));
		Assert.assertEquals(f.linkIds[5], subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_fullRoute() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.linkIds[0], f.linkIds[5]);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		Collections.addAll(linkIds, f.linkIds[1], f.linkIds[2], f.linkIds[3], f.linkIds[4]);
		route.setLinkIds(f.linkIds[0], linkIds, f.linkIds[5]);

		NetworkRoute subRoute = RouteUtils.getSubRoute(route, f.network.getNodes().get(f.nodeIds[1]), f.network.getNodes().get(f.nodeIds[5]), f.network);
		Assert.assertEquals(4, subRoute.getLinkIds().size());
		Assert.assertEquals(f.linkIds[0], subRoute.getStartLinkId());
		Assert.assertEquals(f.linkIds[1], subRoute.getLinkIds().get(0));
		Assert.assertEquals(f.linkIds[2], subRoute.getLinkIds().get(1));
		Assert.assertEquals(f.linkIds[3], subRoute.getLinkIds().get(2));
		Assert.assertEquals(f.linkIds[4], subRoute.getLinkIds().get(3));
		Assert.assertEquals(f.linkIds[5], subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_emptySubRoute() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.linkIds[0], f.linkIds[5]);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		Collections.addAll(linkIds, f.linkIds[1], f.linkIds[2], f.linkIds[3], f.linkIds[4]);
		route.setLinkIds(f.linkIds[0], linkIds, f.linkIds[5]);

		NetworkRoute subRoute = RouteUtils.getSubRoute(route, f.network.getNodes().get(f.nodeIds[4]), f.network.getNodes().get(f.nodeIds[4]), f.network);
		Assert.assertEquals(0, subRoute.getLinkIds().size());
		Assert.assertEquals(f.linkIds[3], subRoute.getStartLinkId());
		Assert.assertEquals(f.linkIds[4], subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_sameStartEnd() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.linkIds[0], f.linkIds[5]);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		Collections.addAll(linkIds, f.linkIds[1], f.linkIds[2], f.linkIds[3], f.linkIds[4]);
		route.setLinkIds(f.linkIds[0], linkIds, f.linkIds[5]);

		NetworkRoute subRoute = RouteUtils.getSubRoute(route, f.network.getNodes().get(f.nodeIds[5]), f.network.getNodes().get(f.nodeIds[4]), f.network);
		Assert.assertEquals(0, subRoute.getLinkIds().size());
		Assert.assertEquals(f.linkIds[4], subRoute.getStartLinkId());
		Assert.assertEquals(f.linkIds[4], subRoute.getEndLinkId());
	}

	@Test
	public void testCalcDistance() {
		Fixture f = new Fixture();
		f.network.getLinks().get(f.linkIds[0]).setLength(100.0);
		f.network.getLinks().get(f.linkIds[1]).setLength(200.0);
		f.network.getLinks().get(f.linkIds[2]).setLength(300.0);
		f.network.getLinks().get(f.linkIds[3]).setLength(400.0);
		f.network.getLinks().get(f.linkIds[4]).setLength(500.0);
		f.network.getLinks().get(f.linkIds[5]).setLength(600.0);
		NetworkRoute route = new LinkNetworkRouteImpl(f.linkIds[0], f.linkIds[5]);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		Collections.addAll(linkIds, f.linkIds[1], f.linkIds[2], f.linkIds[3]);
		route.setLinkIds(f.linkIds[0], linkIds, f.linkIds[4]);
		Assert.assertEquals(900.0, RouteUtils.calcDistanceExcludingStartEndLink(route, f.network), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1400.0, RouteUtils.calcDistance(route, 1.0, 1.0, f.network), MatsimTestUtils.EPSILON);
		// modify the route
		linkIds.add(f.linkIds[4]);
		route.setLinkIds(f.linkIds[0], linkIds, f.linkIds[5]);
		Assert.assertEquals(1400.0, RouteUtils.calcDistanceExcludingStartEndLink(route, f.network), MatsimTestUtils.EPSILON);
		Assert.assertEquals(2000.0, RouteUtils.calcDistance(route, 1.0, 1.0, f.network), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testCalcDistance_sameStartEndRoute() {
		Fixture f = new Fixture();
		f.network.getLinks().get(f.linkIds[0]).setLength(100.0);
		f.network.getLinks().get(f.linkIds[1]).setLength(200.0);
		f.network.getLinks().get(f.linkIds[2]).setLength(300.0);
		f.network.getLinks().get(f.linkIds[3]).setLength(400.0);
		f.network.getLinks().get(f.linkIds[4]).setLength(500.0);
		f.network.getLinks().get(f.linkIds[5]).setLength(600.0);
		NetworkRoute route = new LinkNetworkRouteImpl(f.linkIds[3], f.linkIds[3]);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		route.setLinkIds(f.linkIds[3], linkIds, f.linkIds[3]);
		Assert.assertEquals(0.0, RouteUtils.calcDistanceExcludingStartEndLink(route, f.network), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0.0, RouteUtils.calcDistance(route, 1.0, 1.0, f.network), MatsimTestUtils.EPSILON);
		Assert.assertEquals(400.0, RouteUtils.calcDistance(route, 0.0, 1.0, f.network), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testCalcDistance_subsequentStartEndRoute() {
		Fixture f = new Fixture();
		f.network.getLinks().get(f.linkIds[0]).setLength(100.0);
		f.network.getLinks().get(f.linkIds[1]).setLength(200.0);
		f.network.getLinks().get(f.linkIds[2]).setLength(300.0);
		f.network.getLinks().get(f.linkIds[3]).setLength(400.0);
		f.network.getLinks().get(f.linkIds[4]).setLength(500.0);
		f.network.getLinks().get(f.linkIds[5]).setLength(600.0);
		NetworkRoute route = new LinkNetworkRouteImpl(f.linkIds[2], f.linkIds[3]);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		route.setLinkIds(f.linkIds[2], linkIds, f.linkIds[3]);
		Assert.assertEquals(0.0, RouteUtils.calcDistanceExcludingStartEndLink(route, f.network), MatsimTestUtils.EPSILON);
		Assert.assertEquals(400.0, RouteUtils.calcDistance(route, 1.0, 1.0, f.network), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testCalcDistance_oneLinkRoute() {
		Fixture f = new Fixture();
		f.network.getLinks().get(f.linkIds[0]).setLength(100.0);
		f.network.getLinks().get(f.linkIds[1]).setLength(200.0);
		f.network.getLinks().get(f.linkIds[2]).setLength(300.0);
		f.network.getLinks().get(f.linkIds[3]).setLength(400.0);
		f.network.getLinks().get(f.linkIds[4]).setLength(500.0);
		f.network.getLinks().get(f.linkIds[5]).setLength(600.0);
		NetworkRoute route = new LinkNetworkRouteImpl(f.linkIds[2], f.linkIds[4]);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		linkIds.add(f.linkIds[3]);
		route.setLinkIds(f.linkIds[2], linkIds, f.linkIds[4]);
		Assert.assertEquals(400.0, RouteUtils.calcDistanceExcludingStartEndLink(route, f.network), MatsimTestUtils.EPSILON);
		Assert.assertEquals(900.0, RouteUtils.calcDistance(route, 1.0, 1.0, f.network), MatsimTestUtils.EPSILON);
	}

	private static class Fixture {
		protected final Scenario scenario;
		protected final Network network;
		protected final Id<Link>[] linkIds;
		protected final Id<Node>[] nodeIds;

		protected Fixture() {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.network = this.scenario.getNetwork();
			NetworkFactory nf = this.network.getFactory();

			this.linkIds = new Id[6];
			for (int i = 0; i < this.linkIds.length; i++) {
				this.linkIds[i] = Id.create(i, Link.class);
			}

			this.nodeIds = new Id[7];
			for (int i = 0; i < this.nodeIds.length; i++) {
				this.nodeIds[i] = Id.create(i, Node.class);
			}

			this.network.addNode(nf.createNode(this.nodeIds[0], new Coord((double) 0, (double) 0)));
			this.network.addNode(nf.createNode(this.nodeIds[1], new Coord((double) 100, (double) 0)));
			this.network.addNode(nf.createNode(this.nodeIds[2], new Coord((double) 200, (double) 0)));
			this.network.addNode(nf.createNode(this.nodeIds[3], new Coord((double) 300, (double) 0)));
			this.network.addNode(nf.createNode(this.nodeIds[4], new Coord((double) 400, (double) 0)));
			this.network.addNode(nf.createNode(this.nodeIds[5], new Coord((double) 500, (double) 0)));
			this.network.addNode(nf.createNode(this.nodeIds[6], new Coord((double) 600, (double) 0)));
			this.network.addLink(nf.createLink(this.linkIds[0], this.network.getNodes().get(this.nodeIds[0]), this.network.getNodes().get(this.nodeIds[1])));
			this.network.addLink(nf.createLink(this.linkIds[1], this.network.getNodes().get(this.nodeIds[1]), this.network.getNodes().get(this.nodeIds[2])));
			this.network.addLink(nf.createLink(this.linkIds[2], this.network.getNodes().get(this.nodeIds[2]), this.network.getNodes().get(this.nodeIds[3])));
			this.network.addLink(nf.createLink(this.linkIds[3], this.network.getNodes().get(this.nodeIds[3]), this.network.getNodes().get(this.nodeIds[4])));
			this.network.addLink(nf.createLink(this.linkIds[4], this.network.getNodes().get(this.nodeIds[4]), this.network.getNodes().get(this.nodeIds[5])));
			this.network.addLink(nf.createLink(this.linkIds[5], this.network.getNodes().get(this.nodeIds[5]), this.network.getNodes().get(this.nodeIds[6])));
		}
	}

}
