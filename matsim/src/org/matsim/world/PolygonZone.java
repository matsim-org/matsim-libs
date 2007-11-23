/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonZone.java
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

import org.apache.log4j.Logger;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;

/**
 * A geographical object in MATSim. It describes a zone as a polygon. The
 * coordinates <code>center</code> and <code>shell[]</code> are always
 * defined. <code>center</code> has to be within the polygon defined by
 * <code>shell []</code>.<br>
 * As the coordinates could be present in any coordinate systems (e.g.
 * spherical, like transverse mercator), that the area of the zone does not have
 * to be the area of the polygon.
 * 
 * @see Location
 * @author laemmel
 */
public class PolygonZone extends Location {

	private static final Logger log = Logger.getLogger(PolygonZone.class);

	private CoordI[] shell = null;
	private double area = Double.NaN;

	private String name;

	protected PolygonZone(final PolygonZoneLayer layer, final IdI id, final CoordI center, final CoordI[] shell,
			final double area, final String name) {
		super(layer, id, center);
		this.shell = setShell(shell);
		this.area = area;
		this.name = name;

	}

	private CoordI[] setShell(CoordI[] shell) {
		if (!shell[0].equals(shell[shell.length - 1])) {
			log.warn("The first and the last coordinate of a polygon have to be equal! Automatically closing the polygon now.");
			return closePolygon(shell);
		}
		return shell;
	}

	// ////////////////////////////////////////////////////////////////////
	// query methods
	// ////////////////////////////////////////////////////////////////////

	public final boolean contains(final CoordI coord) {

		// lets reinvent the wheel ...
		int counter = 0;
		double xinters;
		CoordI c1, c2;
		c1 = this.shell[0];
		for (int i = 1; i < this.shell.length; i++) {
			c2 = this.shell[i];
			if ((coord.getY() > Math.min(c1.getY(), c2.getY())) 
					&& (coord.getY() <= Math.max(c1.getY(), c2.getY()))
					&& (coord.getX() <= Math.max(c1.getX(), c2.getX())) 
					&& (c1.getY() != c2.getY())) {
				xinters = (coord.getY() - c1.getY()) * (c2.getY() - c1.getX()) / (c2.getY() - c1.getY()) + c1.getX();
				if (c1.getX() == c2.getX() || coord.getX() <= xinters)
					counter++;
			}
			c1 = c2;
		}
		return (counter % 2 != 0);
	}

	public double getArea() {
		return this.area;
	}

	public String getName() {
		return this.name;
	}

	public CoordI[] getShell() {
		return this.shell;
	}

	@Override
	public double calcDistance(CoordI coord) {
		// TODO Auto-generated method stub
		return 0;
	}

	private CoordI[] closePolygon(CoordI[] shell) {
		CoordI[] enlShell = new CoordI[shell.length + 1];
		System.arraycopy(shell, 0, enlShell, 0, shell.length);
		enlShell[shell.length] = shell[0];
		return enlShell;
	}

}
