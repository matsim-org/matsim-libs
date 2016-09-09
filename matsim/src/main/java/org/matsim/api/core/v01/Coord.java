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
public final class Coord implements Serializable {

	private static final long serialVersionUID = 1L;

	private double x;
	private double y;
	private double z;

	public Coord(final double x, final double y) {
		this.x = x;
		this.y = y;
		this.z = Double.NEGATIVE_INFINITY;
	}
	
	
	public Coord(final double x, final double y, final double z){
		if(z == Double.NEGATIVE_INFINITY){
			throw new IllegalArgumentException("Double.NEGATIVE_INFINITY is an invalid elevation. " + 
					"If you want to ignore elevation, use Coord(x, y) constructor instead.");
		}
		this.x = x;
		this.y = y;
		this.z = z;
	}
	

	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}
	
	public double getZ() {
		if (this.z == Double.NEGATIVE_INFINITY){
			throw new RuntimeException("Requesting elevation (z) without having first set it."); 
		}
		return this.z;
	}


	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof Coord)) {
			return false;
		}
		Coord o = (Coord)other;
		
		if(this.z == Double.NEGATIVE_INFINITY){
			/* this object is a 2D coordinate. */
			@SuppressWarnings("unused")
			double oZ;
			try{
				oZ = o.getZ();
				/* other object is 3D coordinate. */
				return false;
			} catch (Exception e){
				/* both are 2D coordinates. */
				return ((this.x == o.getX()) && (this.y == o.getY()));
			}
		} else{
			/* this object is 3D coordinate. */
			@SuppressWarnings("unused")
			double oZ;
			try{
				oZ = o.getZ();
				/* both objects are 3D coordinates. */
				return (
						(this.x == o.getX()) && 
						(this.y == o.getY()) &&
						(this.z == o.getZ())
				);
			} catch (Exception e){
				/* other object is 2D coordinate. */
				return false;
			}
		}
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
		if(this.z == Double.NEGATIVE_INFINITY){
			return "[x=" + this.x + "][y=" + this.y + "]";
		} else{
			return "[x=" + this.x + "][y=" + this.y + "][z=" + this.z + "]";
		}
	}
}
