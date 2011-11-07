/* *********************************************************************** *
 * project: org.matsim.*
 * CoordAnalyzer.java
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

package playground.christoph.evacuation.analysis;

import org.matsim.api.core.v01.Coord;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class CoordAnalyzer {

	private final Geometry affectedArea;
	private final GeometryFactory factory;
	
	public CoordAnalyzer(Geometry affectedArea) {
		this.affectedArea = affectedArea;
		
		this.factory = new GeometryFactory();
	}
		
	public boolean isCoordAffected(Coord coord) {
		
		Point point = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
		
		return affectedArea.contains(point);		
	}
	
//	public boolean isCoordRescueArea(Coord coord) {
//		return isInside(coord, rescueAreas);
//	}
//	
//	private boolean isInside(Coord coord, Set<Feature> features) {
//		
//		Coord transformedCoord = transform.transform(coord);
//		Point point = MGC.coord2Point(transformedCoord);
//		
//		/*
//		 * check if point is inside
//		 */
//		for (Feature feature : features) {
//			Geometry polygon = feature.getDefaultGeometry();
//			if (polygon.contains(point)) {
//				return true;
//			}
//		}
//		
//		return false;
//	}
}
