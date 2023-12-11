/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimRandomTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.gbl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class MatsimRandomTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * Test that MatsimRandom returns different values.
	 */
	@Test
	void testRandomness() {
		final double value1 = MatsimRandom.getRandom().nextDouble();
		final double value2 = MatsimRandom.getRandom().nextDouble();
		final double value3 = MatsimRandom.getRandom().nextDouble();
		assertTrue(Math.abs(value1 - value2) > MatsimTestUtils.EPSILON);
		assertTrue(Math.abs(value1 - value3) > MatsimTestUtils.EPSILON);
		assertTrue(Math.abs(value2 - value3) > MatsimTestUtils.EPSILON);
	}

	/**
	 * Tests that resetting the RandomObject creates the same random numbers again.
	 */
	@Test
	void testReset() {
		MatsimRandom.reset();
		int value1 = MatsimRandom.getRandom().nextInt();
		MatsimRandom.reset();
		int value2 = MatsimRandom.getRandom().nextInt();
		assertEquals(value1, value2);
	}

	/**
	 * Tests that the same number of random numbers is generated if a custom seed
	 * is used, and that these numbers are different with different seeds.
	 */
	@Test
	void testSeedReset() {
		final long seed1 = 123L;
		final long seed2 = 234L;

		MatsimRandom.reset(seed1);
		double value1 = MatsimRandom.getRandom().nextDouble();
		MatsimRandom.reset(seed1);
		double value2 = MatsimRandom.getRandom().nextDouble();
		assertEquals(value1, value2, MatsimTestUtils.EPSILON);

		MatsimRandom.reset(seed2);
		double value3 = MatsimRandom.getRandom().nextInt();
		assertTrue(Math.abs(value1 - value3) > MatsimTestUtils.EPSILON);
	}

	/**
	 * Tests that local instances can be recreated (=are deterministic) if the
	 * same random seed is used to generate them.
	 */
	@Test
	void testLocalInstances_deterministic() {
		MatsimRandom.reset();
		Random local1a = MatsimRandom.getLocalInstance();
		Random local1b = MatsimRandom.getLocalInstance();

		MatsimRandom.reset();
		Random local2a = MatsimRandom.getLocalInstance();
		Random local2b = MatsimRandom.getLocalInstance();

		assertEqualRandomNumberGenerators(local1a, local2a);
		assertEqualRandomNumberGenerators(local1b, local2b);
	}

	/**
	 * Tests that multiple local instance return different random numbers,
	 * and that they are more or less evenly distributed.
	 */
	@Test
	void testLocalInstances_distribution() {
		MatsimRandom.reset(123L);
		Random local1a = MatsimRandom.getLocalInstance();
		double value1 = local1a.nextDouble();

		MatsimRandom.reset(234L);
		Random local2a = MatsimRandom.getLocalInstance();
		double value2a = local2a.nextDouble();

		Random local2b = MatsimRandom.getLocalInstance();
		double value2b = local2b.nextDouble();

		assertTrue(Math.abs(value1 - value2a) > MatsimTestUtils.EPSILON);
		assertTrue(Math.abs(value2a - value2b) > MatsimTestUtils.EPSILON);
		assertTrue(Math.abs(value1 - value2b) > MatsimTestUtils.EPSILON);
	}

	/** Test that two (Pseudo)Random Number Generators are equil by
	 * drawing a series of random numbers and comparing those.
	 *
	 * @param rng1 first random number generator
	 * @param rng2 second random number generator
	 */
	private void assertEqualRandomNumberGenerators(final Random rng1, final Random rng2) {
		for (int i = 0; i < 10; i++) {
			assertEquals(rng1.nextDouble(), rng2.nextDouble(), MatsimTestUtils.EPSILON, "different element at position " + i);
		}
	}
}
