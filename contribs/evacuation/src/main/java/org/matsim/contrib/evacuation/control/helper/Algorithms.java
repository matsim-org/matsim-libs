/* *********************************************************************** *
 * project: org.matsim.*
 * Algorithms.java
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

package org.matsim.contrib.evacuation.control.helper;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class Algorithms {
	
	private static final double epsilon = 0.00001;

	public static boolean computeLineIntersection(Coordinate a0, Coordinate a1, Coordinate b0, Coordinate b1, Coordinate intersectionCoordinate) {
		
		
		
		double a = (b1.x - b0.x) * (a0.y - b0.y) - (b1.y - b0.y) * (a0.x - b0.x);
		double b = (a1.x - a0.x) * (a0.y - b0.y) - (a1.y - a0.y) * (a0.x - b0.x);
		double denom = (b1.y - b0.y) * (a1.x - a0.x) - (b1.x - b0.x) * (a1.y - a0.y);
		
		//conincident
		if (Math.abs(a) < epsilon && Math.abs(b) < epsilon && Math.abs(denom) < epsilon) {
			intersectionCoordinate.x = (a0.x+a1.x) /2;
			intersectionCoordinate.y = (a0.y+a1.y) /2;
			return true;
		}
		
		//parallel
		if (Math.abs(denom) < epsilon) {
			return false;
		}
		
		double ua = a / denom;
		double ub = b / denom;
		
		if (ua < 0 || ua > 1 || ub < 0 || ub > 1) {
			return false;
		}
		
		double x = a0.x + ua * (a1.x - a0.x);
		double y = a0.y + ua * (a1.y - a0.y);
		intersectionCoordinate.x = x;
		intersectionCoordinate.y = y;
		
		return true;
	}



/**
 * Tests whether a polygon (defined by an array of Coordinate) contains a Coordinate
 * @param coord
 * @param p
 * @return true if coord lays within p
 */
public static boolean contains(Coordinate coord, Coordinate[] p) {
	int wn = getWindingNumber(coord,p);
	return wn != 0;
}

//winding number algorithm
//see softSurfer (www.softsurfer.com) for more details
private static int getWindingNumber(Coordinate c, Coordinate[] p) {


	int wn = 0;

	for (int i=0; i<p.length-1; i++) {
		if (p[i].y <= c.y) {
			if (p[i+1].y > c.y)
				if (isLeftOfLine( c,p[i], p[i+1]) > 0)
					++wn;
		}
		else {
			if (p[i+1].y <= c.y)
				if (isLeftOfLine( c,p[i], p[i+1]) < 0)
					--wn;
		}

		//test for early return here
	}
	return wn;
}

/**
 * tests whether coordinate c0 is located left of the infinite vector that runs through c1 and c2
 * @param c0 the coordinate to test
 * @param c1 one coordinate of the vector
 * @param c2 another coordinate of the same vector
 * @return >0 if c0 is left of the vector
 * 		  ==0 if c0 is on the vector
 * 		   <0 if c0 is right of the vector
 */
public static double isLeftOfLine(Coordinate c0, Coordinate c1, Coordinate c2) {
	return (c2.x - c1.x)*(c0.y - c1.y) - (c0.x - c1.x) * (c2.y - c1.y);
}

}
