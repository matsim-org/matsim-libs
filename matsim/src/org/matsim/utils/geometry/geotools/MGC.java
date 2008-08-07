/* *********************************************************************** *
 * project: org.matsim.*
 * MGC.java
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

package org.matsim.utils.geometry.geotools;

import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Converter factory for various conversion from Geotools to MATSim and vice versa.
 *
 * @author laemmel
 *
 */
public class MGC {

	public static final GeometryFactory geoFac = new GeometryFactory();

	/**
	 * Converts a MATSim {@link org.matsim.utils.geometry.Coord} into a Geotools <code>Coordinate</code>
	 * @param coord MATSim coordinate
	 * @return Geotools coordinate
	 */
	public static final Coordinate coord2Coordinate(final Coord coord) {
		return new Coordinate(coord.getX(), coord.getY());
	}

	/**
	 * Converts a Geotools <code>Coordinate</code> into a MATSim {@link org.matsim.utils.geometry.Coord}
	 * @param coord MATSim coordinate
	 * @return Geotools coordinate
	 */
	public static final Coord coordinate2Coord(final Coordinate coord) {
		return new CoordImpl(coord.x, coord.y);
	}

	/**
	 * Converts a MATSim {@link org.matsim.utils.geometry.Coord} into a Geotools <code>Point</code>
	 * @param coord MATSim coordinate
	 * @return Geotools point
	 */
	public static final Point coord2Point(final Coord coord) {
		return geoFac.createPoint(coord2Coordinate(coord));
	}

	/**
	 * Converts a Geotools <code>Point</code> into a MATSim {@link org.matsim.utils.geometry.Coord}
	 * @param point Geotools point
	 * @return MATSim coordinate
	 */
	public static final Coord point2Coord(final Point point) {
		return new CoordImpl(point.getX(), point.getY());
	}

}
