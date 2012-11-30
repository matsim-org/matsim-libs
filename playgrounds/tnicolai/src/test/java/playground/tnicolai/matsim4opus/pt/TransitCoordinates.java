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
package playground.tnicolai.matsim4opus.pt;

import java.util.Collection;
import java.util.Iterator;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author thomas
 *
 */
public class TransitCoordinates {
	
	private Coord home, work;
	private Coord pt1a, pt1b, pt2a, pt2b, pt3a, pt3b;
	private QuadTree<String> qTree;
	
	public TransitCoordinates(){
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
		
		home = new CoordImpl(150., 500.);
		work = new CoordImpl(900., 100.);
		
		pt1a = new CoordImpl(200., 550.);
		pt1b = new CoordImpl(200., 900.);
		pt2a = new CoordImpl(100., 450.);
		pt2b = new CoordImpl(900., 450.);
		pt3a = new CoordImpl(150., 420.);
		pt3b = new CoordImpl(880., 100.);
		
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
		
		Collection<String> ptColHome = qTree.get(home.getX(), home.getY(), distance);
		Iterator<String> homeIt = ptColHome.iterator();
		System.out.println("Pt stations in " + distance + "m distance to home location:");
		while(homeIt.hasNext())
			System.out.println(homeIt.next());
		
		Collection<String> ptColWork = qTree.get(work.getX(), work.getY(), distance);
		Iterator<String> workIt = ptColWork.iterator();
		System.out.println("Pt stations in " + distance + "m distance to work location:");
		while(workIt.hasNext())
			System.out.println(workIt.next());
	}
	
	public static void main(String args[]){
		
		TransitCoordinates tc = new TransitCoordinates();
		tc.determineNearestPtStation();
		
	}
}
