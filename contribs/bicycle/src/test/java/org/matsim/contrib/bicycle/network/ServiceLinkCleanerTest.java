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

import java.util.Set;

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


	/**
	 * Why both pipelines have to prune mode-less links <em>before</em> running this
	 * cleaner.
	 *
	 * <p>The access rules only empty a dropped link's modes — the link stays in the
	 * graph, keeps its {@code type=highway.service} and still joins its two nodes. This
	 * cleaner knows nothing about modes, so a dropped driveway in the middle of a service
	 * chain still ties the two halves together and saves both. Once the empty link is
	 * pruned afterwards, what is left are two dangling stubs that nothing revisits.
	 *
	 * <p>Not a defect of this class: deciding what is in the graph is the caller's job.
	 * The test pins the hazard so the ordering in both pipelines has a reason on record.
	 */
	@Test
	void anEmptiedLinkStillHoldsItsServiceComponentTogether() {

		Network withEmptied = serviceChainBetweenTwoRoads();
		withEmptied.getLinks().get(Id.createLinkId("x->y")).setAllowedModes(Set.of());

		assertEquals(0, new ServiceLinkCleaner().run(withEmptied),
			"the emptied link still docks at two roads, so the whole chain looks like a shortcut");

		Network pruned = serviceChainBetweenTwoRoads();
		pruned.removeLink(Id.createLinkId("x->y"));

		assertEquals(2, new ServiceLinkCleaner().run(pruned),
			"pruned first, the chain falls into two one-docking-node stubs and both go");
	}

	// =========================================================================
	// helpers
	// =========================================================================

	/** road a->b — service b->x->y->c — road c->d, i.e. a service chain with two docks. */
	private static Network serviceChainBetweenTwoRoads() {
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 100, 0);
		Node x = node(net, "x", 200, 0);
		Node y = node(net, "y", 300, 0);
		Node c = node(net, "c", 400, 0);
		Node d = node(net, "d", 500, 0);
		link(net, a, b, ROAD);
		link(net, b, x, SERVICE);
		link(net, x, y, SERVICE);
		link(net, y, c, SERVICE);
		link(net, c, d, ROAD);
		return net;
	}

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
