/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkSimplifierThresholdTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.network.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class NetworkSimplifierTest {

	@BeforeEach
	public void setUp() {
		Id.resetCaches();
	}

	@Test
	void testBuildNetwork() {
		Network network = buildNetwork();
		assertEquals(6, network.getNodes().size(), "Wrong number of nodes.");
		assertEquals(5, network.getLinks().size(), "Wrong number of links.");
	}

	@Test
	void testRun() {
		Network network = buildNetwork();

		NetworkSimplifier nst = new NetworkSimplifier();
		nst.run(network, 20.0);
		assertEquals(3, network.getLinks().size(), "Wrong number of links");
		assertNotNull(network.getLinks().get(Id.createLinkId("AB-BC")), "Expected link not found.");
		assertNotNull(network.getLinks().get(Id.createLinkId("CD")), "Expected link not found.");
		assertNotNull(network.getLinks().get(Id.createLinkId("DE-EF")), "Expected link not found.");
	}


	@Test
	void testRunMergeLinkStats() {
		Network network = buildNetwork();

		NetworkSimplifier nst = new NetworkSimplifier();
		nst.setMergeLinkStats(true);
		nst.run(network, 20.0);
		assertEquals(2, network.getLinks().size(), "Wrong number of links");
		assertNotNull(network.getLinks().get(Id.createLinkId("AB-BC")), "Expected link not found.");
		assertNotNull(network.getLinks().get(Id.createLinkId("CD-DE-EF")), "Expected link not found.");

		network = buildNetwork();
		nst.run(network, 40.0);
		assertEquals(1, network.getLinks().size(), "Wrong number of links");
		assertNotNull(network.getLinks().get(Id.createLinkId("AB-BC-CD-DE-EF")), "Expected link not found.");

		network = buildNetwork();
		nst.run(network, 5.0);
		assertEquals(5, network.getLinks().size(), "Wrong number of links");
	}

	@Test
	void testDifferentAttributesPerDirection() {
		/*
		 Test-Network

		 A                    J
		 |                    |
		 C---D---E---F---G----H
		 |                    |
		 B                    K

		 - between all connected nodes, there are links going fore and back (.e.g AC, CA, BC, CB, CD, DC, ...)
		 - all links are 10m, have 1 lane, freespeed 10m/s
		 - capacities are different: default 1000veh/h, CD, DE, EF, FG, GH are 2000veh/h, in the opposite direction only FE, ED are 2000veh/h

		 originally, the algorithm had a problem and corrupted the network, resulting in a non-routable network
		 */

		Network network = NetworkUtils.createNetwork();
        Node a = NetworkUtils.createAndAddNode(network, Id.create("A", Node.class), new Coord(0.0, 10.0));
		Node b = NetworkUtils.createAndAddNode(network, Id.create("B", Node.class), new Coord(0.0, -10.0));
		Node c = NetworkUtils.createAndAddNode(network, Id.create("C", Node.class), new Coord(0.0, 0.0));
		Node d = NetworkUtils.createAndAddNode(network, Id.create("D", Node.class), new Coord(10.0, 0.0));
		Node e = NetworkUtils.createAndAddNode(network, Id.create("E", Node.class), new Coord(20.0, 0.0));
		Node f = NetworkUtils.createAndAddNode(network, Id.create("F", Node.class), new Coord(30.0, 0.0));
		Node g = NetworkUtils.createAndAddNode(network, Id.create("G", Node.class), new Coord(40.0, 0.0));
		Node h = NetworkUtils.createAndAddNode(network, Id.create("H", Node.class), new Coord(50.0, 0.0));
		Node j = NetworkUtils.createAndAddNode(network, Id.create("J", Node.class), new Coord(50.0, 10.0));
		Node k = NetworkUtils.createAndAddNode(network, Id.create("K", Node.class), new Coord(50.0, -10.0));

		Id<Link> idAC = Id.create("AC", Link.class);
		Id<Link> idCA = Id.create("CA", Link.class);
		Id<Link> idBC = Id.create("BC", Link.class);
		Id<Link> idCB = Id.create("CB", Link.class);
		Id<Link> idCD = Id.create("CD", Link.class);
		Id<Link> idDC = Id.create("DC", Link.class);
		Id<Link> idDE = Id.create("DE", Link.class);
		Id<Link> idED = Id.create("ED", Link.class);
		Id<Link> idEF = Id.create("EF", Link.class);
		Id<Link> idFE = Id.create("FE", Link.class);
		Id<Link> idFG = Id.create("FG", Link.class);
		Id<Link> idGF = Id.create("GF", Link.class);
		Id<Link> idGH = Id.create("GH", Link.class);
		Id<Link> idHG = Id.create("HG", Link.class);
		Id<Link> idHJ = Id.create("HJ", Link.class);
		Id<Link> idJH = Id.create("JH", Link.class);
		Id<Link> idHK = Id.create("HK", Link.class);
		Id<Link> idKH = Id.create("KH", Link.class);

		NetworkUtils.createAndAddLink(network, idAC, a, c, 10.0, 10.0, 1000, 1);
		NetworkUtils.createAndAddLink(network, idCA, c, a, 10.0, 10.0, 1000, 1);
		NetworkUtils.createAndAddLink(network, idBC, b, c, 10.0, 10.0, 1000, 1);
		NetworkUtils.createAndAddLink(network, idCB, c, b, 10.0, 10.0, 1000, 1);
		NetworkUtils.createAndAddLink(network, idCD, c, d, 10.0, 10.0, 2000, 1);
		NetworkUtils.createAndAddLink(network, idDC, d, c, 10.0, 10.0, 1000, 1);
		NetworkUtils.createAndAddLink(network, idDE, d, e, 10.0, 10.0, 2000, 1);
		NetworkUtils.createAndAddLink(network, idED, e, d, 10.0, 10.0, 2000, 1);
		NetworkUtils.createAndAddLink(network, idEF, e, f, 10.0, 10.0, 2000, 1);
		NetworkUtils.createAndAddLink(network, idFE, f, e, 10.0, 10.0, 2000, 1);
		NetworkUtils.createAndAddLink(network, idFG, f, g, 10.0, 10.0, 2000, 1);
		NetworkUtils.createAndAddLink(network, idGF, g, f, 10.0, 10.0, 1000, 1);
		NetworkUtils.createAndAddLink(network, idGH, g, h, 10.0, 10.0, 2000, 1);
		NetworkUtils.createAndAddLink(network, idHG, h, g, 10.0, 10.0, 1000, 1);
		NetworkUtils.createAndAddLink(network, idHJ, h, j, 10.0, 10.0, 1000, 1);
		NetworkUtils.createAndAddLink(network, idJH, j, h, 10.0, 10.0, 1000, 1);
		NetworkUtils.createAndAddLink(network, idHK, h, k, 10.0, 10.0, 1000, 1);
		NetworkUtils.createAndAddLink(network, idKH, k, h, 10.0, 10.0, 1000, 1);

		new NetworkSimplifier().run(network);

		System.out.println("resulting links:");
		for (Link link : network.getLinks().values()) {
			System.out.println(link.getId());
		}

		// expected links: AC, CA, BC, CB, CD-DE-EF-FG-GH, DC, FE-ED, HG-GF, HJ, JH, HK, KH

		Map<Id<Link>, ? extends Link> links = network.getLinks();
		Id<Link> idCDDEEFFGGH = Id.create("CD-DE-EF-FG-GH", Link.class);
		Id<Link> idHGGF = Id.create("HG-GF", Link.class);
		Id<Link> idFEED = Id.create("FE-ED", Link.class);

		Assertions.assertEquals(12, links.size(), "Wrong number of links.");
		Assertions.assertNotNull(links.get(idAC), "Expected link not found.");
		Assertions.assertNotNull(links.get(idCA), "Expected link not found.");
		Assertions.assertNotNull(links.get(idBC), "Expected link not found.");
		Assertions.assertNotNull(links.get(idCB), "Expected link not found.");

		Assertions.assertNotNull(links.get(idHJ), "Expected link not found.");
		Assertions.assertNotNull(links.get(idJH), "Expected link not found.");
		Assertions.assertNotNull(links.get(idHK), "Expected link not found.");
		Assertions.assertNotNull(links.get(idKH), "Expected link not found.");

		Assertions.assertNotNull(links.get(idCDDEEFFGGH), "Expected link not found.");

		Assertions.assertNotNull(links.get(idHGGF), "Expected link not found.");
		Assertions.assertNotNull(links.get(idFEED), "Expected link not found.");
		Assertions.assertNotNull(links.get(idDC), "Expected link not found.");

		Assertions.assertEquals(10.0, links.get(idAC).getLength(), 1e-8);
		Assertions.assertEquals(50.0, links.get(idCDDEEFFGGH).getLength(), 1e-8);
		Assertions.assertEquals(20.0, links.get(idHGGF).getLength(), 1e-8);
		Assertions.assertEquals(20.0, links.get(idFEED).getLength(), 1e-8);

		Assertions.assertEquals(1000.0, links.get(idAC).getCapacity(), 1e-8);
		Assertions.assertEquals(2000.0, links.get(idCDDEEFFGGH).getCapacity(), 1e-8);
		Assertions.assertEquals(2000.0, links.get(idFEED).getCapacity(), 1e-8);
		Assertions.assertEquals(1000.0, links.get(idHGGF).getCapacity(), 1e-8);

		Assertions.assertEquals(10.0, links.get(idAC).getFreespeed(), 1e-8);
		Assertions.assertEquals(10.0, links.get(idCDDEEFFGGH).getFreespeed(), 1e-8);
		Assertions.assertEquals(10.0, links.get(idHGGF).getFreespeed(), 1e-8);
		Assertions.assertEquals(10.0, links.get(idFEED).getFreespeed(), 1e-8);
	}


	/**
	 * Builds a test network like the following diagram.
	 *
	 * A--->B--->C===>D--->E--->F
	 *
	 * with each link having length 10m. Links AB, BC, DE, and EF have one
	 * lanes each, while CD has two lanes. All free-flow speeds are 60km/h.
	 *
	 * @return
	 */
	private Network buildNetwork(){
        Network network = NetworkUtils.createNetwork();
        Node a = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), CoordUtils.createCoord(0.0, 0.0));
		Node b = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), CoordUtils.createCoord(10.0,  0.0));
		Node c = NetworkUtils.createAndAddNode(network, Id.createNodeId("C"), CoordUtils.createCoord(20.0,  0.0));
		Node d = NetworkUtils.createAndAddNode(network, Id.createNodeId("D"), CoordUtils.createCoord(30.0,  0.0));
		Node e = NetworkUtils.createAndAddNode(network, Id.createNodeId("E"), CoordUtils.createCoord(40.0,  0.0));
		Node f = NetworkUtils.createAndAddNode(network, Id.createNodeId("F"), CoordUtils.createCoord(50.0,  0.0));

		NetworkUtils.createAndAddLink(network, Id.createLinkId("AB"), a, b, 10.0, 60.0/3.6, 1000.0, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("BC"), b, c, 10.0, 60.0/3.6, 1000.0, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("CD"), c, d, 10.0, 60.0/3.6, 1000.0, 2);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("DE"), d, e, 10.0, 60.0/3.6, 1000.0, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("EF"), e, f, 10.0, 60.0/3.6, 1000.0, 1);

		return network;
	}

}
