/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package org.matsim.core.network.algorithms.intersectionSimplifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class HullConverterTest {

	public void testConvert() {

	}

	/**
	 * 	   4           3
	 *   (5,0)-------(5,5)
	 *     |           |
	 *     |           |
	 *     |           |
	 *     |           |
	 *   (0,0)-------(5,0)
	 *     1           2
	 */
	@Test
	void testConvertString(){
		/* Must pass a Geometry. */
		Object o = new Integer(0);
		HullConverter hc = new HullConverter();
		String s = hc.convertToString(o);
		Assertions.assertTrue(s.isEmpty(), "Should receive empty string");
		
		/* Check String. */
		GeometryFactory gf = new GeometryFactory();
		Coordinate[] ca = new Coordinate[5];
		ca[0] = new Coordinate(0.0, 0.0);
		ca[1] = new Coordinate(5.0, 0.0);
		ca[2] = new Coordinate(5.0, 5.0);
		ca[3] = new Coordinate(0.0, 5.0);
		ca[4] = ca[0];
		
		/* Point */
		Point point = gf.createPoint(ca[0]);
		s = hc.convertToString(point);
		String pointString = "(0.0;0.0)";
		Assertions.assertTrue(pointString.equalsIgnoreCase(s), "Wrong string for point.");
		
		/* Line */
		Coordinate[] ca2 = new Coordinate[2];
		ca2[0] = ca[0];
		ca2[1] = ca[1];
		LineString line = gf.createLineString(ca2);
		s = hc.convertToString(line);
		String lineString = "(0.0;0.0),(5.0;0.0)";
		Assertions.assertTrue(lineString.equalsIgnoreCase(s), "Wrong string for line.");
		
		/* Polygon */
		Polygon polygon = gf.createPolygon(ca);
		s = hc.convertToString(polygon);
		String polygonString = "(0.0;0.0),(5.0;0.0),(5.0;5.0),(0.0;5.0),(0.0;0.0)"; 
		Assertions.assertTrue(polygonString.equalsIgnoreCase(s), "Wrong string for polygon.");
	}


	/**
	 *   (5,0)-------(5,5)
	 *     |           |
	 *     |           |
	 *     |           |
	 *     |           |
	 *   (0,0)-------(5,0)
	 */
	@Test
	void testConstructor(){
		HullConverter hc = new HullConverter();
		GeometryFactory gf = new GeometryFactory();
		Coordinate[] ca = new Coordinate[5];
		ca[0] = new Coordinate(0.0, 0.0);
		ca[1] = new Coordinate(5.0, 0.0);
		ca[2] = new Coordinate(5.0, 5.0);
		ca[3] = new Coordinate(0.0, 5.0);
		ca[4] = ca[0];
		
		/* Point */
		Point point = gf.createPoint(ca[0]);
		Assertions.assertEquals(point, hc.convert(hc.convertToString(point)), "Wrong point.");
		
		/* Line */
		Coordinate[] ca2 = new Coordinate[2];
		ca2[0] = ca[0];
		ca2[1] = ca[1];
		LineString line = gf.createLineString(ca2);
		Assertions.assertEquals(line, hc.convert(hc.convertToString(line)), "Wrong line.");
		
		/* Polygon */
		Polygon polygon = gf.createPolygon(ca);
		Assertions.assertEquals(polygon, hc.convert(hc.convertToString(polygon)), "Wrong polygon.");
	}
	

}
