/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author mrieser / senozon
 */
public class NetworkExpandNodeTest {

	@Test
	public void testExpandNode() {
		Fixture f = new Fixture();
		f.createNetwork_ThreeWayIntersection();
		
		NetworkExpandNode exp = new NetworkExpandNode(f.scenario.getNetwork(), 25, 5);
		ArrayList<Tuple<Id, Id>> turns = new ArrayList<Tuple<Id, Id>>();
		turns.add(new Tuple<Id, Id>(f.scenario.createId("1"), f.scenario.createId("6")));
		turns.add(new Tuple<Id, Id>(f.scenario.createId("3"), f.scenario.createId("6")));
		turns.add(new Tuple<Id, Id>(f.scenario.createId("5"), f.scenario.createId("2")));
		turns.add(new Tuple<Id, Id>(f.scenario.createId("5"), f.scenario.createId("4")));
		
		exp.expandNode(f.scenario.createId("3"), turns);
		Network n = f.scenario.getNetwork();
		Assert.assertEquals(12, n.getLinks().size());
		Assert.assertEquals(10, n.getNodes().size());
		Assert.assertNotNull(findLinkBetween(n, f.scenario.createId("1"), f.scenario.createId("6")));
		Assert.assertNotNull(findLinkBetween(n, f.scenario.createId("3"), f.scenario.createId("6")));
		Assert.assertNotNull(findLinkBetween(n, f.scenario.createId("5"), f.scenario.createId("2")));
		Assert.assertNotNull(findLinkBetween(n, f.scenario.createId("5"), f.scenario.createId("4")));
		Assert.assertNull(findLinkBetween(n, f.scenario.createId("1"), f.scenario.createId("2")));
		Assert.assertNull(findLinkBetween(n, f.scenario.createId("1"), f.scenario.createId("4")));
		Assert.assertNull(findLinkBetween(n, f.scenario.createId("3"), f.scenario.createId("2")));
		Assert.assertNull(findLinkBetween(n, f.scenario.createId("3"), f.scenario.createId("4")));
		Assert.assertNull(findLinkBetween(n, f.scenario.createId("5"), f.scenario.createId("6")));
		
		// test correct attributes on new links
		Link l = findLinkBetween(n, f.scenario.createId("1"), f.scenario.createId("6"));
		Assert.assertEquals("Capacity attribute is not correct", 1800.0, l.getCapacity(), 1e-8);
		Assert.assertEquals("Number of lanes is not correct", 2.0, l.getNumberOfLanes(), 1e-8);
		Assert.assertEquals("Freespeed is not correct", 10.0, l.getFreespeed(), 1e-8);
		Set<String> modes = l.getAllowedModes();
		Assert.assertEquals("Allowed modes are not correct", 2, modes.size());
		Assert.assertTrue(modes.contains(TransportMode.walk));
		Assert.assertTrue(modes.contains(TransportMode.car));

		// test correct attributes on modified in-links
		l = n.getLinks().get(f.scenario.createId("3"));
		Assert.assertEquals("Capacity attribute is not correct", 1800.0, l.getCapacity(), 1e-8);
		Assert.assertEquals("Number of lanes is not correct", 2.0, l.getNumberOfLanes(), 1e-8);
		Assert.assertEquals("Freespeed is not correct", 10.0, l.getFreespeed(), 1e-8);
		
		modes = l.getAllowedModes();
		Assert.assertEquals("Allowed modes are not correct", 2, modes.size());
		Assert.assertTrue(modes.contains(TransportMode.walk));
		Assert.assertTrue(modes.contains(TransportMode.car));

		// test correct attributes on modified out-links
		l = n.getLinks().get(f.scenario.createId("6"));
		Assert.assertEquals("Capacity attribute is not correct", 1800.0, l.getCapacity(), 1e-8);
		Assert.assertEquals("Number of lanes is not correct", 2.0, l.getNumberOfLanes(), 1e-8);
		Assert.assertEquals("Freespeed is not correct", 10.0, l.getFreespeed(), 1e-8);
		
		modes = l.getAllowedModes();
		Assert.assertEquals("Allowed modes are not correct", 2, modes.size());
		Assert.assertTrue(modes.contains(TransportMode.walk));
		Assert.assertTrue(modes.contains(TransportMode.car));
		
		// test coordinates of new nodes
		l = n.getLinks().get(f.scenario.createId("1"));
		Coord c = l.getToNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);

		l = n.getLinks().get(f.scenario.createId("2"));
		c = l.getFromNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);

		l = n.getLinks().get(f.scenario.createId("3"));
		c = l.getToNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);
		
		l = n.getLinks().get(f.scenario.createId("4"));
		c = l.getFromNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);
		
		l = n.getLinks().get(f.scenario.createId("5"));
		c = l.getToNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);
		
		l = n.getLinks().get(f.scenario.createId("6"));
		c = l.getFromNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);

	}

	@Test
	public void testExpandNode_sameCoordinateLinks() {
		Fixture f = new Fixture();
		f.createNetwork_ThreeWayIntersection();
		Coord c = f.scenario.getNetwork().getNodes().get(f.scenario.createId("3")).getCoord();
		f.scenario.getNetwork().getNodes().get(f.scenario.createId("1")).getCoord().setXY(c.getX(), c.getY()); // move it on top of node 3
		
		
		NetworkExpandNode exp = new NetworkExpandNode(f.scenario.getNetwork(), 25, 5);
		ArrayList<Tuple<Id, Id>> turns = new ArrayList<Tuple<Id, Id>>();
		turns.add(new Tuple<Id, Id>(f.scenario.createId("1"), f.scenario.createId("6")));
		turns.add(new Tuple<Id, Id>(f.scenario.createId("3"), f.scenario.createId("6")));
		turns.add(new Tuple<Id, Id>(f.scenario.createId("5"), f.scenario.createId("2")));
		turns.add(new Tuple<Id, Id>(f.scenario.createId("5"), f.scenario.createId("4")));
		
		exp.expandNode(f.scenario.createId("3"), turns);
		Network n = f.scenario.getNetwork();
		Assert.assertEquals(12, n.getLinks().size());
		Assert.assertEquals(10, n.getNodes().size());
		Assert.assertNotNull(findLinkBetween(n, f.scenario.createId("1"), f.scenario.createId("6")));
		Assert.assertNotNull(findLinkBetween(n, f.scenario.createId("3"), f.scenario.createId("6")));
		Assert.assertNotNull(findLinkBetween(n, f.scenario.createId("5"), f.scenario.createId("2")));
		Assert.assertNotNull(findLinkBetween(n, f.scenario.createId("5"), f.scenario.createId("4")));
		Assert.assertNull(findLinkBetween(n, f.scenario.createId("1"), f.scenario.createId("2")));
		Assert.assertNull(findLinkBetween(n, f.scenario.createId("1"), f.scenario.createId("4")));
		Assert.assertNull(findLinkBetween(n, f.scenario.createId("3"), f.scenario.createId("2")));
		Assert.assertNull(findLinkBetween(n, f.scenario.createId("3"), f.scenario.createId("4")));
		Assert.assertNull(findLinkBetween(n, f.scenario.createId("5"), f.scenario.createId("6")));
		
		// test correct attributes on new links
		Link l = findLinkBetween(n, f.scenario.createId("1"), f.scenario.createId("6"));
		Assert.assertEquals("Capacity attribute is not correct", 1800.0, l.getCapacity(), 1e-8);
		Assert.assertEquals("Number of lanes is not correct", 2.0, l.getNumberOfLanes(), 1e-8);
		Assert.assertEquals("Freespeed is not correct", 10.0, l.getFreespeed(), 1e-8);
		Set<String> modes = l.getAllowedModes();
		Assert.assertEquals("Allowed modes are not correct", 2, modes.size());
		Assert.assertTrue(modes.contains(TransportMode.walk));
		Assert.assertTrue(modes.contains(TransportMode.car));
		
		// test correct attributes on modified in-links
		l = n.getLinks().get(f.scenario.createId("3"));
		Assert.assertEquals("Capacity attribute is not correct", 1800.0, l.getCapacity(), 1e-8);
		Assert.assertEquals("Number of lanes is not correct", 2.0, l.getNumberOfLanes(), 1e-8);
		Assert.assertEquals("Freespeed is not correct", 10.0, l.getFreespeed(), 1e-8);
		
		modes = l.getAllowedModes();
		Assert.assertEquals("Allowed modes are not correct", 2, modes.size());
		Assert.assertTrue(modes.contains(TransportMode.walk));
		Assert.assertTrue(modes.contains(TransportMode.car));
		
		// test correct attributes on modified out-links
		l = n.getLinks().get(f.scenario.createId("6"));
		Assert.assertEquals("Capacity attribute is not correct", 1800.0, l.getCapacity(), 1e-8);
		Assert.assertEquals("Number of lanes is not correct", 2.0, l.getNumberOfLanes(), 1e-8);
		Assert.assertEquals("Freespeed is not correct", 10.0, l.getFreespeed(), 1e-8);
		
		modes = l.getAllowedModes();
		Assert.assertEquals("Allowed modes are not correct", 2, modes.size());
		Assert.assertTrue(modes.contains(TransportMode.walk));
		Assert.assertTrue(modes.contains(TransportMode.car));
		
		// test coordinates of new nodes
		l = n.getLinks().get(f.scenario.createId("1"));
		c = l.getToNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);
		
		l = n.getLinks().get(f.scenario.createId("2"));
		c = l.getFromNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);
		
		l = n.getLinks().get(f.scenario.createId("3"));
		c = l.getToNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);
		
		l = n.getLinks().get(f.scenario.createId("4"));
		c = l.getFromNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);
		
		l = n.getLinks().get(f.scenario.createId("5"));
		c = l.getToNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);
		
		l = n.getLinks().get(f.scenario.createId("6"));
		c = l.getFromNode().getCoord();
		Assert.assertFalse(Double.isNaN(c.getX()));
		Assert.assertFalse(Double.isNaN(c.getY()));
		Assert.assertFalse(Double.isInfinite(c.getX()));
		Assert.assertFalse(Double.isInfinite(c.getY()));
		Assert.assertTrue(CoordUtils.calcDistance(c, f.scenario.createCoord(1000, 0)) < 30);
		
	}
	
	private static Link findLinkBetween(final Network network, final Id fromLinkId, final Id toLinkId) {
		Link fromLink = network.getLinks().get(fromLinkId);
		Link toLink = network.getLinks().get(toLinkId);
		Node from = fromLink.getToNode();
		Node to = toLink.getFromNode();
		for (Link link : from.getOutLinks().values()) {
			if (link.getToNode() == to) {
				return link;
			}
		}
		return null;
	}
	
	private static class Fixture {
		private final Scenario scenario;
		
		public Fixture() {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		}
		
		/**
		 * Creates the following, simple network for testing purposes:
		 * <pre>
		 *           (1)
		 *           | ^
		 *           1 |
		 *           | 2
		 *           v |
		 *  (2)<-4---(3)<--5--(4)<--7--(5)
		 *  ( )--3-->( )---6->( )---8->( )
		 * </pre>
		 * Each link has 1 lane, is 1000 long, has a capacity of 1800 and a freespeed of 10.0 and is
		 * open for car and walk.
		 */
		public void createNetwork_ThreeWayIntersection() {
			Network n = this.scenario.getNetwork();
			NetworkFactory nf = n.getFactory();
			Node node1 = nf.createNode(this.scenario.createId("1"), this.scenario.createCoord(1000, 1000));
			Node node2 = nf.createNode(this.scenario.createId("2"), this.scenario.createCoord(0, 0));
			Node node3 = nf.createNode(this.scenario.createId("3"), this.scenario.createCoord(1000, 0));
			Node node4 = nf.createNode(this.scenario.createId("4"), this.scenario.createCoord(2000, 0));
			Node node5 = nf.createNode(this.scenario.createId("5"), this.scenario.createCoord(3000, 0));
			n.addNode(node1);
			n.addNode(node2);
			n.addNode(node3);
			n.addNode(node4);
			n.addNode(node5);
			n.addLink(createLink(nf, "1", node1, node3));
			n.addLink(createLink(nf, "2", node3, node1));
			n.addLink(createLink(nf, "3", node2, node3));
			n.addLink(createLink(nf, "4", node3, node2));
			n.addLink(createLink(nf, "5", node4, node3));
			n.addLink(createLink(nf, "6", node3, node4));
			n.addLink(createLink(nf, "7", node5, node4));
			n.addLink(createLink(nf, "8", node4, node5));
		}

		private Link createLink(final NetworkFactory nf, final String id, final Node fromNode, final Node toNode) {
			Link l = nf.createLink(this.scenario.createId(id), fromNode, toNode);
			l.setLength(1000.0);
			l.setCapacity(1800.0);
			l.setNumberOfLanes(2);
			l.setFreespeed(10.0);
			HashSet<String> modes = new HashSet<String>();
			modes.add(TransportMode.car);
			modes.add(TransportMode.walk);
			l.setAllowedModes(modes);
			return l;
		}
	}
}
