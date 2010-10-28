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

package playground.balmermi.world;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

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
 * @see AbstractLocation
 * @author Michael Balmer
 */
@Deprecated // use of current matsim zone object is discouraged; use geotools instead
public class Zone extends AbstractLocation {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private Coord min    = null;
	private Coord max    = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	@Deprecated // use of current matsim zone object is discouraged
	public Zone(final Id id, final Coord center, final Coord min, final Coord max) {
		super(id,center);
		this.setMin(min);
		this.setMax(max);
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
	 * @see playground.balmermi.world.AbstractLocation#calcDistance(org.matsim.api.core.v01.Coord)
	 * @return distance to that zone
	 */
	public final double calcDistance(final Coord coord) {
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
			CoordImpl refPt = new CoordImpl(minX, maxY);
			return CoordUtils.calcDistance(refPt, coord);
		} else if ((minX <= x) && (x <= maxX) && (maxY < y)) {
			// case 3
			CoordImpl refPt = new CoordImpl(x, maxY);
			return CoordUtils.calcDistance(refPt, coord);
		} else if ((maxX < x) && (maxY < y)) {
			// case 4
			CoordImpl refPt = new CoordImpl(maxX, maxY);
			return CoordUtils.calcDistance(refPt, coord);
		} else if ((x < minX) && (minY <= y) && (y <= maxY)) {
			// case 5
			CoordImpl refPt = new CoordImpl(minX, y);
			return CoordUtils.calcDistance(refPt, coord);
		} else if ((maxX < x) && (minY <= y) && (y <= maxY)) {
			// case 6
			CoordImpl refPt = new CoordImpl(maxX, y);
			return CoordUtils.calcDistance(refPt, coord);
		} else if ((x < minX) && (y < minY)) {
			// case 7
			CoordImpl refPt = new CoordImpl(minX, minY);
			return CoordUtils.calcDistance(refPt, coord);
		} else if ((minX <= x) && (x <= maxX) && (y < minY)) {
			// case 8
			CoordImpl refPt = new CoordImpl(x, minY);
			return CoordUtils.calcDistance(refPt, coord);
		} else if ((maxX < x) && (y < maxY)) {
			// case 9
			CoordImpl refPt = new CoordImpl(maxX, minY);
			return CoordUtils.calcDistance(refPt, coord);
		} else {
			Gbl.errorMsg("This should never happen!");
			return Double.NaN;
		}
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

	@Deprecated // use of current matsim zone object is discouraged; use geotools instead
	public final boolean contains(final Coord coord) {
		double x = coord.getX();
		double y = coord.getY();
		if ((this.min.getX() <= x) && (x <= this.max.getX()) && (this.min.getY() <= y) && (y <= this.max.getY())) {
			// case 1
			return true;
		}
		return false;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setMin(final Coord min) {
		if ((min != null) && (min.getX() <= this.center.getX()) && (min.getY() <= this.center.getY())) { this.min = min; }
		else { this.min = this.center; }
	}

	public final void setMax(final Coord max) {
		if ((max != null) && (max.getX() >= this.center.getX()) && (max.getY() >= this.center.getY())) { this.max = max; }
		else { this.max = this.center; }
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final Coord getMin() {
		return this.min;
	}

	public final Coord getMax() {
		return this.max;
	}


	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString() +
				"[min=" + this.min + "]" +
				"[max=" + this.max + "]";
		}

}
