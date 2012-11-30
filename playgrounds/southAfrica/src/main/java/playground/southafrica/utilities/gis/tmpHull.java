/* *********************************************************************** *
 * project: org.matsim.*
 * tmpHull.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.utilities.gis;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class tmpHull {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GeometryFactory gf = new GeometryFactory();

		Coord matsimCoord = new CoordImpl(1.0, 2.0);
		Coordinate c = new Coordinate(matsimCoord.getX(), matsimCoord.getY());
		
		// TODO Find a way to get these from the concave hull algorithm.
		Coordinate[] hullCoordinates = {c}; 
		LinearRing lr = gf.createLinearRing(hullCoordinates);
		Polygon facilityPolygon = gf.createPolygon(lr, null);
	}

}

