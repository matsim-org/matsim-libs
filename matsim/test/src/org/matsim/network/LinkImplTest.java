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

package org.matsim.network;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

public class LinkImplTest extends MatsimTestCase {

	/** Tests the method {@link LinkImpl#calcDistance(org.matsim.utils.geometry.Coord)}.
	 *
	 * @author mrieser
	 **/
	public void testCalcDistance() {
		final double epsilon = 1e-10;
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
		final NetworkLayer network = new NetworkLayer();
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(0, 1000));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(1000, 2000));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(2000, 2000));
		Node node5 = network.createNode(new IdImpl("5"), new CoordImpl(1000, 0));
		Link link1 = network.createLink(new IdImpl("1"), node1, node2, 1000, 1, 3600, 1);
		Link link2 = network.createLink(new IdImpl("2"), node2, node3, 1500, 1, 3600, 1);
		Link link3 = network.createLink(new IdImpl("3"), node3, node4, 1000, 1, 3600, 1);
		Link link4 = network.createLink(new IdImpl("4"), node4, node5, 2800, 1, 3600, 1);

		// do the following cases for each link

		// case 1: point is orthogonal next to link.fromNode, test both sides of link
		assertEquals(10.0, link1.calcDistance(new CoordImpl(10.0, 0.0)), epsilon);
		assertEquals(12.5, link1.calcDistance(new CoordImpl(-12.5, 0.0)), epsilon);
		assertEquals(Math.sqrt(2*65.4*65.4), link2.calcDistance(new CoordImpl(65.4, 1000.0-65.4)), epsilon);
		assertEquals(Math.sqrt(2*76.5*76.5), link2.calcDistance(new CoordImpl(-76.5, 1000.0+76.5)), epsilon);
		assertEquals(123.987, link3.calcDistance(new CoordImpl(1000.0, 2000.0-123.987)), epsilon);
		assertEquals(23.87, link3.calcDistance(new CoordImpl(1000.0, 2000.0+23.87)), epsilon);
		assertEquals(Math.sqrt(32.4*32.4*1.25), link4.calcDistance(new CoordImpl(2000.0+32.4, 2000-32.4/2.0)), epsilon);
		assertEquals(Math.sqrt(56.8*56.8*1.25), link4.calcDistance(new CoordImpl(2000.0-56.8, 2000+56.8/2.0)), epsilon);

		// case 2: point is behind link, but exactly on extension of the link's line
		assertEquals(15.0, link1.calcDistance(new CoordImpl(0.0, -15.0)), epsilon);
		assertEquals(Math.sqrt(50.0), link2.calcDistance(new CoordImpl(-5.0, 995.0)), epsilon);
		assertEquals(12.35, link3.calcDistance(new CoordImpl(987.65, 2000.0)), epsilon);
		assertEquals(Math.sqrt(250.0*250.0 + 500.0*500.0), link4.calcDistance(new CoordImpl(2250.0, 2500.0)), epsilon);

		// case 3: point is behind and on the side of link, test both sides of link
		assertEquals(Math.sqrt(325.0), link1.calcDistance(new CoordImpl(10.0, -15.0)), epsilon);
		assertEquals(Math.sqrt(625.0), link1.calcDistance(new CoordImpl(-15.0, -20.0)), epsilon);
		assertEquals(50.5, link2.calcDistance(new CoordImpl(0.0, 949.5)), epsilon);
		assertEquals(51.5, link2.calcDistance(new CoordImpl(-51.5, 1000.0)), epsilon);
		assertEquals(Math.sqrt(1300.0), link3.calcDistance(new CoordImpl(970.0, 1980.0)), epsilon);
		assertEquals(Math.sqrt(1300.0), link3.calcDistance(new CoordImpl(980.0, 2030.0)), epsilon);
		assertEquals(145.7, link4.calcDistance(new CoordImpl(2000.0, 2145.7)), epsilon);
		assertEquals(89.0, link4.calcDistance(new CoordImpl(2089.0, 2000.0)), epsilon);

		// case 4: point is orthogonal next to link.toNode, test both sides of link
		assertEquals(5.0, link1.calcDistance(new CoordImpl(-5.0, 1000.0)), epsilon);
		assertEquals(7.5, link1.calcDistance(new CoordImpl(7.5, 1000.0)), epsilon);
		assertEquals(Math.sqrt(2*234.5*234.5), link2.calcDistance(new CoordImpl(1234.5, 2000.0-234.5)), epsilon);
		assertEquals(Math.sqrt(2*11.1*11.1), link2.calcDistance(new CoordImpl(1000.0-11.1, 2000.0+11.1)), epsilon);
		assertEquals(43.3, link3.calcDistance(new CoordImpl(2000.0, 1956.7)), epsilon);
		assertEquals(10.3, link3.calcDistance(new CoordImpl(2000.0, 2010.3)), epsilon);
		assertEquals(Math.sqrt(44.4*44.4*1.25), link4.calcDistance(new CoordImpl(1000-44.4, +22.2)), epsilon);
		assertEquals(Math.sqrt(66.6*66.6*1.25), link4.calcDistance(new CoordImpl(1000+66.6, -33.3)), epsilon);

		// case 5: point is in front of link, but exactly on extension of the link's line
		assertEquals(23.4, link1.calcDistance(new CoordImpl(0, 1023.4)), epsilon);
		assertEquals(Math.sqrt(200.0), link2.calcDistance(new CoordImpl(1010, 2010.0)), epsilon);
		assertEquals(987.6, link3.calcDistance(new CoordImpl(2987.6, 2000.0)), epsilon);
		assertEquals(Math.sqrt(250.0*250.0 + 500.0*500.0), link4.calcDistance(new CoordImpl(750.0, -500.0)), epsilon);

		// case 6: point is in front of link and on side of link, test both sides of link
		assertEquals(5.0, link1.calcDistance(new CoordImpl(-3.0, 1004.0)), epsilon);
		assertEquals(Math.sqrt(113.0), link1.calcDistance(new CoordImpl(8.0, 1007.0)), epsilon);
		assertEquals(Math.sqrt(100.0*100.0+50.0*50.0), link2.calcDistance(new CoordImpl(1100.0, 2050.0)), epsilon);
		assertEquals(Math.sqrt(50.0*50.0+100.0*100.0), link2.calcDistance(new CoordImpl(1050.0, 2100.0)), epsilon);
		assertEquals(Math.sqrt(100.0*100.0+50.0*50.0), link3.calcDistance(new CoordImpl(2100.0, 2050.0)), epsilon);
		assertEquals(Math.sqrt(50.0*50.0+100.0*100.0), link3.calcDistance(new CoordImpl(2050.0, 1900.0)), epsilon);
		assertEquals(50.0, link4.calcDistance(new CoordImpl(1000.0, -50.0)), epsilon);
		assertEquals(49.0, link4.calcDistance(new CoordImpl(951.0, 0.0)), epsilon);

		// case 7: point is on the side of link, between from- and to-Node, test both sides of link
		assertEquals(42.0, link1.calcDistance(new CoordImpl(-42.0, 987.65)), epsilon);
		assertEquals(123.4, link1.calcDistance(new CoordImpl(123.4, 98.765)), epsilon);
		assertEquals(Math.sqrt(2*125.0*125.0), link2.calcDistance(new CoordImpl(500.0, 1250.0)), epsilon);
		assertEquals(Math.sqrt(2*250.0*250.0), link2.calcDistance(new CoordImpl(500.0, 2000.0)), epsilon);
		assertEquals(658.3, link3.calcDistance(new CoordImpl(1234.5, 2000.0-658.3)), epsilon);
		assertEquals(422.1, link3.calcDistance(new CoordImpl(1846.3, 2422.1)), epsilon);
		assertEquals(Math.sqrt(250.0*250.0+125.0*125.0), link4.calcDistance(new CoordImpl(2000.0, 1375.0)), epsilon);
		assertEquals(Math.sqrt(500.0*500.0+250.0*250.0), link4.calcDistance(new CoordImpl(1000.0, 1250.0)), epsilon);

		// case 8: point is *on* the link (exactly on fromnode, exactly on tonode, exactly between somewhere)
		assertEquals("point = link1.fromNode", 0.0, link1.calcDistance(link1.getFromNode().getCoord()), epsilon);
		assertEquals("point = link1.toNode", 0.0, link1.calcDistance(link1.getToNode().getCoord()), epsilon);
		assertEquals("point on link1", 0.0, link1.calcDistance(new CoordImpl(0.0, 135.79)), epsilon);

		assertEquals("point = link2.fromNode", 0.0, link2.calcDistance(link2.getFromNode().getCoord()), epsilon);
		assertEquals("point = link2.toNode", 0.0, link2.calcDistance(link2.getToNode().getCoord()), epsilon);
		assertEquals("point on link2", 0.0, link2.calcDistance(new CoordImpl(65.43, 1065.43)), epsilon);

		assertEquals("point = link3.fromNode", 0.0, link3.calcDistance(link3.getFromNode().getCoord()), epsilon);
		assertEquals("point = link3.toNode", 0.0, link3.calcDistance(link3.getToNode().getCoord()), epsilon);
		assertEquals("point on link3", 0.0, link3.calcDistance(new CoordImpl(1234.5678, 2000.0)), epsilon);

		assertEquals("point = link4.fromNode", 0.0, link4.calcDistance(link4.getFromNode().getCoord()), epsilon);
		assertEquals("point = link4.toNode", 0.0, link4.calcDistance(link4.getToNode().getCoord()), epsilon);
		assertEquals("point on link4", 0.0, link4.calcDistance(new CoordImpl(1750.0, 1500.0)), epsilon);
	}

}
