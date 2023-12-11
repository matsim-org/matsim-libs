/* *********************************************************************** *
 * project: org.matsim.*
 * DJClusterTest.java
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

package org.matsim.core.network.algorithms.intersectionSimplifier;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public class DensityClusterTest{


	/**
	 * Tests if the following cluster pattern is clustered into two clusters:
	 *       ___________  
	 * 	    |    * *	|
	 * 		|			|
	 * 		|  *     *	|
	 * 		| * *   * * |
	 * 		| * *    *	|
	 * 		| * *		|
	 * 		|  *		|
	 * 		|___________|
	 */		
	@Test
	void testDJCluster(){
		List<Node> al = buildTestArrayList();
		DensityCluster djc = new DensityCluster(al, false);
		djc.clusterInput(2, 3);
		
		Assertions.assertEquals(2, djc.getClusterList().size(), "There should only be two clusters");
		
		int small = Math.min(djc.getClusterList().get(0).getPoints().size(), djc.getClusterList().get(1).getPoints().size());
		int large = Math.max(djc.getClusterList().get(0).getPoints().size(), djc.getClusterList().get(1).getPoints().size());
		
		Assertions.assertEquals(4, small, "The small cluster must have 4 points.");
		Assertions.assertEquals(8, large, "The large cluster must have 8 points.");
	}
	
	
	private static List<Node> buildTestArrayList(){
		// Build the test QuadTree
		List<Node> al = new ArrayList<>();
		
		// Cluster 1
		Coord c1 = new Coord((double) 2, (double) 1);
		al.add(NetworkUtils.createNode(Id.createNodeId("c1"), c1));
		Coord c2 = new Coord((double) 1, (double) 2);
		al.add(NetworkUtils.createNode(Id.createNodeId("c2"), c2));
		Coord c3 = new Coord((double) 1, (double) 3);
		al.add(NetworkUtils.createNode(Id.createNodeId("c3"), c3));
		Coord c4 = new Coord((double) 1, (double) 4);
		al.add(NetworkUtils.createNode(Id.createNodeId("c4"), c4));
		Coord c5 = new Coord((double) 2, (double) 5);
		al.add(NetworkUtils.createNode(Id.createNodeId("c5"), c5));
		Coord c6 = new Coord((double) 3, (double) 4);
		al.add(NetworkUtils.createNode(Id.createNodeId("c6"), c6));
		Coord c7 = new Coord((double) 3, (double) 3);
		al.add(NetworkUtils.createNode(Id.createNodeId("c7"), c7));
		Coord c8 = new Coord((double) 3, (double) 2);
		al.add(NetworkUtils.createNode(Id.createNodeId("c8"), c8));
			
		// Cluster 2
		Coord c9 = new Coord((double) 7, (double) 3);
		al.add(NetworkUtils.createNode(Id.createNodeId("c9"), c9));
		Coord c10 = new Coord((double) 6, (double) 4);
		al.add(NetworkUtils.createNode(Id.createNodeId("c10"), c10));
		Coord c11 = new Coord((double) 7, (double) 5);
		al.add(NetworkUtils.createNode(Id.createNodeId("c11"), c11));
		Coord c12 = new Coord((double) 8, (double) 4);
		al.add(NetworkUtils.createNode(Id.createNodeId("c12"), c12));
		
		// Cluster 3 - not enough points
		Coord c13 = new Coord((double) 4, (double) 7);
		al.add(NetworkUtils.createNode(Id.createNodeId("c13"), c13));
		Coord c14 = new Coord((double) 5, (double) 7);
		al.add(NetworkUtils.createNode(Id.createNodeId("c14"), c14));

		return al;
	}
}
