/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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
package org.matsim.contrib.matrixbasedptrouter;

import java.util.Collection;
import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;

/**
 * I <i>think</i> that this tests if the centrally provided quad tree provides expected results.  kai, dec'14
 * 
 * @author (of original material) thomas
 * @author (of additional material) nagel
 *
 */
public class TestQuadTree {

	private Coord home, work;
	private Coord pt1a, pt1b, pt2a, pt2b, pt3a, pt3b;
	private QuadTree<String> qTree;

	public TestQuadTree(){
		init();
	}

	/**
	 * 		 	pt1
	 * 			|
	 * 			|
	 * 			|
	 *       	pt1  
	 * 		home
	 *   pt2---------------------pt2
	 *   		pt3
	 * 				\
	 * 				 \
	 * 				  \	
	 * 				   \
	 * 					pt3		work
	 * 
	 */
	private void init(){

		home = new Coord(150., 500.);
		work = new Coord(900., 100.);

		pt1a = new Coord(200., 550.);
		pt1b = new Coord(200., 900.);
		pt2a = new Coord(100., 450.);
		pt2b = new Coord(900., 450.);
		pt3a = new Coord(150., 420.);
		pt3b = new Coord(880., 100.);

		double minX = 0.;
		double maxX = 1000.;
		double minY = 0.;
		double maxY = 1000.;

		qTree= new QuadTree<String>(minX, minY, maxX, maxY);

		qTree.put(pt1a.getX(), pt1a.getY(), "pt1a");
		qTree.put(pt1b.getX(), pt1b.getY(), "pt1b");

		qTree.put(pt2a.getX(), pt2a.getY(), "pt2a");
		qTree.put(pt2b.getX(), pt2b.getY(), "pt2b");

		qTree.put(pt3a.getX(), pt3a.getY(), "pt3a");
		qTree.put(pt3b.getX(), pt3b.getY(), "pt3b");
	}

	private void determineNearestPtStation(){

		double distance = 100.;

		{		
			Collection<String> ptColHome = qTree.getDisk(home.getX(), home.getY(), distance);
			Iterator<String> homeIt = ptColHome.iterator();
			System.out.println("Pt stations in " + distance + "m distance to home location:");
			StringBuilder strb = new StringBuilder() ;
			while(homeIt.hasNext()) {
				strb.append( homeIt.next() );
				strb.append( ' ' );
			}
			System.out.println( strb );
			Assertions.assertEquals( "pt1a pt2a pt3a ", strb.toString() );
		}
		{
			Collection<String> ptColWork = qTree.getDisk(work.getX(), work.getY(), distance);
			Iterator<String> workIt = ptColWork.iterator();
			System.out.println("Pt stations in " + distance + "m distance to work location:");
			StringBuilder strb = new StringBuilder() ;
			while(workIt.hasNext()) {
				strb.append(workIt.next() );
				strb.append( ' ') ;
			}
			System.out.println( strb );
			Assertions.assertEquals( "pt3b ", strb.toString() );
		}
	}

	@Test
	void test() {
		TestQuadTree tc = new TestQuadTree();
		tc.determineNearestPtStation();
	}

	public static void main(String args[]){

		TestQuadTree tc = new TestQuadTree();
		tc.determineNearestPtStation();

	}
}
