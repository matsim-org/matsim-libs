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

package org.matsim.interfaces.basic.v01;

/**
 * In MATSim, generally Cartesian Coordinates are used, with x increasing
 * to the right, and y increasing to the top:
 * 
 *     ^
 *   y |
 *     |     x
 *   (0/0) ---->
 */
public interface BasicCoord {

	public void setX(final double x); 

	public void setY(final double y);

	public void setXY(final double x, final double y);

	public double getX();

	public double getY();

}
