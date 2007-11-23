/* *********************************************************************** *
 * project: org.matsim.*
 * CoordWGS84Test.java
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

package org.matsim.utils.geometry.shared;

import org.matsim.testcases.MatsimTestCase;

public class CoordWGS84Test extends MatsimTestCase {

	public void testcreateFromCH1903DoubleDouble() {

		double realX = 200000;
		double realY = 600000;

		CoordWGS84 originOfCH1903System = CoordWGS84.createFromCH1903(realX, realY);

		double expectedCH1903X = 200000;
		double deltaCH1903X = 1;
		double expectedCH1903Y = 600000;
		double deltaCH1903Y = 1;
		double expectedLongitude = 7.438637222222222;
		double deltaLongitude = 0.00000001;
		double expectedLatitude = 46.95108111111111;
		double deltaLatitude = 0.00000001;

		double actualX = originOfCH1903System.getXCH1903();
		double actualY = originOfCH1903System.getYCH1903();
		junit.framework.Assert.assertEquals(expectedCH1903X, actualX, deltaCH1903X);
		junit.framework.Assert.assertEquals(expectedCH1903Y, actualY, deltaCH1903Y);

		double actualLongitude = originOfCH1903System.getLongitude();
		double actualLatitude = originOfCH1903System.getLatitude();
		junit.framework.Assert.assertEquals(expectedLongitude, actualLongitude, deltaLongitude);
		junit.framework.Assert.assertEquals(expectedLatitude, actualLatitude, deltaLatitude);

		// the CH1903 system does not really work outside Switzerland
		// we show it in an example where x and y are (accidentally) exchanged

		double expectedNotBerneX = 200043;
		double expectedNotBerneY = 600030;
		CoordWGS84 notBerne = CoordWGS84.createFromCH1903(realY, realX);

		actualX = notBerne.getXCH1903();
		actualY = notBerne.getYCH1903();
		junit.framework.Assert.assertEquals(expectedNotBerneX, actualY, deltaCH1903Y);
		junit.framework.Assert.assertEquals(expectedNotBerneY, actualX, deltaCH1903X);
	}

	public void testCreateFromWGS84DoubleDouble() {

		double realLongitude = 8.0;
		double realLatitude = 47.0;

		CoordWGS84 somewhereCloseToLucerne = CoordWGS84.createFromWGS84(realLongitude, realLatitude);

		double expectedLongitude = 8.0;
		double deltaLongitude = 0.00000001;
		double expectedLatitude = 47.0;
		double deltaLatitude = 0.00000001;

		double actualLongitude = somewhereCloseToLucerne.getLongitude();
		double actualLatitude = somewhereCloseToLucerne.getLatitude();
		junit.framework.Assert.assertEquals(expectedLongitude, actualLongitude, deltaLongitude);
		junit.framework.Assert.assertEquals(expectedLatitude, actualLatitude, deltaLatitude);

	}

	public void testCreateFromWGS84DoubleDoubleDoubleDoubleDoubleDouble() {

		CoordWGS84 testFromSwisstopoExample = CoordWGS84.createFromWGS84(8, 43, 49.79, 46, 2, 38.87);

		double expectedCH1903X = 100000;
		double deltaCH1903X = 1;
		double expectedCH1903Y = 700000;
		double deltaCH1903Y = 1;
		double expectedLongitude = 8.730497222222223;
		double deltaLongitude = 0.00000001;
		double expectedLatitude = 46.044130555555554;
		double deltaLatitude = 0.00000001;

		double actualX = testFromSwisstopoExample.getXCH1903();
		double actualY = testFromSwisstopoExample.getYCH1903();
		junit.framework.Assert.assertEquals(expectedCH1903X, actualX, deltaCH1903X);
		junit.framework.Assert.assertEquals(expectedCH1903Y, actualY, deltaCH1903Y);

		double actualLongitude = testFromSwisstopoExample.getLongitude();
		double actualLatitude = testFromSwisstopoExample.getLatitude();
		junit.framework.Assert.assertEquals(expectedLongitude, actualLongitude, deltaLongitude);
		junit.framework.Assert.assertEquals(expectedLatitude, actualLatitude, deltaLatitude);

	}
}
