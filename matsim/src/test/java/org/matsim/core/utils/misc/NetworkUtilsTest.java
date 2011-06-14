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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.testcases.fakes.FakeNode;

/**
 * @author mrieser
 */
public class NetworkUtilsTest {

	private final static Logger log = Logger.getLogger(NetworkUtilsTest.class);
	private final static double EPSILON = 1e-8;

	@Test
	public void testGetNodes_Empty() {
		NetworkImpl network = getTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, "");
		assertEquals(0, nodes.size());

		List<Node> nodes2 = NetworkUtils.getNodes(network, " ");
		assertEquals(0, nodes2.size());

		List<Node> nodes3 = NetworkUtils.getNodes(network, " \t\r\n \t  \t ");
		assertEquals(0, nodes3.size());
	}

	@Test
	public void testGetNodes_Null() {
		NetworkImpl network = getTestNetwork();
		List<Node> nodes = NetworkUtils.getNodes(network, null);
		assertEquals(0, nodes.size());
	}

	@Test
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

	@Test
	public void testGetNodes_NonExistant() {
		NetworkImpl network = getTestNetwork();
		try {
			NetworkUtils.getNodes(network, "1 3 ab 5");
			fail("expected Exception, but didn't happen.");
		} catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}

	@Test
	public void testGetLinks_Empty() {
		NetworkImpl network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, "");
		assertEquals(0, links.size());

		List<Link> links2 = NetworkUtils.getLinks(network, " ");
		assertEquals(0, links2.size());

		List<Link> links3 = NetworkUtils.getLinks(network, " \t\r\n \t  \t ");
		assertEquals(0, links3.size());
	}

	@Test
	public void testGetLinks_StringNull() {
		NetworkImpl network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, (String)null);
		assertEquals(0, links.size());
	}

	@Test
	public void testGetLinks_mixedDelimiters() {
		NetworkImpl network = getTestNetwork();
		List<Link> links = NetworkUtils.getLinks(network, " 1\t\t2 \n4\t \t      3 ");
		assertEquals(4, links.size());
		assertEquals(network.getLinks().get(new IdImpl(1)), links.get(0));
		assertEquals(network.getLinks().get(new IdImpl(2)), links.get(1));
		assertEquals(network.getLinks().get(new IdImpl(4)), links.get(2));
		assertEquals(network.getLinks().get(new IdImpl(3)), links.get(3));
	}

	@Test
	public void testGetLinks_NonExistant() {
		NetworkImpl network = getTestNetwork();
		try {
			NetworkUtils.getLinks(network, "1 3 ab 4");
			fail("expected Exception, but didn't happen.");
		} catch (IllegalArgumentException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}

	@Test
	public void testGetLinksID_ListNull() {
		List<Id> linkIds = NetworkUtils.getLinkIds((List<Link>) null);
		assertEquals(0, linkIds.size());
	}

	@Test
	public void testGetLinksID_StringNull() {
		List<Id> linkIds = NetworkUtils.getLinkIds((String) null);
		assertEquals(0, linkIds.size());
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testIsMultimodal_carOnly() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("car"));
		}
		Assert.assertFalse(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	public void testIsMultimodal_walkOnly() {
		// tests that isMultimodal is not somehow hard-coded on "car"
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("walk"));
		}
		Assert.assertFalse(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	public void testIsMultimodal_2modesOnSingleLink() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("car"));
		}
		f.links[3].setAllowedModes(CollectionUtils.stringToSet("car,bike"));
		Assert.assertTrue(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	public void testIsMultimodal_2modesOnDifferentLinks() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("car"));
		}
		f.links[2].setAllowedModes(CollectionUtils.stringToSet("bike"));
		Assert.assertTrue(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	public void testIsMultimodal_3modes() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("car"));
		}
		f.links[2].setAllowedModes(CollectionUtils.stringToSet("bike,walk"));
		Assert.assertTrue(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	public void testIsMultimodal_onlyNoModes() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet(""));
		}
		Assert.assertFalse(NetworkUtils.isMultimodal(f.network));
	}

	@Test
	public void testIsMultimodal_sometimesNoModes() {
		MultimodalFixture f = new MultimodalFixture();
		for (Link l : f.links) {
			l.setAllowedModes(CollectionUtils.stringToSet("car"));
		}
		f.links[2].setAllowedModes(CollectionUtils.stringToSet(""));
		Assert.assertTrue(NetworkUtils.isMultimodal(f.network));
	}

	private static class PseudoLink extends FakeLink {
		private final double nOfLanes;
		public PseudoLink(final double nOfLanes) {
			super(new IdImpl(1));
			this.nOfLanes = nOfLanes;
		}

		@Override
		public double getNumberOfLanes(final double time) {
			return this.nOfLanes;
		}
	}

	private static class PseudoNode extends FakeNode {
		private final Coord coord;
		public PseudoNode(final Id id, final Coord coord) {
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

	private static class MultimodalFixture {
		/*package*/ final NetworkImpl network = NetworkImpl.createNetwork();
		Node[] nodes = new Node[6];
		Link[] links = new Link[this.nodes.length - 1];

		public MultimodalFixture() {
			for (int i = 0; i < this.nodes.length; i++) {
				this.nodes[i] = this.network.createAndAddNode(new IdImpl(i), new CoordImpl(1000 * i, 0));
			}
			for (int i = 0; i < this.links.length; i++) {
				this.links[i] = this.network.createAndAddLink(new IdImpl(i), this.nodes[i], this.nodes[i+1], 1000.0, 10.0, 3600.0, 1);
			}
		}
	}
}
