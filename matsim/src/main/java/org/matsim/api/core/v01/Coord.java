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
public class Coord implements Serializable {

	private static final long serialVersionUID = 1L;

	private double x;
	private double y;
	private double z;
	private boolean hasZ = false;

	public Coord(final double x, final double y) {
		this.x = x;
		this.y = y;		
	}
	
	public Coord (final double x, final double y, final double z){
		this.x = x;
		this.y = y;
		this.z = z;
		this.hasZ = true;
	}


	public double getX() {
		return this.x;
	}

	public double getY() {
		return this.y;
	}
	
	public double getZ() {
		if(!hasZ) throw new IllegalStateException("Coord has no Z component defined.");
		return z;
	}
	
	public boolean hasZ(){
		return this.hasZ;
	}

	public void setX(final double x) {
		this.x = x;
	}

	public void setY(final double y) {
		this.y = y;
	}
	
	public void setZ(final double z) {
		this.z = z;
		this.hasZ = true;
	}

	public void setXY(final double x, final double y) {
		this.x = x;
		this.y = y;
	}
	
	public void setXYZ(final double x, final double y, final double z){
		this.x = x;
		this.y = y;
		this.z = z;
		this.hasZ = true;
	}

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


}
