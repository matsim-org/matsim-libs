/* *********************************************************************** *
 * project: org.matsim.*
 * CoordI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.geometry;

/*
 * In MATSim, generally Cartesian Coordinates are used, with x increasing
 * to the right, and y increasing to the top:
 * 
 *     ^
 *   y |
 *     |     x
 *   (0/0) ---->
 */

public interface Coord {

	public void setX(final double x); 

	public void setY(final double y);

	public void setXY(final double x, final double y);

	public double getX();

	public double getY();


	/* 
	 * I have deactivated the following functions, as I am not sure if
	 * they should be part of an Interface. The functions would be useful
	 * in anycase, but they may blow up the interface behind the scope
	 * of the interface.
	 * requiring "equals()" to be implemented could help preventing errors
	 * "calcDistance" is ``nice to have''
	 * maybe both functions could be implemented in a static utils class,
	 * e.g. "CartesianCoordUtils", which would implement several math.
	 * operators for CoordI-Objects like test for equality, subtraction
	 * (=calcDistance) and other often-used functions for Coords.
	 * [Rie, 2006-08-29]
	 */
// 	public boolean equals(Object o);
// 	
 	public double calcDistance(final Coord other);
	
}
