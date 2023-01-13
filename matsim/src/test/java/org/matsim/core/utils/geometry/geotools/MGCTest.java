
/* *********************************************************************** *
 * project: org.matsim.*
 * MGCTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.utils.geometry.geotools;

import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.testcases.MatsimTestUtils;

/**
 *
 * @author laemmel
 *
 */
public class MGCTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();


	@Test public void testCoord2CoordinateAndViceVersa(){
		double x = 123.456789;
		double y = 987.654321;
		double delta = 0.0000001;
		Coord coord1 = new Coord(x, y);
		Coordinate coord2 = MGC.coord2Coordinate(coord1);
		Coord coord3 = MGC.coordinate2Coord(coord2);
		double x1 = coord3.getX();
		double y1 = coord3.getY();
		org.junit.Assert.assertEquals(x,x1,delta);
		org.junit.Assert.assertEquals(y,y1,delta);
	}

	@Test public void testCoord2PointAndViceVersa(){
		double x = 123.456789;
		double y = 987.654321;
		double delta = 0.0000001;
		Coord coord1 = new Coord(x, y);
		Point p = MGC.coord2Point(coord1);
		Coord coord2 = MGC.point2Coord(p);
		double x1 = coord2.getX();
		double y1 = coord2.getY();
		org.junit.Assert.assertEquals(x,x1,delta);
		org.junit.Assert.assertEquals(y,y1,delta);

	}

	@Test public void testGetUTMEPSGCodeForWGS84Coordinate() {
		{
			//Hamburg - should be UTM 32 North --> EPSG:32632
			double lat = 53.562021;
			double lon = 9.961533;
			String epsg = MGC.getUTMEPSGCodeForWGS84Coordinate(lon, lat);
			org.junit.Assert.assertEquals("EPSG:32632", epsg);
		}
		{
			//Cupertino - should be UTM 10 North --> EPSG:32610
			double lat = 37.333488;
			double lon  = -122.029710;
			String epsg = MGC.getUTMEPSGCodeForWGS84Coordinate(lon, lat);
			org.junit.Assert.assertEquals("EPSG:32610", epsg);
		}
		{
			//Padang - should be UTM 47 South --> EPSG:32747
			double lat = -0.959484;
			double lon  =  100.354052;
			String epsg = MGC.getUTMEPSGCodeForWGS84Coordinate(lon, lat);
			org.junit.Assert.assertEquals("EPSG:32747", epsg);
		}
	}

	@Test public void testGetCRS(){
		// CH1903_LV03 Id
		org.junit.Assert.assertNotNull(MGC.getCRS("EPSG:21781"));

		try {
			MGC.getCRS("");
			org.junit.Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException expected) { }

		// unknown EPSG Id
		try {
			MGC.getCRS("EPSG:MATSim");
			org.junit.Assert.fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException expected) { }

	}
}
