/* *********************************************************************** *
 * project: org.matsim.*
 * WorldUtilsTest.java
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

package org.matsim.util;

import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.WorldUtils;
import org.matsim.utils.geometry.shared.Coord;

public class WorldUtilsTest extends MatsimTestCase {

	/**
	 * Tests org.matsim.util.WorldUtils.distancePointLinesegment()
	 *
	 * @author mrieser
	 */
	public void testDistancePointLinesegment() {
		Coord p1 = new Coord(10, 20);
		Coord p2 = new Coord(10, 30);
		Coord p3 = new Coord(10, 40);
		Coord p4 = new Coord(20, 30);

		assertEquals(10.0, WorldUtils.distancePointLinesegment(p1, p3, p4), 1e-10);

		assertEquals("special case where lineFrom == lineTo",
				10.0, WorldUtils.distancePointLinesegment(p1, p1, p2), 1e-10);

		assertEquals("special case where point before lineFrom",
				10.0, WorldUtils.distancePointLinesegment(p2, p3, p1), 1e-10);

		assertEquals("special case where point after lineTo",
				10.0, WorldUtils.distancePointLinesegment(p1, p2, p3), 1e-10);

		assertEquals("special case where point on line segment",
				0.0, WorldUtils.distancePointLinesegment(p1, p3, p2), 1e-10);

	}

}
