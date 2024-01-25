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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.testcases.fakes.FakeNode;

/**
 * @author mrieser
 */
public class NetworkUtilsTest {

	private final static Logger log = LogManager.getLogger(NetworkUtilsTest.class);
	private final static double EPSILON = 1e-8;

	@Test
	void testGetNodes_Empty() {
		Network network = getTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, "");
		assertEquals(0, nodes.size());

		List<Node> nodes2 = NetworkUtils.getNodes(network, " ");
		assertEquals(0, nodes2.size());

		List<Node> nodes3 = NetworkUtils.getNodes(network, " \t\r\n \t  \t ");
		assertEquals(0, nodes3.size());
	}

	@Test
	void testGetNodes_Null() {
		Network network = getTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, null);
		assertEquals(0, nodes.size());
	}

	@Test
	void testGetNodes_mixedDelimiters() {
		Network network = getTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, " 1\t\t2 \n4\t \t5      3 ");
		assertEquals(5, nodes.size());
		assertEquals(network.getNodes().get(Id.create(1, Node.class)), nodes.get(0));
		assertEquals(network.getNodes().get(Id.create(2, Node.class)), nodes.get(1));
		assertEquals(network.getNodes().get(Id.create(4, Node.class)), nodes.get(2));
		assertEquals(network.getNodes().get(Id.create(5, Node.class)), nodes.get(3));
		assertEquals(network.getNodes().get(Id.create(3, Node.class)), nodes.get(4));
	}

	@Test
	void testGetNodes_NonExistant() {
		Network network = getTestNetwork();
		try {
			NetworkUtils.getNodes(network, "1 3 ab 5");
			fail("expected Exception, but didn't happen.");
		} catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}

	@Test
	void testGetLinks_Empty() {
		Network network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, "");
		assertEquals(0, links.size());

		List<Link> links2 = NetworkUtils.getLinks(network, " ");
		assertEquals(0, links2.size());

		List<Link> links3 = NetworkUtils.getLinks(network, " \t\r\n \t  \t ");
		assertEquals(0, links3.size());
	}

	@Test
	void testGetLinks_StringNull() {
		Network network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, (String)null);
		assertEquals(0, links.size());
	}

	@Test
	void testGetLinks_mixedDelimiters() {
		Network network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, " 1\t\t2 \n4\t \t      3 ");
		assertEquals(4, links.size());
		assertEquals(network.getLinks().get(Id.create(1, Link.class)), links.get(0));
		assertEquals(network.getLinks().get(Id.create(2, Link.class)), links.get(1));
		assertEquals(network.getLinks().get(Id.create(4, Link.class)), links.get(2));
		assertEquals(network.getLinks().get(Id.create(3, Link.class)), links.get(3));
	}

	@Test
	void testGetLinks_NonExistant() {
		Network network = getTestNetwork();
		try {
			NetworkUtils.getLinks(network, "1 3 ab 4");
			fail("expected Exception, but didn't happen.");
		} catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}

	@Test
	void testGetLinksID_ListNull() {
		List<Id<Link>> linkIds = NetworkUtils.getLinkIds((List<Link>) null);
		assertEquals(0, linkIds.size());
	}

	@Test
	void testGetLinksID_StringNull() {
		List<Id<Link>> linkIds = NetworkUtils.getLinkIds((String) null);
		assertEquals(0, linkIds.size());
	}

	@Test
	void testGetNumberOfLanesAsInt() {
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

		assertEquals(3, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(3.2)));
		assertEquals(3, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(3.1)));
		assertEquals(3, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(3.0)));
		assertEquals(2, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(2.9)));
		assertEquals(2, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(2.5)));
		assertEquals(2, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(2.0)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(1.9)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(1.5)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(1.0)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(0.9)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(0.5)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(0.1)));
		assertEquals(1, NetworkUtils.getNumberOfLanesAsInt(new PseudoLink(0.0)));
	}

	@Test
	void testGetBoundingBox() {
		Collection<Node> nodes = new ArrayList<Node>();
		Id<Node> id = Id.create("dummy", Node.class);
		nodes.add(new PseudoNode(id, new Coord((double) 100, (double) 100)));
		nodes.add(new PseudoNode(id, new Coord((double) 200, (double) 105)));
		nodes.add(new PseudoNode(id, new Coord((double) 120, (double) 250)));
		nodes.add(new PseudoNode(id, new Coord((double) 150, (double) 300)));
		nodes.add(new PseudoNode(id, new Coord((double) 50, (double) 199)));
		double[] box = NetworkUtils.getBoundingBox(nodes);
		assertEquals(50, box[0], EPSILON); // minX
		assertEquals(100, box[1], EPSILON); // miny
		assertEquals(200, box[2], EPSILON); // maxX
		assertEquals(300, box[3], EPSILON); // maxY
	}

	@Test
	void testGetBoundingBox_negativeNodesOnly() {
		Collection<Node> nodes = new ArrayList<Node>();
		Id<Node> id = Id.create("dummy", Node.class);
		final double x4 = -100;
		final double y4 = -100;
		nodes.add(new PseudoNode(id, new Coord(x4, y4)));
		final double x3 = -200;
		final double y3 = -105;
		nodes.add(new PseudoNode(id, new Coord(x3, y3)));
		final double x2 = -120;
		final double y2 = -250;
		nodes.add(new PseudoNode(id, new Coord(x2, y2)));
		final double x1 = -150;
		final double y1 = -300;
		nodes.add(new PseudoNode(id, new Coord(x1, y1)));
		final double x = -50;
		final double y = -199;
		nodes.add(new PseudoNode(id, new Coord(x, y)));
		double[] box = NetworkUtils.getBoundingBox(nodes);
		assertEquals(-200, box[0], EPSILON); // minX
		assertEquals(-300, box[1], EPSILON); // minY
		assertEquals(-50, box[2], EPSILON); // maxX
		assertEquals(-100, box[3], EPSILON); // maxY
	}

	@Test
	void testIsMultimodal_carOnly() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("car"));
		}
		Assertions.assertFalse(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	void testIsMultimodal_walkOnly() {
		// tests that isMultimodal is not somehow hard-coded on "car"
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("walk"));
		}
		Assertions.assertFalse(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	void testIsMultimodal_2modesOnSingleLink() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("car"));
		}
		f.links[3].setAllowedModes(CollectionUtils.stringToSet("car,bike"));
		Assertions.assertTrue(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	void testIsMultimodal_2modesOnDifferentLinks() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("car"));
		}
		f.links[2].setAllowedModes(CollectionUtils.stringToSet("bike"));
		Assertions.assertTrue(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	void testIsMultimodal_3modes() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("car"));
		}
		f.links[2].setAllowedModes(CollectionUtils.stringToSet("bike,walk"));
		Assertions.assertTrue(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	void testIsMultimodal_onlyNoModes() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet(""));
		}
		Assertions.assertFalse(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	void testIsMultimodal_sometimesNoModes() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("car"));
		}
		f.links[2].setAllowedModes(CollectionUtils.stringToSet(""));
		Assertions.assertTrue(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	void testGetConnectingLink() {
		Network net = getTestNetwork();
		Node node1 = net.getNodes().get(Id.create(1, Node.class));
		Node node2 = net.getNodes().get(Id.create(2, Node.class));
		Node node3 = net.getNodes().get(Id.create(3, Node.class));
		Link link1 = net.getLinks().get(Id.create(1, Link.class));
		Link link2 = net.getLinks().get(Id.create(2, Link.class));
		
		Assertions.assertEquals(link1, NetworkUtils.getConnectingLink(node1, node2));
		Assertions.assertEquals(link2, NetworkUtils.getConnectingLink(node2, node3));
		Assertions.assertNull(NetworkUtils.getConnectingLink(node1, node3)); // skip one node
		Assertions.assertNull(NetworkUtils.getConnectingLink(node3, node2)); // backwards
		Assertions.assertNull(NetworkUtils.getConnectingLink(node2, node1)); // backwards
	}
	
	private static class PseudoLink extends FakeLink {
		private final double nOfLanes;
		public PseudoLink(final double nOfLanes) {
			super(Id.create(1, Link.class));
			this.nOfLanes = nOfLanes;
		}

		@Override
		public double getNumberOfLanes() {
			return this.nOfLanes;
		}
	}

	private static class PseudoNode extends FakeNode {
		private final Coord coord;
		public PseudoNode(final Id<Node> id, final Coord coord) {
			super(id);
			this.coord = coord;
		}
		@Override
		public Coord getCoord() {
			return this.coord;
		}
	}

	private Network getTestNetwork() {
		int numOfLinks = 5;

		Network network = NetworkUtils.createNetwork();
        Node[] nodes = new Node[numOfLinks + 1];
		for (int i = 0; i <= numOfLinks; i++) {
			nodes[i] = NetworkUtils.createAndAddNode(network, Id.create(i, Node.class), new Coord((double) (1000 * i), (double) 0));
		}
		for (int i = 0; i < numOfLinks; i++) {
			NetworkUtils.createAndAddLink(network,Id.create(i, Link.class), nodes[i], nodes[i+1], 1000.0, 10.0, 3600.0, (double) 1 );
		}
		return network;
	}

	private static class MultimodalFixture {
        /*package*/ final Network network = NetworkUtils.createNetwork();
        Node[] nodes = new Node[6];
		Link[] links = new Link[this.nodes.length - 1];

		public MultimodalFixture() {
			for (int i = 0; i < this.nodes.length; i++) {
				this.nodes[i] = NetworkUtils.createAndAddNode(this.network, Id.create(i, Node.class), new Coord((double) (1000 * i), (double) 0));
			}
			for (int i = 0; i < this.links.length; i++) {
				this.links[i] = NetworkUtils.createAndAddLink(this.network,Id.create(i, Link.class), this.nodes[i], this.nodes[i+1], 1000.0, 10.0, 3600.0, (double) 1 );
			}
		}
	}
	
}
