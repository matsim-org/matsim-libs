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
import org.matsim.core.population.routes.NetworkRouteWRefs;

public class RouteUtilsTest {

	@Test
	public void testGetNodes() {
		Fixture f = new Fixture();
		Link startLink = f.network.getLinks().get(f.scenario.createId("0"));
		Link endLink = f.network.getLinks().get(f.scenario.createId("5"));
		List<Link> links = new ArrayList<Link>(4);
		links.add(f.network.getLinks().get(f.scenario.createId("1")));
		links.add(f.network.getLinks().get(f.scenario.createId("2")));
		links.add(f.network.getLinks().get(f.scenario.createId("3")));
		links.add(f.network.getLinks().get(f.scenario.createId("4")));
		NetworkRouteWRefs route = new LinkNetworkRouteImpl(startLink, endLink);
		route.setLinks(startLink, links, endLink);

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(5, nodes.size());
		Assert.assertEquals(f.scenario.createId("1"), nodes.get(0).getId());
		Assert.assertEquals(f.scenario.createId("2"), nodes.get(1).getId());
		Assert.assertEquals(f.scenario.createId("3"), nodes.get(2).getId());
		Assert.assertEquals(f.scenario.createId("4"), nodes.get(3).getId());
		Assert.assertEquals(f.scenario.createId("5"), nodes.get(4).getId());
	}

	@Test
	public void testGetNodes_SameStartEndLink() {
		Fixture f = new Fixture();
		Link startLink = f.network.getLinks().get(f.scenario.createId("2"));
		Link endLink = f.network.getLinks().get(f.scenario.createId("2"));
		List<Link> links = new ArrayList<Link>(0);
		NetworkRouteWRefs route = new LinkNetworkRouteImpl(startLink, endLink);
		route.setLinks(startLink, links, endLink);

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(0, nodes.size());
	}

	@Test
	public void testGetNodes_NoLinksBetween() {
		Fixture f = new Fixture();
		Link startLink = f.network.getLinks().get(f.scenario.createId("3"));
		Link endLink = f.network.getLinks().get(f.scenario.createId("4"));
		List<Link> links = new ArrayList<Link>(0);
		NetworkRouteWRefs route = new LinkNetworkRouteImpl(startLink, endLink);
		route.setLinks(startLink, links, endLink);

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(1, nodes.size());
		Assert.assertEquals(f.scenario.createId("4"), nodes.get(0).getId());
	}

	@Test
	public void testGetNodes_CircularRoute() {
		Fixture f = new Fixture();
		f.network.addLink(f.network.getFactory().createLink(f.scenario.createId("99"), f.scenario.createId("6"), f.scenario.createId("0")));
		Link startLink = f.network.getLinks().get(f.scenario.createId("3"));
		Link endLink = f.network.getLinks().get(f.scenario.createId("3"));
		List<Link> links = new ArrayList<Link>(6);
		links.add(f.network.getLinks().get(f.scenario.createId("4")));
		links.add(f.network.getLinks().get(f.scenario.createId("5")));
		links.add(f.network.getLinks().get(f.scenario.createId("99")));
		links.add(f.network.getLinks().get(f.scenario.createId("0")));
		links.add(f.network.getLinks().get(f.scenario.createId("1")));
		links.add(f.network.getLinks().get(f.scenario.createId("2")));

		NetworkRouteWRefs route = new LinkNetworkRouteImpl(startLink, endLink);
		route.setLinks(startLink, links, endLink);

		List<Node> nodes = RouteUtils.getNodes(route, f.network);
		Assert.assertEquals(7, nodes.size());
		Assert.assertEquals(f.scenario.createId("4"), nodes.get(0).getId());
		Assert.assertEquals(f.scenario.createId("5"), nodes.get(1).getId());
		Assert.assertEquals(f.scenario.createId("6"), nodes.get(2).getId());
		Assert.assertEquals(f.scenario.createId("0"), nodes.get(3).getId());
		Assert.assertEquals(f.scenario.createId("1"), nodes.get(4).getId());
		Assert.assertEquals(f.scenario.createId("2"), nodes.get(5).getId());
		Assert.assertEquals(f.scenario.createId("3"), nodes.get(6).getId());
	}

	@Test
	public void testGetLinksFromNodes() {
		Fixture f = new Fixture();
		ArrayList<Node> nodes = new ArrayList<Node>();
		List<Link> links = RouteUtils.getLinksFromNodes(nodes);

		Assert.assertEquals(0, links.size());

		nodes.add(f.network.getNodes().get(f.scenario.createId("3")));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(0, links.size());

		nodes.add(f.network.getNodes().get(f.scenario.createId("4")));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(1, links.size());
		Assert.assertEquals(f.scenario.createId("3"), links.get(0).getId());

		nodes.add(f.network.getNodes().get(f.scenario.createId("5")));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(2, links.size());
		Assert.assertEquals(f.scenario.createId("3"), links.get(0).getId());
		Assert.assertEquals(f.scenario.createId("4"), links.get(1).getId());

		nodes.add(0, f.network.getNodes().get(f.scenario.createId("2")));
		links = RouteUtils.getLinksFromNodes(nodes);
		Assert.assertEquals(3, links.size());
		Assert.assertEquals(f.scenario.createId("2"), links.get(0).getId());
		Assert.assertEquals(f.scenario.createId("3"), links.get(1).getId());
		Assert.assertEquals(f.scenario.createId("4"), links.get(2).getId());
	}

	private static class Fixture {
		protected final Scenario scenario;
		protected final Network network;

		protected Fixture() {
			this.scenario = new ScenarioImpl();
			this.network = this.scenario.getNetwork();
			NetworkFactory nf = this.network.getFactory();

			Id[] ids = new Id[7];
			for (int i = 0; i < ids.length; i++) {
				ids[i] = this.scenario.createId(Integer.toString(i));
			}

			this.network.addNode(nf.createNode(ids[0], this.scenario.createCoord(0, 0)));
			this.network.addNode(nf.createNode(ids[1], this.scenario.createCoord(100, 0)));
			this.network.addNode(nf.createNode(ids[2], this.scenario.createCoord(200, 0)));
			this.network.addNode(nf.createNode(ids[3], this.scenario.createCoord(300, 0)));
			this.network.addNode(nf.createNode(ids[4], this.scenario.createCoord(400, 0)));
			this.network.addNode(nf.createNode(ids[5], this.scenario.createCoord(500, 0)));
			this.network.addNode(nf.createNode(ids[6], this.scenario.createCoord(600, 0)));
			this.network.addLink(nf.createLink(ids[0], ids[0], ids[1]));
			this.network.addLink(nf.createLink(ids[1], ids[1], ids[2]));
			this.network.addLink(nf.createLink(ids[2], ids[2], ids[3]));
			this.network.addLink(nf.createLink(ids[3], ids[3], ids[4]));
			this.network.addLink(nf.createLink(ids[4], ids[4], ids[5]));
			this.network.addLink(nf.createLink(ids[5], ids[5], ids[6]));
		}
	}

}
