/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.accessibility.run;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.utils.Distances;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dziemke
 *
 */
public class NetworkUtilTest {
	
	private static final Logger log = Logger.getLogger(NetworkUtilTest.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	
	@Test
	public void testNetworkUtil() {
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
		 * 
		 * why is that a "special case"? in a normal network all sort of slopes are *normally* present. dz, feb'16
		 */
		
		NetworkImpl network = (NetworkImpl) NetworkUtils.createNetwork();
		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord((double) 1000, (double) 2000));
		Node node4 = network.createAndAddNode(Id.create("4", Node.class), new Coord((double) 2000, (double) 2000));
		Node node5 = network.createAndAddNode(Id.create("5", Node.class), new Coord((double) 1000, (double) 0));
		LinkImpl link1 = (LinkImpl) network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1000, 1, 3600, 1);
		LinkImpl link2 = (LinkImpl) network.createAndAddLink(Id.create("2", Link.class), node2, node3, 1500, 1, 3600, 1);
		LinkImpl link3 = (LinkImpl) network.createAndAddLink(Id.create("3", Link.class), node3, node4, 1000, 1, 3600, 1);
		LinkImpl link4 = (LinkImpl) network.createAndAddLink(Id.create("4", Link.class), node4, node5, 2800, 1, 3600, 1);

		Coord a = new Coord((double) 100, (double) 0);
		Distances distance1 = NetworkUtil.getDistances2Node(a, link1, node1);
		log.info(distance1.getDistancePoint2Road() + distance1.getDistanceRoad2Node() + " distance1");
		Assert.assertEquals("... should be 100....!", 100.0, distance1.getDistancePoint2Road(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("... should be 0....!", 0.0, distance1.getDistanceRoad2Node(), MatsimTestUtils.EPSILON);
		

//		final double y = -10;
		Distances distance2 = NetworkUtil.getDistances2Node(new Coord((double) 100, (double) -10), link1, node1);
		log.info(distance2.getDistancePoint2Road() + distance2.getDistanceRoad2Node() + " distance2");

		Distances distance3 = NetworkUtil.getDistances2Node(new Coord((double) 100, (double) 1000), link2, node2);
		log.info(distance3.getDistancePoint2Road() + distance3.getDistanceRoad2Node() + " distance3");

//		final double x = -100;
		Distances distance4 = NetworkUtil.getDistances2Node(new Coord((double) -100, (double) 1000), link2, node2);
		log.info(distance4.getDistancePoint2Road() + distance4.getDistanceRoad2Node() + " distance4");
	}

	
	
}