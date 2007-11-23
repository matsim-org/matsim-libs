/* *********************************************************************** *
 * project: org.matsim.*
 * Zone.java
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

import org.matsim.gbl.Gbl;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;

/**
 * A geographical object in MATSim. It describes a zone as a rectangle.
 * The coordinates <code>center</code>, <code>min</code> and <code>max</code> are
 * always defined and fulfills the following constraints:<br>
 * minX <= centerX <= maxX<br>
 * minY <= centerY <= maxY<br>
 * If <code>min</code> and/or <code>max</code> are not set, it will be automatically set to
 * the center coordinate.<br>
 * Note, that the area of the zone does not have to be the area of the rectangle. It just need
 * to be greater or equal than zero.
 * @see Location
 * @author Michael Balmer
 */
public class Zone extends Location {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private CoordI min    = null;
	private CoordI max    = null;
	private double area  = Double.NaN;
	private String name  = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	protected Zone(final ZoneLayer layer, final IdI id, final CoordI center) {
		super(layer,id,center);
		this.setMin(center);
		this.setMax(center);
		this.setArea(0.0);
		this.setName(null);
	}
	
	protected Zone(final ZoneLayer layer, final IdI id, final CoordI center,
	               final CoordI min, final CoordI max, final double area, final String name) {
		super(layer,id,center);
		this.setMin(min);
		this.setMax(max);
		this.setArea(area);
		this.setName(name);
	}
	
	protected Zone(final ZoneLayer layer, final String id, final CoordI center,
	               final CoordI min, final CoordI max, final String area, final String name) {
		super(layer,id,center);
		this.setMin(min);
		this.setMax(max);
		this.setArea(area);
		this.setName(name);
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	/**
	 * <p>Calculates the distance from coordinate <code>coord</code> to that zone.
	 * Depending on the position of <code>coord</code> it returns (i) zero, if
	 * lies within the zone (inclusive minimum and maximum), (ii) the distance
	 * to the nearest corner or (iii) the distance to the nearest facet.</p>
	 * <p>Code details:<br>
	 * The different cases are numbered as followed:<br>
	 * <code>
	 * ......|.....|.......         1: within min/max-area<br>
	 * ...2..|..3..|..4....         2: outside on top-left<br>
	 * ..----+-----+-----..         3: outside on top<br>
	 * ......|.....|.......         4: outside on top-right<br>
	 * ...5..|..1..|..6....         5: outside on left<br>
	 * ......|.....|.......         6: outside on right<br>
	 * ..----+-----+-----..         7: outside on bottom-left<br>
	 * ...7..|..8..|..9....         8: outside on bottom<br>
	 * ......|.....|.......         9: outside on bottom-right<br>
	 * </code>
	 * </p>
	 * 
	 * @param coord
	 * @see org.matsim.world.Location#calcDistance(org.matsim.utils.geometry.CoordI)
	 * @return distance to that zone
	 */
	@Override
	public final double calcDistance(final CoordI coord) {
		double x = coord.getX();
		double y = coord.getY();
		double minX = this.min.getX();
		double minY = this.min.getY();
		double maxX = this.max.getX();
		double maxY = this.max.getY();

		if ((minX <= x) && (x <= maxX) && (minY <= y) && (y <= maxY)) {
			// case 1
			return 0.0;
		} else if ((x < minX) && (maxY < y)) {
			// case 2
			Coord refPt = new Coord(minX, maxY);
			return refPt.calcDistance(coord);
		} else if ((minX <= x) && (x <= maxX) && (maxY < y)) {
			// case 3
			Coord refPt = new Coord(x, maxY);
			return refPt.calcDistance(coord);
		} else if ((maxX < x) && (maxY < y)) {
			// case 4
			Coord refPt = new Coord(maxX, maxY);
			return refPt.calcDistance(coord);
		} else if ((x < minX) && (minY <= y) && (y <= maxY)) {
			// case 5
			Coord refPt = new Coord(minX, y);
			return refPt.calcDistance(coord);
		} else if ((maxX < x) && (minY <= y) && (y <= maxY)) {
			// case 6
			Coord refPt = new Coord(maxX, y);
			return refPt.calcDistance(coord);
		} else if ((x < minX) && (y < minY)) {
			// case 7
			Coord refPt = new Coord(minX, minY);
			return refPt.calcDistance(coord);
		} else if ((minX <= x) && (x <= maxX) && (y < minY)) {
			// case 8
			Coord refPt = new Coord(x, minY);
			return refPt.calcDistance(coord);
		} else if ((maxX < x) && (y < maxY)) {
			// case 9
			Coord refPt = new Coord(maxX, minY);
			return refPt.calcDistance(coord);
		} else {
			Gbl.errorMsg("This should never happen!");
			return Double.NaN;
		}
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

	public final boolean contains(final CoordI coord) {
		double x = coord.getX();
		double y = coord.getY();
		if ((this.min.getX() <= x) && (x <= this.max.getX()) && (this.min.getY() <= y) && (y <= this.max.getY())) {
			// case 1
			return true;
		}
		return false;
	}

	//////////////////////////////////////////////////////////////////////
	// reset methods
	//////////////////////////////////////////////////////////////////////

	// even it is written by myself, i do not like that...
	// especially about the center. I start thinking that a center must be always defined.
	// that would help in many aspects... well... will see. balmermi
//	public final void resetAttributes() {
//		this.center = null;
//		this.min = null;
//		this.max = null;
//		this.area = Double.NaN;
//	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setArea(final double area) {
		if (area < 0.0) { throw new NumberFormatException("An area has to be >= 0.0"); }
		this.area = area;
	}

	public final void setArea(final String area) {
		if (area != null) { this.setArea(Double.parseDouble(area)); }
	}

	public final void setName(final String name) {
		this.name = name;
	}

	//////////////////////////////////////////////////////////////////////

	public final void setMin(final CoordI min) {
		if ((min != null) && (min.getX() <= this.center.getX()) && (min.getY() <= this.center.getY())) { this.min = min; }
		else { this.min = center; }
	}

	public final void setMin(final double x, final double y) {
		this.setMin(new Coord(x,y));
	}

	protected final void setMin(final String x, final String y) {
		if ((x != null) && (y != null)) { this.setMin(new Coord(x,y)); }
	}

	//////////////////////////////////////////////////////////////////////

	public final void setMax(final CoordI max) {
		if ((max != null) && (max.getX() >= this.center.getX()) && (max.getY() >= this.center.getY())) { this.max = max; }
		else { this.max = center; }
	}

	public final void setMax(final double x, final double y) {
		this.setMax(new Coord(x,y));
	}

	protected final void setMax(final String x, final String y) {
		if ((x != null) && (y != null)) { this.setMax(new Coord(x,y)); }
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final CoordI getMin() {
		return this.min;
	}

	public final CoordI getMax() {
		return this.max;
	}

	public final double getArea() {
		return this.area;
	}

	public final String getName() {
		return this.name;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString() +
				"[min=" + this.min + "]" +
				"[max=" + this.max + "]" +
				"[area=" + this.area + "]" +
				"[name=" + this.name + "]";
	}
}
