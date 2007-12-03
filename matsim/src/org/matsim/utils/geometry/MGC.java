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

package org.matsim.utils.geometry;

import org.matsim.utils.geometry.shared.Coord;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * Converter factory for various conversion from Geotools to Matsim and
 * vice versa 
 * 
 * @author laemmel
 *
 */
public class MGC {
	
	
	public static final GeometryFactory geoFac = new GeometryFactory();
	
	/**
	 * Converts a Matsim <code>CoordI</code> into a Geotools <code>Coordinate</code>
	 * @param Matsim coordinate
	 * @return Geotools coordinate
	 */
	public static final Coordinate coord2Coordinate(CoordI coord){
		return new Coordinate(coord.getX(),coord.getY());
	}

	/**
	 * Converts a Geotools <code>Coordinate</code> into a Matsim <code>CoordI</code> 
	 * @param Matsim coordinate
	 * @return Geotools coordinate
	 */
	public static final CoordI Coordinate2Coord(Coordinate coord){
		return new Coord(coord.x,coord.y);
	}

	/**
	 * Converts a Matsim <code>CoordI</code> into a Geotools <code>Point</code> 
	 * @param Matsim coordinate
	 * @return Geotools point
	 */
	public static final Point coord2Point(CoordI coord){
		return new Point(new CoordinateArraySequence(new Coordinate [] {coord2Coordinate(coord)}), geoFac);
	}
	
	/**
	 * Converts a Geotools <code>Point</code> into a Matsim <code>CoordI</code> 
	 * @param Geotools point
	 * @return Matsim coordinate
	 */
	public static final CoordI point2Coord(Point point){
		return new Coord(point.getX(),point.getY());
	}
	
}
