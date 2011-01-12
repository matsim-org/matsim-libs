/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkUtilsTest.java
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

package org.matsim.core.utils.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.testcases.fakes.FakeNode;

/**
 * @author mrieser
 */
public class NetworkUtilsTest extends MatsimTestCase {

	private final static Logger log = Logger.getLogger(NetworkUtilsTest.class);

	public void testGetNodes_Empty() {
		NetworkImpl network = getTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, "");
		assertEquals(0, nodes.size());

		List<Node> nodes2 = NetworkUtils.getNodes(network, " ");
		assertEquals(0, nodes2.size());

		List<Node> nodes3 = NetworkUtils.getNodes(network, " \t\r\n \t  \t ");
		assertEquals(0, nodes3.size());
	}

	public void testGetNodes_Null() {
		NetworkImpl network = getTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, null);
		assertEquals(0, nodes.size());
	}

	public void testGetNodes_mixedDelimiters() {
		NetworkImpl network = getTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, " 1\t\t2 \n4\t \t5      3 ");
		assertEquals(5, nodes.size());
		assertEquals(network.getNodes().get(new IdImpl(1)), nodes.get(0));
		assertEquals(network.getNodes().get(new IdImpl(2)), nodes.get(1));
		assertEquals(network.getNodes().get(new IdImpl(4)), nodes.get(2));
		assertEquals(network.getNodes().get(new IdImpl(5)), nodes.get(3));
		assertEquals(network.getNodes().get(new IdImpl(3)), nodes.get(4));
	}

	public void testGetNodes_NonExistant() {
		NetworkImpl network = getTestNetwork();
		try {
			NetworkUtils.getNodes(network, "1 3 ab 5");
			fail("expected Exception, but didn't happen.");
		} catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}

	public void testGetLinks_Empty() {
		NetworkImpl network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, "");
		assertEquals(0, links.size());

		List<Link> links2 = NetworkUtils.getLinks(network, " ");
		assertEquals(0, links2.size());

		List<Link> links3 = NetworkUtils.getLinks(network, " \t\r\n \t  \t ");
		assertEquals(0, links3.size());
	}

	public void testGetLinks_StringNull() {
		NetworkImpl network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, (String)null);
		assertEquals(0, links.size());
	}

	public void testGetLinks_mixedDelimiters() {
		NetworkImpl network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, " 1\t\t2 \n4\t \t      3 ");
		assertEquals(4, links.size());
		assertEquals(network.getLinks().get(new IdImpl(1)), links.get(0));
		assertEquals(network.getLinks().get(new IdImpl(2)), links.get(1));
		assertEquals(network.getLinks().get(new IdImpl(4)), links.get(2));
		assertEquals(network.getLinks().get(new IdImpl(3)), links.get(3));
	}

	public void testGetLinks_NonExistant() {
		NetworkImpl network = getTestNetwork();
		try {
			NetworkUtils.getLinks(network, "1 3 ab 4");
			fail("expected Exception, but didn't happen.");
		} catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}

	public void testGetLinksID_ListNull() {
		List<Id> linkIds = NetworkUtils.getLinkIds((List<Link>) null);
		assertEquals(0, linkIds.size());
	}

	public void testGetLinksID_StringNull() {
		List<Id> linkIds = NetworkUtils.getLinkIds((String) null);
		assertEquals(0, linkIds.size());
	}

	public void testGetNumberOfLanesAsInt() {
		assertEquals(3, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(3.2)));
		assertEquals(3, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(3.1)));
		assertEquals(3, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(3.0)));
		assertEquals(2, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(2.9)));
		assertEquals(2, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(2.5)));
		assertEquals(2, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(2.0)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(1.9)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(1.5)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(1.0)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(0.9)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(0.5)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(0.1)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(7*3600, new PseudoLink(0.0)));
	}

	public void testGetBoundingBox() {
		Collection<Node> nodes = new ArrayList<Node>();
		Id id = new IdImpl("dummy");
		nodes.add(new PseudoNode(id, new CoordImpl(100, 100)));
		nodes.add(new PseudoNode(id, new CoordImpl(200, 105)));
		nodes.add(new PseudoNode(id, new CoordImpl(120, 250)));
		nodes.add(new PseudoNode(id, new CoordImpl(150, 300)));
		nodes.add(new PseudoNode(id, new CoordImpl(50, 199)));
		double[] box = NetworkUtils.getBoundingBox(nodes);
		assertEquals(50, box[0], EPSILON); // minX
		assertEquals(100, box[1], EPSILON); // miny
		assertEquals(200, box[2], EPSILON); // maxX
		assertEquals(300, box[3], EPSILON); // maxY
	}

	public void testGetBoundingBox_negativeNodesOnly() {
		Collection<Node> nodes = new ArrayList<Node>();
		Id id = new IdImpl("dummy");
		nodes.add(new PseudoNode(id, new CoordImpl(-100, -100)));
		nodes.add(new PseudoNode(id, new CoordImpl(-200, -105)));
		nodes.add(new PseudoNode(id, new CoordImpl(-120, -250)));
		nodes.add(new PseudoNode(id, new CoordImpl(-150, -300)));
		nodes.add(new PseudoNode(id, new CoordImpl(-50, -199)));
		double[] box = NetworkUtils.getBoundingBox(nodes);
		assertEquals(-200, box[0], EPSILON); // minX
		assertEquals(-300, box[1], EPSILON); // minY
		assertEquals(-50, box[2], EPSILON); // maxX
		assertEquals(-100, box[3], EPSILON); // maxY
	}

	private static class PseudoLink extends FakeLink {
		private final double nOfLanes;
		public PseudoLink(final double nOfLanes) {
			super(new IdImpl(1));
			this.nOfLanes = nOfLanes;
		}

		@Override
		public double getNumberOfLanes(double time) {
			return this.nOfLanes;
		}
	}

	private static class PseudoNode extends FakeNode {
		private final Coord coord;
		public PseudoNode(Id id, Coord coord) {
			super(id);
			this.coord = coord;
		}
		@Override
		public Coord getCoord() {
			return this.coord;
		}
	}

	private NetworkImpl getTestNetwork() {
		int numOfLinks = 5;

		NetworkImpl network = NetworkImpl.createNetwork();
		Node[] nodes = new Node[numOfLinks+1];
		for (int i = 0; i <= numOfLinks; i++) {
			nodes[i] = network.createAndAddNode(new IdImpl(i), new CoordImpl(1000 * i, 0));
		}
		for (int i = 0; i < numOfLinks; i++) {
			network.createAndAddLink(new IdImpl(i), nodes[i], nodes[i+1], 1000.0, 10.0, 3600.0, 1);
		}
		return network;
	}
}
