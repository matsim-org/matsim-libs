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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;

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
	public void testDJCluster(){
		List<Coord> al = buildTestArrayList();
		DensityCluster djc = new DensityCluster(al, false);
		djc.clusterInput(2, 3);
		
		Assert.assertEquals("There should only be two clusters", 2, djc.getClusterList().size());
		
		int small = Math.min(djc.getClusterList().get(0).getPoints().size(), djc.getClusterList().get(1).getPoints().size());
		int large = Math.max(djc.getClusterList().get(0).getPoints().size(), djc.getClusterList().get(1).getPoints().size());
		
		Assert.assertEquals("The small cluster must have 4 points.", 4, small);
		Assert.assertEquals("The large cluster must have 8 points.", 8, large);
	}
	
	
	private static List<Coord> buildTestArrayList(){
		// Build the test QuadTree
		List<Coord> al = new ArrayList<Coord>();
		
		// Cluster 1
		Coord c1 = new Coord((double) 2, (double) 1);
		al.add(c1);
		Coord c2 = new Coord((double) 1, (double) 2);
		al.add(c2);
		Coord c3 = new Coord((double) 1, (double) 3);
		al.add(c3);
		Coord c4 = new Coord((double) 1, (double) 4);
		al.add(c4);
		Coord c5 = new Coord((double) 2, (double) 5);
		al.add(c5);
		Coord c6 = new Coord((double) 3, (double) 4);
		al.add(c6);
		Coord c7 = new Coord((double) 3, (double) 3);
		al.add(c7);
		Coord c8 = new Coord((double) 3, (double) 2);
		al.add(c8);
			
		// Cluster 2
		Coord c9 = new Coord((double) 7, (double) 3);
		al.add(c9);
		Coord c10 = new Coord((double) 6, (double) 4);
		al.add(c10);
		Coord c11 = new Coord((double) 7, (double) 5);
		al.add(c11);
		Coord c12 = new Coord((double) 8, (double) 4);
		al.add(c12);
		
		// Cluster 3 - not enough points
		Coord c13 = new Coord((double) 4, (double) 7);
		al.add(c13);
		Coord c14 = new Coord((double) 5, (double) 7);
		al.add(c14);

		return al;
	}
}
