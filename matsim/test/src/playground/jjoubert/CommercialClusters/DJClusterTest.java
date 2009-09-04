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

package playground.jjoubert.CommercialClusters;

import java.util.ArrayList;
import java.util.List;

import org.matsim.testcases.MatsimTestCase;

import playground.jjoubert.Utilities.Clustering.DJCluster;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class DJClusterTest extends MatsimTestCase{
	
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
	 
	public void testDJCluster(){
//		QuadTree<Point> qt = buildTestQuadTree();
		List<Point> al = buildTestArrayList();
		DJCluster djc = new DJCluster(2, 3, al);
		djc.clusterInput();
		
		assertEquals("There should only be two clusters", 2, djc.getClusterList().size());
		
		assertEquals("The left cluster must have 8 points.", 8, djc.getClusterList().get(0).getPoints().size());
		assertEquals("The right cluster must have 4 points.", 4, djc.getClusterList().get(1).getPoints().size());
		
	}
	private static List<Point> buildTestArrayList(){
		// Build the test QuadTree
		List<Point> al = new ArrayList<Point>();
		GeometryFactory gf = new GeometryFactory();
		
		// Cluster 1
		Point p1 = gf.createPoint(new Coordinate(2,1));
		al.add(p1);
		Point p2 = gf.createPoint(new Coordinate(1,2));
		al.add(p2);
		Point p3 = gf.createPoint(new Coordinate(1,3));
		al.add(p3);
		Point p4 = gf.createPoint(new Coordinate(1,4));
		al.add(p4);		
		Point p5 = gf.createPoint(new Coordinate(2,5));
		al.add(p5);
		Point p6 = gf.createPoint(new Coordinate(3,4));
		al.add(p6);
		Point p7 = gf.createPoint(new Coordinate(3,3));
		al.add(p7);
		Point p8 = gf.createPoint(new Coordinate(3,2));
		al.add(p8);
		
		// Cluster 2
		Point p9 = gf.createPoint(new Coordinate(7,3));
		al.add(p9);
		Point p10 = gf.createPoint(new Coordinate(6,4));
		al.add(p10);
		Point p11 = gf.createPoint(new Coordinate(7,5));
		al.add(p11);
		Point p12 = gf.createPoint(new Coordinate(8,4));
		al.add(p12);
		
		// Cluster 3 - not enough points
		Point p13 = gf.createPoint(new Coordinate(4,7));
		al.add(p13);
		Point p14 = gf.createPoint(new Coordinate(5,7));
		al.add(p14);
		
		return al;
	}
	
//	private static QuadTree<Point> buildTestQuadTree(){
//		// Build the test QuadTree
//		QuadTree<Point> qt = new QuadTree<Point>(0,0,9,8);
//		GeometryFactory gf = new GeometryFactory();
//		
//		// Cluster 1
//		Point p1 = gf.createPoint(new Coordinate(2,1));
//		qt.put(p1.getX(), p1.getY(), p1);
//		Point p2 = gf.createPoint(new Coordinate(1,2));
//		qt.put(p2.getX(), p2.getY(), p2);
//		Point p3 = gf.createPoint(new Coordinate(1,3));
//		qt.put(p3.getX(), p3.getY(), p3);
//		Point p4 = gf.createPoint(new Coordinate(1,4));
//		qt.put(p4.getX(), p4.getY(), p4);		
//		Point p5 = gf.createPoint(new Coordinate(2,5));
//		qt.put(p5.getX(), p5.getY(), p5);
//		Point p6 = gf.createPoint(new Coordinate(3,4));
//		qt.put(p6.getX(), p6.getY(), p6);
//		Point p7 = gf.createPoint(new Coordinate(3,3));
//		qt.put(p7.getX(), p7.getY(), p7);
//		Point p8 = gf.createPoint(new Coordinate(3,2));
//		qt.put(p8.getX(), p8.getY(), p8);
//		
//		// Cluster 2
//		Point p9 = gf.createPoint(new Coordinate(7,3));
//		qt.put(p9.getX(), p9.getY(), p9);
//		Point p10 = gf.createPoint(new Coordinate(6,4));
//		qt.put(p10.getX(), p10.getY(), p10);
//		Point p11 = gf.createPoint(new Coordinate(7,5));
//		qt.put(p11.getX(), p11.getY(), p11);
//		Point p12 = gf.createPoint(new Coordinate(8,4));
//		qt.put(p12.getX(), p12.getY(), p12);
//		
//		// Cluster 3 - not enough points
//		Point p13 = gf.createPoint(new Coordinate(4,7));
//		qt.put(p13.getX(), p13.getY(), p13);
//		Point p14 = gf.createPoint(new Coordinate(5,7));
//		qt.put(p14.getX(), p14.getY(), p14);
//		
//		return qt;
//	}

}
