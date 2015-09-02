/* *********************************************************************** *
 * project: org.matsim.*
 * Geometries.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.gis;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

/**
 * @author illenberger
 *
 */
public class Geometries {

	public static Geometry makePolygonFromCircle(double radius, double x, double y) {
		final int n = 100;
		Coordinate points[] = new Coordinate[n+1];
		for(int i = 0; i < n; i++) {
			double b = 2*Math.PI/(double)n * i;
			double x_i = Math.cos(b) * radius + x;
			double y_i = Math.sin(b) * radius + y;
			points[i] = new Coordinate(x_i, y_i);
		}
		points[n] = points[0];
		
		GeometryFactory factory = new GeometryFactory();
		LinearRing ring = factory.createLinearRing(points);
		return factory.createPolygon(ring, null);
	}
}
