/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.networkEditor.visualizing;

/**
 * This is a re-implementation of {@link Coord}, but with setters.  It can be used for certain computations.  It is deliberately not related
 * to {@link Coord}, i.e. not under the same interface and not inherited, because we want to make sure that if we get a Coord in the
 * core classes, it is immutable.  And not some mutable object behind a seemingly immutable interface.  kai, jul'16
 * 
 * @author nagel
 *
 */
public class MutableCoord {

	private double x;
	private double y;

	public MutableCoord(final double x, final double y) {
		this.x = x;
		this.y = y;
	}
	public MutableCoord( org.matsim.api.core.v01.Coord coord ) {
		this.x = coord.getX() ;
		this.y = coord.getY() ;
	}


	public double getX() {
		return this.x;
	}

	public double getY() {
		return this.y;
	}

	public void setX(final double x) {
		this.x = x;
	}

	public void setY(final double y) {
		this.y = y;
	}

	public void setXY(final double x, final double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof MutableCoord)) {
			return false;
		}
		MutableCoord o = (MutableCoord)other;
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

}
