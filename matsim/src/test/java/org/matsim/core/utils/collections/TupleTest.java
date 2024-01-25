/* *********************************************************************** *
 * project: org.matsim.*
 * TupleTest.java
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

package org.matsim.core.utils.collections;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TupleTest {
	@Test
	void testOf() {
		Tuple<Integer, Double> t1 = new Tuple<>(1, 1.1);
		Tuple<Integer, Double> t2 = Tuple.of(1, 1.1);
		assertEquals(t1, t2);
	}


	@Test
	void testEquals() {
		// the basic Tuple we will usually compare against
		Tuple<Integer, Double> t1 = new Tuple<Integer, Double>(1, 1.1);

		// test the the Tuple is equals to itself
		assertTrue(t1.equals(t1));

		// test if other Tuples are recognized as different, or as equal
		Tuple<Integer, Double> t2 = new Tuple<Integer, Double>(2, 2.2);
		Tuple<Integer, Double> t3 = new Tuple<Integer, Double>(1, 1.1);

		assertFalse(t1.equals(t2));
		assertFalse(t2.equals(t1));
		assertTrue(t1.equals(t3));
		assertTrue(t3.equals(t1));

		// ensure that not only one of the two values is compared
		Tuple<Integer, Double> t4 = new Tuple<Integer, Double>(1, 2.2); // first entry is the same as t1
		assertFalse(t1.equals(t4));
		Tuple<Integer, Double> t5 = new Tuple<Integer, Double>(2, 1.1); // second entry is the same as t1
		assertFalse(t1.equals(t5));

		// ensure that the order of the values is taken into account
		Tuple<Double, Integer> t6 = new Tuple<Double, Integer>(1.1, 1); // switched first and second
		assertFalse(t1.equals(t6));

		// ensure that the types of the values is respected as well
		Tuple<Integer, Double> t7 = new Tuple<Integer, Double>(1, 2.0);
		Tuple<Integer, Integer> t8 = new Tuple<Integer, Integer>(1, 2);
		assertFalse(t7.equals(t8));
		assertFalse(t8.equals(t7));

		// ensure the comparison with null and other objects
		assertNotNull(t1);
		assertFalse(t1.equals(Integer.valueOf(1)));
	}

	@Test
	void testEquals_withNull() {
		Integer i1 = Integer.valueOf(1);
		Integer i2 = Integer.valueOf(2);
		Tuple<Integer, Integer> tuple1a = new Tuple<Integer, Integer>(i1, null);
		Tuple<Integer, Integer> tuple1b = new Tuple<Integer, Integer>(i1, null);
		Tuple<Integer, Integer> tuple1c = new Tuple<Integer, Integer>(i2, null);
		Tuple<Integer, Integer> tuple2a = new Tuple<Integer, Integer>(null, i1);
		Tuple<Integer, Integer> tuple2b = new Tuple<Integer, Integer>(null, i1);
		Tuple<Integer, Integer> tuple2c = new Tuple<Integer, Integer>(null, i2);
		Tuple<Integer, Integer> tuple3a = new Tuple<Integer, Integer>(null, null);
		Tuple<Integer, Integer> tuple3b = new Tuple<Integer, Integer>(null, null);
		Tuple<Double, Double> tuple3c = new Tuple<Double, Double>(null, null);
		Tuple<Integer, Integer> tuple4 = new Tuple<Integer, Integer>(i1, i1);

		// check cases where first or second is null in both tuples
		assertTrue(tuple1a.equals(tuple1b));
		assertFalse(tuple1a.equals(tuple1c));
		assertTrue(tuple2a.equals(tuple2b));
		assertFalse(tuple2a.equals(tuple2c));
		assertTrue(tuple3a.equals(tuple3b));
		assertTrue(tuple3a.equals(tuple3c)); // different generic types cannot be recognized

		// check cases where first or second is null in one of the tuples only
		assertFalse(tuple4.equals(tuple1a));
		assertFalse(tuple1a.equals(tuple4));
		assertFalse(tuple4.equals(tuple2a));
		assertFalse(tuple2a.equals(tuple4));
		assertFalse(tuple4.equals(tuple3a));
		assertFalse(tuple3a.equals(tuple4));
	}

	@Test
	void testHashCode_withNull() {
		Integer i1 = Integer.valueOf(1);
		Integer i2 = Integer.valueOf(2);
		Tuple<Integer, Integer> tuple = new Tuple<Integer, Integer>(i1, i2);
		assertEquals(3, tuple.hashCode());
		tuple = new Tuple<Integer, Integer>(null, i2);
		assertEquals(2, tuple.hashCode());
		tuple = new Tuple<Integer, Integer>(i1, null);
		assertEquals(1, tuple.hashCode());
		tuple = new Tuple<Integer, Integer>(null, null);
		assertEquals(0, tuple.hashCode());
	}
}
