/* *********************************************************************** *
 * project: org.matsim.*
 * Coord3dImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.core.utils.geometry;


import playground.christoph.evacuation.api.core.v01.Coord3d;

public class Coord3dImpl extends CoordImpl implements Coord3d {

	private static final long serialVersionUID = 1L;
	
	private double z;

	public Coord3dImpl(final double x, final double y, final double z) {
		super(x, y);
		this.z = z;
	}

	public Coord3dImpl(final String x, final String y, final String z) {
		this(Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z));
	}

	public Coord3dImpl(final Coord3d coord) {
		this(coord.getX(), coord.getY(), coord.getZ());
	}
	
	@Override
	public void setZ(double z) {
		this.z = z;
	}

	@Override
	public void setXYZ(double x, double y, double z) {
		this.setX(x);
		this.setY(y);
		this.z = z;
	}

	@Override
	public double getZ() {
		return this.z;
	}

	@Override
	public boolean equals(final Object other) {
		if (!super.equals(other)) return false;
		if (!(other instanceof Coord3d)) {
			return false;
		}
		Coord3d o = (Coord3d)other;
		return (this.getZ() == o.getZ());
	}
	
	// TODO: remove final from CoordImpl
//	@Override
//	public final String toString() {
//		return "[x=" + this.getX() + "][y=" + this.getY() + "][z=" + this.getZ() + "]";
//	}
}