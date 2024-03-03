/* *********************************************************************** *
 * project: matsim
 * CoordUtils.java
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

package org.matsim.utils.geometry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author nagel
 */
public class CoordUtilsTest {
	private static final double delta = 0.000001;

	/**
	 * Test method for {@link org.matsim.core.utils.geometry.CoordUtils#plus(org.matsim.api.core.v01.Coord, org.matsim.api.core.v01.Coord)}.
	 */
	@Test
	void testPlus() {
		Coord coord1 = new Coord(1., 2.);
		Coord coord2 = new Coord(3., 4.);
		Coord result = CoordUtils.plus( coord1, coord2 ) ;
		Assertions.assertEquals( 4., result.getX(), delta) ;
		Assertions.assertEquals( 6., result.getY(), delta) ;
	}

	/**
	 * Test method for {@link org.matsim.core.utils.geometry.CoordUtils#minus(org.matsim.api.core.v01.Coord, org.matsim.api.core.v01.Coord)}.
	 */
	@Test
	void testMinus() {
		Coord coord1 = new Coord(1., 2.);
		Coord coord2 = new Coord(3., 5.);
		Coord result = CoordUtils.minus( coord1, coord2 ) ;
		Assertions.assertEquals( -2., result.getX(), delta) ;
		Assertions.assertEquals( -3., result.getY(), delta) ;
	}

	/**
	 * Test method for {@link org.matsim.core.utils.geometry.CoordUtils#scalarMult(double, org.matsim.api.core.v01.Coord)}.
	 */
	@Test
	void testScalarMult() {
		Coord coord1 = new Coord(1., 2.);
		Coord result = CoordUtils.scalarMult( -0.33 , coord1 ) ;
		Assertions.assertEquals( -0.33, result.getX(), delta) ;
		Assertions.assertEquals( -0.66, result.getY(), delta) ;
	}

	/**
	 * Test method for {@link org.matsim.core.utils.geometry.CoordUtils#getCenter(org.matsim.api.core.v01.Coord, org.matsim.api.core.v01.Coord)}.
	 */
	@Test
	void testGetCenter() {
		Coord coord1 = new Coord(1., 2.);
		Coord coord2 = new Coord(3., 5.);
		Coord result = CoordUtils.getCenter( coord1, coord2 ) ;
		Assertions.assertEquals( 2., result.getX(), delta) ;
		Assertions.assertEquals( 3.5, result.getY(), delta) ;
	}

	/**
	 * Test method for {@link org.matsim.core.utils.geometry.CoordUtils#length(org.matsim.api.core.v01.Coord)}.
	 */
	@Test
	void testLength() {
		Coord coord1 = new Coord(3., 2.);
		double result = CoordUtils.length( coord1 ) ;
		Assertions.assertEquals( Math.sqrt( 9. + 4. ), result, delta) ;
	}
}
