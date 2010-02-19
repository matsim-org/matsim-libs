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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

public class RouteUtilsTest {

	@Test
	public void testGetNodes() {
		Fixture f = new Fixture();
		Link startLink = f.network.getLinks().get(f.ids[0]);
		Link endLink = f.network.getLinks().get(f.ids[5]);
		List<Id> linkIds = new ArrayList<Id>(4);
		Collections.addAll(linkIds, f.ids[1], f.ids[2], f.ids[3], f.ids[4]);
		NetworkRoute route = new LinkNetworkRouteImpl(startLink.getId(), endLink.getId(), f.network);
		route.setLinkIds(startLink.getId(), linkIds, endLink.getId());

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(5, nodes.size());
		Assert.assertEquals(f.ids[1], nodes.get(0).getId());
		Assert.assertEquals(f.ids[2], nodes.get(1).getId());
		Assert.assertEquals(f.ids[3], nodes.get(2).getId());
		Assert.assertEquals(f.ids[4], nodes.get(3).getId());
		Assert.assertEquals(f.ids[5], nodes.get(4).getId());
	}

	@Test
	public void testGetNodes_SameStartEndLink() {
		Fixture f = new Fixture();
		Link startLink = f.network.getLinks().get(f.ids[2]);
		Link endLink = f.network.getLinks().get(f.ids[2]);
		List<Id> links = new ArrayList<Id>(0);
		NetworkRoute route = new LinkNetworkRouteImpl(startLink.getId(), endLink.getId(), f.network);
		route.setLinkIds(startLink.getId(), links, endLink.getId());

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(0, nodes.size());
	}

	@Test
	public void testGetNodes_NoLinksBetween() {
		Fixture f = new Fixture();
		Id startLinkId = f.ids[3];
		Id endLinkId = f.ids[4];
		List<Id> linkIds = new ArrayList<Id>(0);
		NetworkRoute route = new LinkNetworkRouteImpl(startLinkId, endLinkId, f.network);
		route.setLinkIds(startLinkId, linkIds, endLinkId);

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(1, nodes.size());
		Assert.assertEquals(f.ids[4], nodes.get(0).getId());
	}

	@Test
	public void testGetNodes_CircularRoute() {
		Fixture f = new Fixture();
		Id id99 = f.scenario.createId("99");
		f.network.addLink(f.network.getFactory().createLink(id99, f.ids[6], f.ids[0]));
		Link startLink = f.network.getLinks().get(f.ids[3]);
		Link endLink = f.network.getLinks().get(f.ids[3]);
		List<Id> linkIds = new ArrayList<Id>(6);
		Collections.addAll(linkIds, f.ids[4], f.ids[5], id99, f.ids[0], f.ids[1], f.ids[2]);

		NetworkRoute route = new LinkNetworkRouteImpl(startLink.getId(), endLink.getId(), f.network);
		route.setLinkIds(startLink.getId(), linkIds, endLink.getId());

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(7, nodes.size());
		Assert.assertEquals(f.ids[4], nodes.get(0).getId());
		Assert.assertEquals(f.ids[5], nodes.get(1).getId());
		Assert.assertEquals(f.ids[6], nodes.get(2).getId());
		Assert.assertEquals(f.ids[0], nodes.get(3).getId());
		Assert.assertEquals(f.ids[1], nodes.get(4).getId());
		Assert.assertEquals(f.ids[2], nodes.get(5).getId());
		Assert.assertEquals(f.ids[3], nodes.get(6).getId());
	}

	@Test
	public void testGetLinksFromNodes() {
		Fixture f = new Fixture();
		ArrayList<Node> nodes = new ArrayList<Node>();
		List<Link> links = RouteUtils.getLinksFromNodes(nodes);

		Assert.assertEquals(0, links.size());

		nodes.add(f.network.getNodes().get(f.ids[3]));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(0, links.size());

		nodes.add(f.network.getNodes().get(f.ids[4]));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(1, links.size());
		Assert.assertEquals(f.ids[3], links.get(0).getId());

		nodes.add(f.network.getNodes().get(f.ids[5]));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(2, links.size());
		Assert.assertEquals(f.ids[3], links.get(0).getId());
		Assert.assertEquals(f.ids[4], links.get(1).getId());

		nodes.add(0, f.network.getNodes().get(f.ids[2]));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(3, links.size());
		Assert.assertEquals(f.ids[2], links.get(0).getId());
		Assert.assertEquals(f.ids[3], links.get(1).getId());
		Assert.assertEquals(f.ids[4], links.get(2).getId());
	}

	@Test
	public void testGetSubRoute() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.ids[0], f.ids[5], f.network);
		List<Id> linkIds = new ArrayList<Id>();
		Collections.addAll(linkIds, f.ids[1], f.ids[2], f.ids[3], f.ids[4]);
		route.setLinkIds(f.ids[0], linkIds, f.ids[5]);

		NetworkRoute subRoute = RouteUtils.getSubRoute(route, f.network.getNodes().get(f.ids[3]), f.network.getNodes().get(f.ids[5]), f.network);
		Assert.assertEquals(2, subRoute.getLinkIds().size());
		Assert.assertEquals(f.ids[2], subRoute.getStartLinkId());
		Assert.assertEquals(f.ids[3], subRoute.getLinkIds().get(0));
		Assert.assertEquals(f.ids[4], subRoute.getLinkIds().get(1));
		Assert.assertEquals(f.ids[5], subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_fullRoute() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.ids[0], f.ids[5], f.network);
		List<Id> linkIds = new ArrayList<Id>();
		Collections.addAll(linkIds, f.ids[1], f.ids[2], f.ids[3], f.ids[4]);
		route.setLinkIds(f.ids[0], linkIds, f.ids[5]);

		NetworkRoute subRoute = RouteUtils.getSubRoute(route, f.network.getNodes().get(f.ids[1]), f.network.getNodes().get(f.ids[5]), f.network);
		Assert.assertEquals(4, subRoute.getLinkIds().size());
		Assert.assertEquals(f.ids[0], subRoute.getStartLinkId());
		Assert.assertEquals(f.ids[1], subRoute.getLinkIds().get(0));
		Assert.assertEquals(f.ids[2], subRoute.getLinkIds().get(1));
		Assert.assertEquals(f.ids[3], subRoute.getLinkIds().get(2));
		Assert.assertEquals(f.ids[4], subRoute.getLinkIds().get(3));
		Assert.assertEquals(f.ids[5], subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_emptySubRoute() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.ids[0], f.ids[5], f.network);
		List<Id> linkIds = new ArrayList<Id>();
		Collections.addAll(linkIds, f.ids[1], f.ids[2], f.ids[3], f.ids[4]);
		route.setLinkIds(f.ids[0], linkIds, f.ids[5]);

		NetworkRoute subRoute = RouteUtils.getSubRoute(route, f.network.getNodes().get(f.ids[4]), f.network.getNodes().get(f.ids[4]), f.network);
		Assert.assertEquals(0, subRoute.getLinkIds().size());
		Assert.assertEquals(f.ids[3], subRoute.getStartLinkId());
		Assert.assertEquals(f.ids[4], subRoute.getEndLinkId());
	}

	@Test
	public void testGetSubRoute_sameStartEnd() {
		Fixture f = new Fixture();
		NetworkRoute route = new LinkNetworkRouteImpl(f.ids[0], f.ids[5], f.network);
		List<Id> linkIds = new ArrayList<Id>();
		Collections.addAll(linkIds, f.ids[1], f.ids[2], f.ids[3], f.ids[4]);
		route.setLinkIds(f.ids[0], linkIds, f.ids[5]);

		NetworkRoute subRoute = RouteUtils.getSubRoute(route, f.network.getNodes().get(f.ids[5]), f.network.getNodes().get(f.ids[4]), f.network);
		Assert.assertEquals(0, subRoute.getLinkIds().size());
		Assert.assertEquals(f.ids[4], subRoute.getStartLinkId());
		Assert.assertEquals(f.ids[4], subRoute.getEndLinkId());
	}

	private static class Fixture {
		protected final Scenario scenario;
		protected final Network network;
		protected final Id[] ids;

		protected Fixture() {
			this.scenario = new ScenarioImpl();
			this.network = this.scenario.getNetwork();
			NetworkFactory nf = this.network.getFactory();

			this.ids = new Id[7];
			for (int i = 0; i < this.ids.length; i++) {
				this.ids[i] = this.scenario.createId(Integer.toString(i));
			}

			this.network.addNode(nf.createNode(this.ids[0], this.scenario.createCoord(0, 0)));
			this.network.addNode(nf.createNode(this.ids[1], this.scenario.createCoord(100, 0)));
			this.network.addNode(nf.createNode(this.ids[2], this.scenario.createCoord(200, 0)));
			this.network.addNode(nf.createNode(this.ids[3], this.scenario.createCoord(300, 0)));
			this.network.addNode(nf.createNode(this.ids[4], this.scenario.createCoord(400, 0)));
			this.network.addNode(nf.createNode(this.ids[5], this.scenario.createCoord(500, 0)));
			this.network.addNode(nf.createNode(this.ids[6], this.scenario.createCoord(600, 0)));
			this.network.addLink(nf.createLink(this.ids[0], this.ids[0], this.ids[1]));
			this.network.addLink(nf.createLink(this.ids[1], this.ids[1], this.ids[2]));
			this.network.addLink(nf.createLink(this.ids[2], this.ids[2], this.ids[3]));
			this.network.addLink(nf.createLink(this.ids[3], this.ids[3], this.ids[4]));
			this.network.addLink(nf.createLink(this.ids[4], this.ids[4], this.ids[5]));
			this.network.addLink(nf.createLink(this.ids[5], this.ids[5], this.ids[6]));
		}
	}

}
