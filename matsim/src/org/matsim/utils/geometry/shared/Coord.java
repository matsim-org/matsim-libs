/* *********************************************************************** *
 * project: org.matsim.*
 * Coord.java
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

package org.matsim.utils.geometry.shared;

import java.io.Serializable;

import org.matsim.utils.geometry.CoordI;

public class Coord implements Serializable, CoordI {

	private static final long serialVersionUID = 1L;

	private double x = 0.0;
	private double y = 0.0;

	public Coord(final double x, final double y) {
		this.x = x;
		this.y = y;
	}

	public Coord(final String x, final String y) {
		this(Double.parseDouble(x), Double.parseDouble(y));
	}

	public Coord(CoordI coord) {
		this.x = coord.getX();
		this.y = coord.getY();
	}

	public final double calcDistance(final CoordI other) {
		//depending on the coordinate system that is used, determining the 
		//distance based on the euclidean distance will lead to wrong results.
		//however, if the distance is not to large (<1km) this will be a usable distance estimation.
		//Another comfortable way to calculate correct distances would be, to use the distance functions
		//provided by geotools lib. May be we need to discuss what part of GIS functionality we should implement 
		//by our own and for what part we could use an existing GIS like geotools. We need to discuss this in terms
		//of code robustness, performance and so on ... [gl]
		double xDiff = other.getX()-this.x;
		double yDiff = other.getY()-this.y;
		return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void setXY(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public final String toString() {
		return "[x=" + this.x + "][y=" + this.y + "]";
	}
}
