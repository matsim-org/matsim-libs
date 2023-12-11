/* *********************************************************************** *
 * project: org.matsim.*
 * CoordTest.java
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

package org.matsim.api.core.v01;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.matsim.testcases.MatsimTestUtils;

public class CoordTest {

	@Test
	void testCoord2D() {
		@SuppressWarnings("unused")
		Coord c;
		try{
			c = new Coord(0.0, 1.0);
		} catch (Exception e){
			fail("Should create coordinate.");
		}
	}

	@Test
	void testCoord3D() {
		@SuppressWarnings("unused")
		Coord c;
		try{
			c = new Coord(0.0, 1.0, 2.0);
		} catch (Exception e){
			fail("Should create coordinate");
		}
		
		/* Should not accept Double.NEGATIVE_INFINITY as elevation. */
		@SuppressWarnings("unused")
		Coord c2d;
		try{
			c2d = new Coord(0.0, 1.0, Double.NEGATIVE_INFINITY);
			fail("Should not accept special-value elevation.");
		} catch ( IllegalArgumentException e){
			/* Pass */
		}
	}

	@Test
	void testGetX() {
		// 2D
		Coord c2 = new Coord(0.0, 1.0);
		assertEquals(0.0, c2.getX(), MatsimTestUtils.EPSILON, "Wrong x-value.");

		// 3D
		Coord c3 = new Coord(0.0, 1.0, 2.0);
		assertEquals(0.0, c3.getX(), MatsimTestUtils.EPSILON, "Wrong x-value.");
	}

	@Test
	void testGetY() {
		// 2D
		Coord c2 = new Coord(0.0, 1.0);
		assertEquals(1.0, c2.getY(), MatsimTestUtils.EPSILON, "Wrong y-value.");

		// 3D
		Coord c3 = new Coord(0.0, 1.0, 2.0);
		assertEquals(1.0, c3.getY(), MatsimTestUtils.EPSILON, "Wrong y-value.");
	}

	@Test
	void testGetZ() {
		// 2D
		Coord c2 = new Coord(0.0, 1.0);
		try{
			@SuppressWarnings("unused")
			double z = c2.getZ();
			fail("Should not return z-value.");
		} catch (Exception e){
			// Pass.
		}
		
		// 3D
		Coord c3 = new Coord(0.0, 1.0, 2.0);
		assertEquals(2.0, c3.getZ(), MatsimTestUtils.EPSILON, "Wrong z-value.");
	}

	@Test
	void testEqualsObject() {
		Double dummy = 0.0;
		
		Coord c2a = new Coord(0.0, 1.0);
		Coord c2b = new Coord(0.0, 1.0);
		Coord c2c = new Coord(0.0, 2.0);
		
		Coord c3a = new Coord(0.0, 1.0, 2.0);
		Coord c3b = new Coord(0.0, 1.0, 2.0);
		Coord c3c = new Coord(0.0, 1.0, 3.0);
		
		assertFalse(c2a.equals(dummy), "Coordinates should not be equal.");
		assertTrue(c2a.equals(c2b), "Coordinates should not be equal.");
		assertFalse(c2a.equals(c2c), "Coordinates should not be equal.");
		assertFalse(c2a.equals(c3a), "2D coordinate should not be equal to 3D coordinate.");
		
		assertFalse(c3a.equals(dummy), "Coordinates should not be equal.");
		assertTrue(c3a.equals(c3b), "Coordinates should not be equal.");
		assertFalse(c3a.equals(c3c), "Coordinates should not be equal.");
		assertFalse(c3a.equals(c2a), "3D coordinate should not be equal to 2D coordinate.");
	}

	@Test
	void testToString() {
		Coord c2 = new Coord(0.0, 1.0);
		assertTrue(c2.toString().equalsIgnoreCase("[x=0.0 | y=1.0]"));
		
		Coord c3 = new Coord(0.0, 1.0, 2.0);
		assertTrue(c3.toString().equalsIgnoreCase("[x=0.0 | y=1.0 | z=2.0]"));
	}

}
