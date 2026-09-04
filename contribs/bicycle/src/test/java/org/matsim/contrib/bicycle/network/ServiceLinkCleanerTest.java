/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.contrib.bicycle.network;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ServiceLinkCleaner}. Small hand-built networks, no file I/O.
 * "Service" is decided by the {@code type} attribute value {@code highway.service}
 * (the {@code highway.} prefix is stripped before comparing), matching what the
 * OSM reader writes.
 *
 * <p>A service-link component is removed entirely when it docks onto the rest of
 * the graph at 0 or 1 nodes (it can't be a shortcut). When it docks at 2+ nodes,
 * only the hair-like dead-end branches are trimmed and the connecting spine is
 * kept.
 *
 * @author smetzler
 */
public class ServiceLinkCleanerTest {

	private static final String SERVICE = "highway.service";
	private static final String ROAD = "highway.residential";

	@Test
	void removesAServiceDeadEnd() {
		// a real road main1->main2 with a service stub hanging off main2
		Network net = NetworkUtils.createNetwork();
		Node main1 = node(net, "main1", 0, 0);
		Node main2 = node(net, "main2", 100, 0);
		Node dead = node(net, "dead", 100, 100);
		link(net, main1, main2, ROAD);
		link(net, main2, dead, SERVICE);

		int removed = new ServiceLinkCleaner().run(net);

		assertEquals(1, removed);
		assertTrue(net.getLinks().containsKey(Id.createLinkId("main1->main2")), "the real road stays");
		assertFalse(net.getLinks().containsKey(Id.createLinkId("main2->dead")), "the service stub is gone");
		assertFalse(net.getNodes().containsKey(Id.createNodeId("dead")), "its orphaned node is gone too");
	}

	@Test
	void keepsAServiceLinkConnectingTwoRoads() {
		// service link a2->b1 connects two separate roads -> a useful shortcut, keep it
		Network net = NetworkUtils.createNetwork();
		Node a1 = node(net, "a1", 0, 0);
		Node a2 = node(net, "a2", 100, 0);
		Node b1 = node(net, "b1", 200, 0);
		Node b2 = node(net, "b2", 300, 0);
		link(net, a1, a2, ROAD);
		link(net, b1, b2, ROAD);
		link(net, a2, b1, SERVICE);

		int removed = new ServiceLinkCleaner().run(net);

		assertEquals(0, removed);
		assertTrue(net.getLinks().containsKey(Id.createLinkId("a2->b1")), "the connecting service link stays");
		assertEquals(3, net.getLinks().size());
	}

	@Test
	void keepsEverythingWhenThereAreNoServiceLinks() {
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 100, 0);
		Node c = node(net, "c", 200, 0);
		link(net, a, b, ROAD);
		link(net, b, c, ROAD);

		int removed = new ServiceLinkCleaner().run(net);

		assertEquals(0, removed);
		assertEquals(2, net.getLinks().size());
	}

	@Test
	void trimsAHairlineTwigButKeepsTheConnectingSpine() {
		// two roads joined by a service spine ra2->rb1, with a service twig
		// dangling off rb1. The twig is trimmed; the spine is kept.
		Network net = NetworkUtils.createNetwork();
		Node ra1 = node(net, "ra1", 0, 0);
		Node ra2 = node(net, "ra2", 100, 0);
		Node rb1 = node(net, "rb1", 200, 0);
		Node rb2 = node(net, "rb2", 300, 0);
		Node twig = node(net, "twig", 200, 100);
		link(net, ra1, ra2, ROAD);      // makes ra2 a docking node
		link(net, rb1, rb2, ROAD);      // makes rb1 a docking node
		link(net, ra2, rb1, SERVICE);   // the connecting spine
		link(net, rb1, twig, SERVICE);  // the dead-end twig

		int removed = new ServiceLinkCleaner().run(net);

		assertEquals(1, removed);
		assertTrue(net.getLinks().containsKey(Id.createLinkId("ra2->rb1")), "the connecting spine stays");
		assertFalse(net.getLinks().containsKey(Id.createLinkId("rb1->twig")), "the dead-end twig is trimmed");
		assertFalse(net.getNodes().containsKey(Id.createNodeId("twig")), "its orphaned node is gone too");
	}


	// =========================================================================
	// helpers
	// =========================================================================

	private static Node node(Network net, String id, double x, double y) {
		Node n = net.getFactory().createNode(Id.createNodeId(id), new Coord(x, y));
		net.addNode(n);
		return n;
	}

	private static void link(Network net, Node from, Node to, String type) {
		Link l = NetworkUtils.createAndAddLink(net, Id.createLinkId(from.getId() + "->" + to.getId()),
			from, to, 100.0, 8.0, 1000.0, 1.0);
		l.getAttributes().putAttribute("type", type);
	}
}
