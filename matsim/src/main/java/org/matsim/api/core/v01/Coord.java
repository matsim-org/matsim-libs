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
 * {@literal
 *     ^
 *   y |
 *     |     x
 *   (0/0) ---->
 * }
 */
public final class Coord implements Serializable {

	private static final long serialVersionUID = 1L;

	private double x;
	private double y;
	private double z = Double.NEGATIVE_INFINITY;

	/**
	 * Added this constructor so that Gson can again parse 2D coordinates from json files, e.g. via
	 * <p>
	 * Coord coord = new Gson().fromJson("{\"x\":123.0,\"y\":456.0}", Coord.class);
	 * </p>
	 * Without the constructor, the default values are ignored and the non-existing z-coordinate is initialized with the JAVA default of 0.0.
	 * {@linkplain Double}s would be initialized with {@code null}. This could also better indicate that there is indeed no information on the z-coordinate. However, memory consumption goes up.
	 * AN, 09/2016 
	 */
	public Coord() {
	}
	
	public Coord(final double x, final double y) {
		this.x = x;
		this.y = y;
		this.z = Double.NEGATIVE_INFINITY;
	}
	
	public Coord( double[] coord ) {
		this() ;
		switch ( coord.length ) {
			case 3:
				z = coord[2] ;
			case 2:
				x = coord[0] ; y = coord[1] ;
				break ;
			default:
				throw new RuntimeException( "double[] of wrong length; cannot be interpreted as coordinate ") ;
		}
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
	
	public double getZ() throws IllegalStateException {
		if ( !hasZ() ){
			throw new IllegalStateException("Requesting elevation (z) without having first set it.");
		}
		return this.z;
	}

	public boolean hasZ() {
		return this.z != Double.NEGATIVE_INFINITY;
	}


	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof Coord)) {
			return false;
		}
		Coord o = (Coord)other;
		
		if( !hasZ() ){
			/* this object is a 2D coordinate. */

			if ( o.hasZ() ) return false;

			/* both are 2D coordinates. */
			return (this.x == o.getX()) && (this.y == o.getY());
		}
		else {
			/* this object is 3D coordinate. */

			if ( !o.hasZ() ) return false;

			/* both objects are 3D coordinates. */
			return
					(this.x == o.getX()) &&
					(this.y == o.getY()) &&
					(this.z == o.getZ());
		}
	}

	
	@Override
	public int hashCode() {
		// Implementation based on chapter 3 of Joshua Bloch's "Effective Java"
		long xbits = Double.doubleToLongBits(this.x);
		long ybits = Double.doubleToLongBits(this.y);
		long zbits = Double.doubleToLongBits(this.z);
		int result = (int) (xbits ^ (xbits >>> 32));
		result = 31 * result + (int) (ybits ^ (ybits >>> 32));
		result = 31 * result + (int) (zbits ^ (zbits >>> 32));
		return result;
	}

	
	@Override
	public final String toString() {
		if( !hasZ() ){
			return "[x=" + this.x + " | y=" + this.y + "]";
		} else{
			return "[x=" + this.x + " | y=" + this.y + " | z=" + this.z + "]";
		}
	}
}
