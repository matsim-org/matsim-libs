/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.geometry.transformations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;

/**
 * @author mrieser
 */
public class CH1903LV03toWGS84Test {

	/**
	 * Tests the transformation with the values from
	 * http://www.swisstopo.ch/pub/down/basics/geo/system/ch1903_wgs84_de.pdf
	 */
	@Test
	void testTransform() {
		double xx = 8.0 + 43.0/60 + 49.80/3600;
		double yy = 46.0 + 02.0/60 + 38.86/3600;
		double epsilon = 1e-6;

		CH1903LV03toWGS84 converter = new CH1903LV03toWGS84();
		Coord n = converter.transform(new Coord((double) 700000, (double) 100000));
		Assertions.assertEquals(xx, n.getX(), epsilon);
		Assertions.assertEquals(yy, n.getY(), epsilon);
	}
}
