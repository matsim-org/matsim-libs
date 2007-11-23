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

package org.matsim.world;

import java.io.Serializable;

import org.matsim.utils.geometry.CoordI;

// should probably in util. balmermi
public class Coord implements Serializable, CoordI {

	private static final long serialVersionUID = -6600222561096905676L;

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private double x;
	private double y;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Coord(final String x, final String y) {
		this.set(Double.parseDouble(x),Double.parseDouble(y));
	}

	public Coord(final double x, final double y) {
		this.set(x,y);
	}

	public Coord(final CoordI coord) {
		this.set(coord.getX(), coord.getY());
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void set(double x, double y) {
		if (Double.isNaN(x) || Double.isNaN(y)) {
			throw new NumberFormatException("A coordinate must define both x and y.");
		}
		this.x = x;
		this.y = y;
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final boolean equals(Object o) {
		if ((o instanceof Coord) && (((Coord)o).getX() == this.getX()) && (((Coord)o).getY() == this.getY())) {
			return true;
		}
		return false;
	}

	@Override
	public final int hashCode() {
		return (int)Double.doubleToLongBits(this.x - this.y);
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	public final double calcDistance(final CoordI other) {
		double xDiff = other.getX()-this.x;
		double yDiff = other.getY()-this.y;
		return Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setX(final double x) {
		this.set(x,this.y);
	}

	public final void setY(final double y) {
		this.set(this.x,y);
	}

	public final void setXY(final double x, final double y) {
		this.set(x,y);
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final double getX() {
		return this.x;
	}

	public final double getY() {
		return this.y;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[x=" + this.x + "]" +
			"[y=" + this.y + "]";
	}
}
