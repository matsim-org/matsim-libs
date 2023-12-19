/* *********************************************************************** *
 * project: org.matsim.*
 * CoordUtilsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.geometry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.testcases.MatsimTestUtils;

public class CoordUtilsTest {

	@Test
	void testCreateCoord2D() {
		Coord c1 = new Coord(0.0, 1.0);
		Coord c2 = CoordUtils.createCoord(0.0, 1.0);
		Coord c3 = CoordUtils.createCoord(0.0, 2.0);
		assertEquals(c1, c2);
		assertNotEquals(c1, c3);
	}

	@Test
	void testCreateCoord3D() {
		Coord c1 = new Coord(0.0, 1.0, 2.0);
		Coord c2 = CoordUtils.createCoord(0.0, 1.0, 2.0);
		Coord c3 = CoordUtils.createCoord(0.0, 2.0, 2.0);
		assertEquals(c1, c2);
		assertNotEquals(c1, c3);
	}

	@Test
	void testPlus() {
		Coord c2a = CoordUtils.createCoord(1.0, 1.0);
		Coord c2b = CoordUtils.createCoord(2.0, 2.0);
		Coord c2c = CoordUtils.createCoord(3.0, 3.0);

		Coord c3a = CoordUtils.createCoord(1.0, 1.0, 1.0);
		Coord c3b = CoordUtils.createCoord(2.0, 2.0, 2.0);
		Coord c3c = CoordUtils.createCoord(3.0, 3.0, 3.0);

		// 2D
		assertEquals(c2c, CoordUtils.plus(c2a, c2b));
		// 3D
		assertEquals(c3c, CoordUtils.plus(c3a, c3b));
		// Mixing 2D and 3D;
		try{
			@SuppressWarnings("unused")
			Coord c = CoordUtils.plus(c2a, c3a);
			fail("Should not be able to mix 2D and 3D coordinate calculations.");
		} catch(Exception e){
			/* Pass */
		}
		try{
			@SuppressWarnings("unused")
			Coord c = CoordUtils.plus(c3a, c2a);
			fail("Should not be able to mix 3D and 2D coordinate calculations.");
		} catch(Exception e){
			/* Pass */
		}
	}

	@Test
	void testMinus() {
		Coord c2a = CoordUtils.createCoord(1.0, 1.0);
		Coord c2b = CoordUtils.createCoord(2.0, 2.0);
		Coord c2c = CoordUtils.createCoord(3.0, 3.0);

		Coord c3a = CoordUtils.createCoord(1.0, 1.0, 1.0);
		Coord c3b = CoordUtils.createCoord(2.0, 2.0, 2.0);
		Coord c3c = CoordUtils.createCoord(3.0, 3.0, 3.0);

		// 2D
		assertEquals(c2a, CoordUtils.minus(c2c, c2b));
		// 3D
		assertEquals(c3a, CoordUtils.minus(c3c, c3b));
		// Mixing 2D and 3D;
		try{
			@SuppressWarnings("unused")
			Coord c = CoordUtils.minus(c2a, c3a);
			fail("Should not be able to mix 2D and 3D coordinate calculations.");
		} catch(Exception e){
			/* Pass */
		}
		try{
			@SuppressWarnings("unused")
			Coord c = CoordUtils.minus(c3a, c2a);
			fail("Should not be able to mix 3D and 2D coordinate calculations.");
		} catch(Exception e){
			/* Pass */
		}
	}

	@Test
	void testScalarMult() {
		// 2D
		Coord c2a = CoordUtils.createCoord(1.0, 1.0);
		Coord c2b = CoordUtils.createCoord(2.0, 2.0);
		assertEquals(c2b, CoordUtils.scalarMult(2.0, c2a));

		// 3D
		Coord c3a = CoordUtils.createCoord(1.0, 1.0, 1.0);
		Coord c3b = CoordUtils.createCoord(2.0, 2.0, 2.0);
		assertEquals(c3b, CoordUtils.scalarMult(2.0, c3a));
	}

	@Test
	void testGetCenter() {
		// 2D
		Coord c2a = CoordUtils.createCoord(0.0, 0.0);
		Coord c2b = CoordUtils.createCoord(2.0, 2.0);
		Coord c2c = CoordUtils.createCoord(1.0, 1.0);
		assertEquals(c2c, CoordUtils.getCenter(c2a, c2b));

		// 3D
		Coord c3a = CoordUtils.createCoord(0.0, 0.0, 0.0);
		Coord c3b = CoordUtils.createCoord(2.0, 2.0, 2.0);
		Coord c3c = CoordUtils.createCoord(1.0, 1.0, 1.0);
		assertEquals(c3c, CoordUtils.getCenter(c3a, c3b));
	}

	@Test
	void testLength() {
		// 2D
		Coord c2 = CoordUtils.createCoord(2.0, 2.0);
		assertEquals(Math.sqrt(8.0), CoordUtils.length(c2), MatsimTestUtils.EPSILON);
		// 3D
		Coord c3 = CoordUtils.createCoord(2.0, 2.0, 2.0);
		assertEquals(Math.sqrt(12.0), CoordUtils.length(c3), MatsimTestUtils.EPSILON);
	}

	@Test
	void testRotateToRight() {
		Coord coord1 = new Coord(3., 2.);

		Coord result = CoordUtils.rotateToRight( coord1 ) ;
		Assertions.assertEquals(  2., result.getX(), MatsimTestUtils.EPSILON ) ;
		Assertions.assertEquals( -3., result.getY(), MatsimTestUtils.EPSILON ) ;

		result = CoordUtils.rotateToRight( result ) ;
		Assertions.assertEquals( -3., result.getX(), MatsimTestUtils.EPSILON ) ;
		Assertions.assertEquals( -2., result.getY(), MatsimTestUtils.EPSILON ) ;

		result = CoordUtils.rotateToRight( result ) ;
		Assertions.assertEquals( -2., result.getX(), MatsimTestUtils.EPSILON ) ;
		 Assertions.assertEquals( 3., result.getY(), MatsimTestUtils.EPSILON ) ;

		result = CoordUtils.rotateToRight( result ) ;
		Assertions.assertEquals( coord1.getX(), result.getX(), MatsimTestUtils.EPSILON ) ;
		Assertions.assertEquals( coord1.getY(), result.getY(), MatsimTestUtils.EPSILON ) ;
	}

	@Test
	@Disabled
	void testGetCenterWOffset() {
		fail("Not yet implemented");
	}

	@Test
	void testCalcEuclideanDistance() {
		// 2D
		Coord c2a = CoordUtils.createCoord(0.0, 0.0);
		Coord c2b = CoordUtils.createCoord(1.0, 0.0);
		Coord c2c = CoordUtils.createCoord(0.0, 1.0);
		Coord c2d = CoordUtils.createCoord(1.0, 1.0);
		assertEquals(1.0, CoordUtils.calcEuclideanDistance(c2a, c2b), MatsimTestUtils.EPSILON);
		assertEquals(1.0, CoordUtils.calcEuclideanDistance(c2a, c2c), MatsimTestUtils.EPSILON);
		assertEquals(Math.sqrt(2.0), CoordUtils.calcEuclideanDistance(c2a, c2d), MatsimTestUtils.EPSILON);

		// 3D
		Coord c3a = CoordUtils.createCoord(0.0, 0.0, 0.0);
		Coord c3b = CoordUtils.createCoord(1.0, 0.0, 0.0);
		Coord c3c = CoordUtils.createCoord(0.0, 1.0, 0.0);
		Coord c3d = CoordUtils.createCoord(0.0, 0.0, 1.0);
		Coord c3e = CoordUtils.createCoord(1.0, 1.0, 1.0);
		assertEquals(1.0, CoordUtils.calcEuclideanDistance(c3a, c3b), MatsimTestUtils.EPSILON);
		assertEquals(1.0, CoordUtils.calcEuclideanDistance(c3a, c3c), MatsimTestUtils.EPSILON);
		assertEquals(1.0, CoordUtils.calcEuclideanDistance(c3a, c3d), MatsimTestUtils.EPSILON);
		assertEquals(Math.sqrt(3.0), CoordUtils.calcEuclideanDistance(c3a, c3e), MatsimTestUtils.EPSILON);

		// Mixed 2D and 3D
		assertEquals(Math.sqrt(2.0), CoordUtils.calcEuclideanDistance(c2a, c3e), MatsimTestUtils.EPSILON);
	}


	@Test
	void testCalcProjectedDistance() {
		// 2D
		Coord c2a = CoordUtils.createCoord(0.0, 0.0);
		Coord c2b = CoordUtils.createCoord(1.0, 1.0);
		assertEquals(Math.sqrt(2.0), CoordUtils.calcProjectedEuclideanDistance(c2a, c2b), MatsimTestUtils.EPSILON);

		// 3D
		Coord c3a = CoordUtils.createCoord(0.0, 0.0, 0.0);
		Coord c3b = CoordUtils.createCoord(1.0, 1.0, 1.0);
		assertEquals(Math.sqrt(2.0), CoordUtils.calcProjectedEuclideanDistance(c3a, c3b), MatsimTestUtils.EPSILON);

		// Mixed 2D and 3D
		assertEquals(Math.sqrt(2.0), CoordUtils.calcProjectedEuclideanDistance(c2a, c3b), MatsimTestUtils.EPSILON);
	}


	@Test
	void testDistancePointLinesegment() {
		/* First: 2D */

		/*   * (0,1) c1
		 *
		 *  c2 (1,0) *-------* (2,0) c3
		 */
		Coord c1 = CoordUtils.createCoord(0.0, 1.0);
		Coord c2 = CoordUtils.createCoord(1.0, 0.0);
		Coord c3 = CoordUtils.createCoord(2.0, 0.0);
		double dist = CoordUtils.distancePointLinesegment(c2, c3, c1);
		assertEquals(Math.sqrt(2.0), dist, MatsimTestUtils.EPSILON);

		/*                        * (3,1) c1
		 *
		 *  c2 (1,0) *-------* (2,0) c3
		 */
		c1 = CoordUtils.createCoord(3.0, 1.0);
		dist = CoordUtils.distancePointLinesegment(c2, c3, c1);
		assertEquals(Math.sqrt(2.0), dist, MatsimTestUtils.EPSILON);

		/*               * (1.5,1) c1
		 *
		 *  c2 (1,0) *-------* (2,0) c3
		 */
		c1 = CoordUtils.createCoord(1.5, 1.0);
		dist = CoordUtils.distancePointLinesegment(c2, c3, c1);
		assertEquals(Math.sqrt(1.0), dist, MatsimTestUtils.EPSILON);

		/* Second: 3D */

		/* Here the line segment has first/from coordinate (1.0, 1.0, 1.0) and
		 * a second/to coordinate (2.0, 2.0, 2.0) */
		c1 = CoordUtils.createCoord(0.0, 0.0, 0.0);
		c2 = CoordUtils.createCoord(1.0, 1.0, 1.0);
		c3 = CoordUtils.createCoord(2.0, 2.0, 2.0);
		dist = CoordUtils.distancePointLinesegment(c2, c3, c1);
		assertEquals(Math.sqrt(3.0), dist, MatsimTestUtils.EPSILON);

		c1 = CoordUtils.createCoord(1.5, 1.5, 1.5);
		dist = CoordUtils.distancePointLinesegment(c2, c3, c1);
		assertEquals(0.0, dist, MatsimTestUtils.EPSILON);

		c1 = CoordUtils.createCoord(3.0, 2.0, 3.0);
		dist = CoordUtils.distancePointLinesegment(c2, c3, c1);
		assertEquals(CoordUtils.calcEuclideanDistance(c3, c1), dist, MatsimTestUtils.EPSILON);
	}

	@Test
	void testOrthogonalProjectionOnLineSegment(){
		/* First: 2D */
		Coord point = CoordUtils.createCoord(2.0, 0.0);
		Coord lineFrom = CoordUtils.createCoord(0.0, 0.0);
		Coord lineTo = CoordUtils.createCoord(2.0, 2.0);

		Coord projection = CoordUtils.createCoord(1.0, 1.0);
		assertEquals(projection, CoordUtils.orthogonalProjectionOnLineSegment(lineFrom, lineTo, point));

		/* Second: 3D */
		lineFrom = CoordUtils.createCoord(0.0, 0.0, 0.0);
		lineTo = CoordUtils.createCoord(2.0, 2.0, 2.0);
		point = CoordUtils.createCoord(2.0, 0.0, 1.0);

		projection = CoordUtils.createCoord(1.0, 1.0, 1.0);
		assertEquals(projection, CoordUtils.orthogonalProjectionOnLineSegment(lineFrom, lineTo, point));

		point = CoordUtils.createCoord(3.0, 3.0, 3.0);
		assertEquals(point, CoordUtils.orthogonalProjectionOnLineSegment(lineFrom, lineTo, point));
	}


}
