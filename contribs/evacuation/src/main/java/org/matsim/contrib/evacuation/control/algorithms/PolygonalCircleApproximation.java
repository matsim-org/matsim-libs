/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonalCircleApproximation.java
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

package org.matsim.contrib.evacuation.control.algorithms;

import org.geotools.geometry.jts.JTS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public abstract class PolygonalCircleApproximation {
	private static final GeometryFactory geofac = new GeometryFactory();
	private static final double INCR = Math.PI/16;
	
	public static Polygon getPolygonFromGeoCoords(Coordinate c0, Coordinate c1) {

		
		double x = c1.x - c0.x;
		double y = c1.y - c0.y;
		
		x = Math.hypot(x, y);
		y = 0;
		
		int steps =(int) (0.5+Math.PI*2/INCR);
		Coordinate [] coords = new Coordinate[steps+1];
		int idx =0;
		coords[idx++] = new Coordinate(x+c0.x,y+c0.y); 
		for (double alpha = 0 +INCR; alpha < 2*Math.PI; alpha+=INCR){
			double tmpX = x * Math.cos(alpha) - y * Math.sin(alpha);
			double tmpY = x * Math.sin(alpha) + y * Math.cos(alpha);
			Coordinate c = new Coordinate(tmpX+c0.x,tmpY+c0.y);
			coords[idx++]=c;
 		}
		coords[idx] = coords[0];
		
		LinearRing ls = geofac.createLinearRing(coords);
		Polygon poly = geofac.createPolygon(ls,null);
		return poly;
	}

	
	public static void transform(Coordinate c0, MathTransform transform) {
		try {
			JTS.transform(c0, c0, transform);
		} catch (TransformException e) {
			e.printStackTrace();
		}
	}
	
	public static Geometry transform(Polygon p, MathTransform transform) {
		try {
			return JTS.transform(p, transform);
		} catch (TransformException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
}
