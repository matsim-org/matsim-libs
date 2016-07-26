/* *********************************************************************** *
 * project: org.matsim.contrib.networkEditor
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 Daniel Ampuero
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

package org.matsim.contrib.networkEditor.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.networkEditor.visualizing.MutableCoord;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author danielmaxx
 */
public class GeometryTools {

	static public Point getCentroid(Polygon poly){
		return poly.getCentroid();
	}

	static public double getArea(Polygon poly) {
		return poly.getArea();
	}

	static public boolean intersects(LinearRing ring, LineSegment segment) {
		Coordinate coordinates[] = ring.getCoordinates();
		final int points = coordinates.length;
		RobustLineIntersector intersector = new RobustLineIntersector();
		for(int i=0; i<coordinates.length; ++i) {
			LineSegment aux = new LineSegment(coordinates[i], coordinates[(i+1)%points]);
			intersector.computeIntersection(segment.p0, segment.p1, aux.p0, aux.p1);
			if(intersector.hasIntersection()) {
				return true;
			}
		}
		return false;
	}

	static public boolean intersectRectangle(Coordinate p1, Coordinate p2, LineSegment segment) {
		return intersects(getRectangle(p1, p2), segment);
	}

	static public boolean isInside(LinearRing ring, LineSegment segment) {
		final boolean p1 = CGAlgorithms.isPointInRing(segment.p0, ring.getCoordinates());
		final boolean p2 = CGAlgorithms.isPointInRing(segment.p1, ring.getCoordinates());
		return p1&&p2;
	}

	static public LinearRing getRectangle(Coordinate p1, Coordinate p2) {
		if(p1.x > p2.x) {
			Coordinate aux = p1;
			p1 = p2;
			p2 = aux;
		}
		if(p1.y < p2.y) {
			double aux = p2.y;
			p2.y = p1.y;
			p1.y = aux;
		}
		Coordinate rectangle[] = new Coordinate[5];
		rectangle[0] = p1;
		rectangle[1] = new Coordinate(p2.x, p1.y);
		rectangle[2] = p2;
		rectangle[3] = new Coordinate(p1.x, p2.y);
		rectangle[4] = p1;
		return new GeometryFactory().createLinearRing(rectangle);
	}

	static public LinearRing featureToLinearRing(SimpleFeature ft) {
		return (LinearRing)ft.getDefaultGeometry();
	}

	static public java.awt.Polygon toJavaPolygon(LinearRing ring){
		java.awt.Polygon poly = new java.awt.Polygon();
		Coordinate coordinates[] = ring.getCoordinates();
		for(int i=0; i<coordinates.length; ++i)
			poly.addPoint((int)coordinates[i].x, (int)coordinates[i].y);
		return poly;
	}

	static public Coordinate MATSimCoordToCoordinate(Coord c) {
		return new Coordinate(c.getX(), c.getY());
	}
	static public Coordinate MATSimCoordToCoordinate(MutableCoord c) {
		return new Coordinate(c.getX(), c.getY());
	}

}
