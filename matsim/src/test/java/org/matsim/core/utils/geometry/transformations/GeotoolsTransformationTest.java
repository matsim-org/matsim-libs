/* *********************************************************************** *
 * project: org.matsim.*
 * GeotoolsTransformationTest.java
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

package org.matsim.core.utils.geometry.transformations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author laemmel
 *
 */
public class GeotoolsTransformationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testTransform(){
		String toCRS = "WGS84";
		String fromCRS = "WGS84_UTM47S";

		double x = 638748.9000000004;
		double y = 9916839.69;

		double targetX = 100.24690901110904;
		double targetY = -0.7521976363533539;
		double delta = 1e-16;

		Coord coordWGS84UTM47S = new Coord(x, y);

		CoordinateTransformation transform = new GeotoolsTransformation(fromCRS,toCRS);
		Coord coordWGS84 = transform.transform(coordWGS84UTM47S);
		double xWGS84 = coordWGS84.getX();
		double yWGS84 = coordWGS84.getY();


		Assertions.assertEquals(targetX, xWGS84, delta);
		Assertions.assertEquals(targetY, yWGS84, delta);

	}

}
