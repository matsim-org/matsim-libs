/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimCoordUtils.java
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
package playground.johannes.socialnetworks.sim.gis;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class MatsimCoordUtils {

	private final static GeometryFactory geoFacctory = new GeometryFactory();
	
	public static Point coordToPoint(Coord coord) {
		return geoFacctory.createPoint(new Coordinate(coord.getX(), coord.getY()));
	}
	
	public static Coord pointToCoord(Point point) {
		return new CoordImpl(point.getX(), point.getY());
	}
	
}
