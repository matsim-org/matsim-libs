/* *********************************************************************** *
 * project: org.matsim.*
 * LinkImplTest.java
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

package org.matsim.core.network;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl.HashSetCache;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author mrieser
 */
public class LinkImplTest {

	private final static Logger log = LogManager.getLogger(LinkImplTest.class);
	private static final double EPSILON = 1e-7;


	@Test
	void testCalcDistance() {
		/* create a sample network:
		 *
		 *        (3)---3---(4)
		 *       /         /
		 *     2          /
		 *   /           /
		 * (2)          4
		 *  |          /
		 *  1         /
		 *  |        /
		 * (1)    (5)
		 *
		 * The network contains an exactly horizontal, an exactly vertical, an exactly diagonal
		 * and another link with no special slope to also test possible special cases.
		 */
		final Network network = new NetworkImpl(new LinkFactoryImpl());
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 1000, (double) 2000));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 2000, (double) 2000));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord((double) 1000, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = (Link) NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 1000, (double) 1, (double) 3600, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		Link link2 = (Link) NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 1500, (double) 1, (double) 3600, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		Link link3 = (Link) NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, (double) 1000, (double) 1, (double) 3600, (double) 1 );
		final Node fromNode3 = node4;
		final Node toNode3 = node5;
		Link link4 = (Link) NetworkUtils.createAndAddLink(network,Id.create("4", Link.class), fromNode3, toNode3, (double) 2800, (double) 1, (double) 3600, (double) 1 );

		// do the following cases for each link

		// case 1: point is orthogonal next to link.fromNode, test both sides of link
		Assertions.assertEquals(10.0, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(10.0, 0.0)), EPSILON);
		final double x7 = -12.5;
		Assertions.assertEquals(12.5, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(x7, 0.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(2*65.4*65.4), CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(65.4, 1000.0 - 65.4)), EPSILON);
		final double x6 = -76.5;
		Assertions.assertEquals(Math.sqrt(2*76.5*76.5), CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(x6, 1000.0 + 76.5)), EPSILON);
		Assertions.assertEquals(123.987, CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(1000.0, 2000.0 - 123.987)), EPSILON);
		Assertions.assertEquals(23.87, CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(1000.0, 2000.0 + 23.87)), EPSILON);
		Assertions.assertEquals(Math.sqrt(32.4*32.4*1.25), CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(2000.0 + 32.4, 2000 - 32.4 / 2.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(56.8*56.8*1.25), CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(2000.0 - 56.8, 2000 + 56.8 / 2.0)), EPSILON);

		// case 2: point is behind link, but exactly on extension of the link's line
		final double y6 = -15.0;
		Assertions.assertEquals(15.0, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(0.0, y6)), EPSILON);
		final double x5 = -5.0;
		Assertions.assertEquals(Math.sqrt(50.0), CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(x5, 995.0)), EPSILON);
		Assertions.assertEquals(12.35, CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(987.65, 2000.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(250.0*250.0 + 500.0*500.0), CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(2250.0, 2500.0)), EPSILON);

		// case 3: point is behind and on the side of link, test both sides of link
		final double y5 = -15.0;
		Assertions.assertEquals(Math.sqrt(325.0), CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(10.0, y5)), EPSILON);
		final double x4 = -15.0;
		final double y4 = -20.0;
		Assertions.assertEquals(Math.sqrt(625.0), CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(x4, y4)), EPSILON);
		Assertions.assertEquals(50.5, CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(0.0, 949.5)), EPSILON);
		final double x3 = -51.5;
		Assertions.assertEquals(51.5, CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(x3, 1000.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(1300.0), CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(970.0, 1980.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(1300.0), CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(980.0, 2030.0)), EPSILON);
		Assertions.assertEquals(145.7, CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(2000.0, 2145.7)), EPSILON);
		Assertions.assertEquals(89.0, CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(2089.0, 2000.0)), EPSILON);

		// case 4: point is orthogonal next to link.toNode, test both sides of link
		final double x2 = -5.0;
		Assertions.assertEquals(5.0, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(x2, 1000.0)), EPSILON);
		Assertions.assertEquals(7.5, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(7.5, 1000.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(2*234.5*234.5), CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(1234.5, 2000.0 - 234.5)), EPSILON);
		Assertions.assertEquals(Math.sqrt(2*11.1*11.1), CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(1000.0 - 11.1, 2000.0 + 11.1)), EPSILON);
		Assertions.assertEquals(43.3, CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(2000.0, 1956.7)), EPSILON);
		Assertions.assertEquals(10.3, CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(2000.0, 2010.3)), EPSILON);
		final double y3 = +22.2;
		Assertions.assertEquals(Math.sqrt(44.4*44.4*1.25), CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(1000 - 44.4, y3)), EPSILON);
		final double y2 = -33.3;
		Assertions.assertEquals(Math.sqrt(66.6*66.6*1.25), CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(1000 + 66.6, y2)), EPSILON);

		// case 5: point is in front of link, but exactly on extension of the link's line
		Assertions.assertEquals(23.4, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord((double) 0, 1023.4)), EPSILON);
		Assertions.assertEquals(Math.sqrt(200.0), CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord((double) 1010, 2010.0)), EPSILON);
		Assertions.assertEquals(987.6, CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(2987.6, 2000.0)), EPSILON);
		final double y1 = -500.0;
		Assertions.assertEquals(Math.sqrt(250.0*250.0 + 500.0*500.0), CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(750.0, y1)), EPSILON);

		// case 6: point is in front of link and on side of link, test both sides of link
		final double x1 = -3.0;
		Assertions.assertEquals(5.0, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(x1, 1004.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(113.0), CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(8.0, 1007.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(100.0*100.0+50.0*50.0), CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(1100.0, 2050.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(50.0*50.0+100.0*100.0), CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(1050.0, 2100.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(100.0*100.0+50.0*50.0), CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(2100.0, 2050.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(50.0*50.0+100.0*100.0), CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(2050.0, 1900.0)), EPSILON);
		final double y = -50.0;
		Assertions.assertEquals(50.0, CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(1000.0, y)), EPSILON);
		Assertions.assertEquals(49.0, CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(951.0, 0.0)), EPSILON);

		// case 7: point is on the side of link, between from- and to-Node, test both sides of link
		final double x = -42.0;
		Assertions.assertEquals(42.0, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(x, 987.65)), EPSILON);
		Assertions.assertEquals(123.4, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(123.4, 98.765)), EPSILON);
		Assertions.assertEquals(Math.sqrt(2*125.0*125.0), CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(500.0, 1250.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(2*250.0*250.0), CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(500.0, 2000.0)), EPSILON);
		Assertions.assertEquals(658.3, CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(1234.5, 2000.0 - 658.3)), EPSILON);
		Assertions.assertEquals(422.1, CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(1846.3, 2422.1)), EPSILON);
		Assertions.assertEquals(Math.sqrt(250.0*250.0+125.0*125.0), CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(2000.0, 1375.0)), EPSILON);
		Assertions.assertEquals(Math.sqrt(500.0*500.0+250.0*250.0), CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(1000.0, 1250.0)), EPSILON);

		// case 8: point is *on* the link (exactly on fromnode, exactly on tonode, exactly between somewhere)
		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), link1.getFromNode().getCoord()), EPSILON, "point = link1.fromNode");
		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), link1.getToNode().getCoord()), EPSILON, "point = link1.toNode");
		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link1.getFromNode().getCoord(), link1.getToNode().getCoord(), new Coord(0.0, 135.79)), EPSILON, "point on link1");

		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), link2.getFromNode().getCoord()), EPSILON, "point = link2.fromNode");
		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), link2.getToNode().getCoord()), EPSILON, "point = link2.toNode");
		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link2.getFromNode().getCoord(), link2.getToNode().getCoord(), new Coord(65.43, 1065.43)), EPSILON, "point on link2");

		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), link3.getFromNode().getCoord()), EPSILON, "point = link3.fromNode");
		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), link3.getToNode().getCoord()), EPSILON, "point = link3.toNode");
		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link3.getFromNode().getCoord(), link3.getToNode().getCoord(), new Coord(1234.5678, 2000.0)), EPSILON, "point on link3");

		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), link4.getFromNode().getCoord()), EPSILON, "point = link4.fromNode");
		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), link4.getToNode().getCoord()), EPSILON, "point = link4.toNode");
		Assertions.assertEquals(0.0, CoordUtils.distancePointLinesegment(link4.getFromNode().getCoord(), link4.getToNode().getCoord(), new Coord(1750.0, 1500.0)), EPSILON, "point on link4");
	}

	@Test
	void testSetAttributes() {
		Network network = new NetworkImpl(new LinkFactoryImpl());
		network.setCapacityPeriod(3600.0);
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 50, (double) 50));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = (Link) NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, 500.0, 10.0, 1000.0, 1.0 );
		Assertions.assertEquals(50.0, NetworkUtils.getFreespeedTravelTime(link1), EPSILON, "wrong freespeed traveltime.");
		link1.setLength(1000.0);
		Assertions.assertEquals(100.0, NetworkUtils.getFreespeedTravelTime(link1), EPSILON, "wrong freespeed traveltime.");
		link1.setFreespeed(20.0);
		Assertions.assertEquals(50.0, NetworkUtils.getFreespeedTravelTime(link1), EPSILON, "wrong freespeed traveltime.");
	}

	/**
	 * Tests setting and getting allowed modes for links.
	 *
	 */
	@Test
	void testAllowedModes() {
		Network network = new NetworkImpl(new LinkFactoryImpl());
		Node n1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node n2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		final Node fromNode = n1;
		final Node toNode = n2;
		Link l = NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, (double) 1000, (double) 10, (double) 3600, (double) 1 );

		// test default
		Set<String> modes = l.getAllowedModes();
		Assertions.assertEquals(1, modes.size(), "wrong number of default entries.");
		Assertions.assertTrue(modes.contains(TransportMode.car), "wrong default.");

		// test set/get empty list
		l.setAllowedModes(new HashSet<String>());
		modes = l.getAllowedModes();
		Assertions.assertEquals(0, modes.size(), "wrong number of allowed modes.");

		// test set/get list with entries
		modes = new HashSet<String>();
		modes.add(TransportMode.walk);
		modes.add(TransportMode.car);
		modes.add(TransportMode.bike);
		l.setAllowedModes(modes);
		modes = l.getAllowedModes();
		Assertions.assertEquals(3, modes.size(), "wrong number of allowed modes");
		Assertions.assertTrue(modes.contains(TransportMode.walk));
		Assertions.assertTrue(modes.contains(TransportMode.car));
		Assertions.assertTrue(modes.contains(TransportMode.bike));
	}


	@Test
	void testHashSetCache_get_unmodifiable() {
		Set<String> s1 = new TreeSet<String>();
		s1.add("A");
		s1.add("B");

		Set<String> s = HashSetCache.get(s1);
		Assertions.assertTrue(s != s1);
		Assertions.assertEquals(2, s.size());
		Assertions.assertTrue(s.contains("A"));
		Assertions.assertTrue(s.contains("B"));
		Assertions.assertFalse(s.contains("C"));
		Assertions.assertFalse(s.contains("D"));
		try {
			s.add("D");
			Assertions.fail("Expected Exception, but got none");
		} catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}

		s1.add("C");
		Assertions.assertEquals(2, s.size(), "the returned set must be independent of the given, original set.");
	}


	@Test
	void testHashSetCache_get_null() {
		Set<String> s = HashSetCache.get((Set<String>) null);
		Assertions.assertNull(s);
	}


	@Test
	void testHashSetCache_get_emptySet() {
		Set<String> s = HashSetCache.get(new TreeSet<String>());
		Assertions.assertNotNull(s);
		Assertions.assertEquals(0, s.size());
	}


	@Test
	void testHashSetCache_get() {
		Set<String> s1 = new TreeSet<String>();
		s1.add("A");
		s1.add("B");
		Set<String> s2 = new HashSet<String>();
		s2.add("A");
		s2.add("B");
		Set<String> s3 = new HashSet<String>();
		s3.add("C");
		s3.add("B");
		Set<String> s4 = new HashSet<String>();
		s4.add("A");

		Set<String> s_1 = HashSetCache.get(s1);
		Assertions.assertTrue(s_1 != s1);
		Assertions.assertEquals(2, s_1.size());
		Assertions.assertTrue(s_1.contains("A"));
		Assertions.assertTrue(s_1.contains("B"));
		Assertions.assertFalse(s_1.contains("C"));
		Assertions.assertFalse(s_1.contains("D"));

		Set<String> s_2 = HashSetCache.get(s2);
		Assertions.assertTrue(s_1 == s_2);

		Set<String> s_3 = HashSetCache.get(s3);
		Assertions.assertTrue(s_3 != s3);
		Assertions.assertEquals(2, s_3.size());
		Assertions.assertFalse(s_3.contains("A"));
		Assertions.assertTrue(s_3.contains("B"));
		Assertions.assertTrue(s_3.contains("C"));
		Assertions.assertFalse(s_3.contains("D"));

		Set<String> s_4 = HashSetCache.get(s4);
		Assertions.assertTrue(s_4 != s4);
		Assertions.assertEquals(1, s_4.size());
	}


	@Test
	void testHashSetCache_get_identicalObjects() {
		Set<String> s1 = new TreeSet<String>();
		s1.add("A");
		s1.add("B");
		Set<String> s2 = new HashSet<String>();
		s2.add("A");
		s2.add("B");
		Set<String> s3 = new HashSet<String>();
		s3.add("C");
		s3.add("B");
		s3.add("A");

		Set<String> s_1 = HashSetCache.get(s1);
		Assertions.assertTrue(s_1 != s1);
		Assertions.assertEquals(2, s_1.size());

		Set<String> s_2 = HashSetCache.get(s2);
		Assertions.assertTrue(s_1 == s_2);

		Set<String> s_3 = HashSetCache.get(s3);
		Assertions.assertTrue(s_3 != s3);
		Assertions.assertEquals(3, s_3.size());
	}

}
