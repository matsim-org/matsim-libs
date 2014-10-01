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

package playground.southafrica.freight.digicore.algorithms.djcluster;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.southafrica.freight.digicore.algorithms.djcluster.DJCluster;

public class DJClusterTest{
	
	
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
		DJCluster djc = new DJCluster(al, false);
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
		Coord c1 = new CoordImpl(2, 1);
		al.add(c1);
		Coord c2 = new CoordImpl(1, 2);
		al.add(c2);
		Coord c3 = new CoordImpl(1, 3);
		al.add(c3);
		Coord c4 = new CoordImpl(1, 4);
		al.add(c4);
		Coord c5 = new CoordImpl(2, 5);
		al.add(c5);
		Coord c6 = new CoordImpl(3, 4);
		al.add(c6);
		Coord c7 = new CoordImpl(3, 3);
		al.add(c7);
		Coord c8 = new CoordImpl(3, 2);
		al.add(c8);
			
		// Cluster 2
		Coord c9 = new CoordImpl(7, 3);
		al.add(c9);
		Coord c10 = new CoordImpl(6, 4);
		al.add(c10);
		Coord c11 = new CoordImpl(7, 5);
		al.add(c11);
		Coord c12 = new CoordImpl(8, 4);
		al.add(c12);
		
		// Cluster 3 - not enough points
		Coord c13 = new CoordImpl(4, 7);
		al.add(c13);
		Coord c14 = new CoordImpl(5, 7);
		al.add(c14);

		return al;
	}
}
