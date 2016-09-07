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

package org.matsim.api.core.v01;

import java.io.Serializable;

import org.matsim.core.scenario.Lockable;

/**
 * In MATSim, generally Cartesian Coordinates are used, with x increasing
 * to the right, and y increasing to the top:
 * <pre>
 *     ^
 *   y |
 *     |     x
 *   (0/0) ---->
 * </pre>
 */
public final class Coord implements Serializable, Lockable {

	private static final long serialVersionUID = 1L;

	private double x;
	private double y;

	private boolean locked = false ;

	public Coord(final double x, final double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return this.x;
	}
	public double getY() {
		return this.y;
	}

//	public void setX(final double x) {
//		testForLocked() ;
//		this.x = x;
//	}

//	public void setY(final double y) {
//		testForLocked() ;
//		this.y = y;
//	}

//	public void setXY(final double x, final double y) {
//		testForLocked() ;
//		this.x = x;
//		this.y = y;
//	}

	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof Coord)) {
			return false;
		}
		Coord o = (Coord)other;
		return ((this.x == o.getX()) && (this.y == o.getY()));
	}

	@Override
	public int hashCode() {
		// Implementation based on chapter 3 of Joshua Bloch's "Effective Java"
		long xbits = Double.doubleToLongBits(this.x);
		long ybits = Double.doubleToLongBits(this.y);
		int result = (int) (xbits ^ (xbits >>> 32));
		result = 31 * result + (int) (ybits ^ (ybits >>> 32));
		return result;
	}

	@Override
	public final String toString() {
		return "[x=" + this.x + "][y=" + this.y + "]";
	}


	@Override
	public void setLocked() {
		this.locked = true ;
	}
	private void testForLocked() {
		if ( locked ) {
			throw new RuntimeException( "Coord is locked; too late to do this.  See comments in code.") ;
		}
	}



}
