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

package org.matsim.utils.collections;

import org.matsim.testcases.MatsimTestCase;

public class TupleTest extends MatsimTestCase {

	public void testEquals() {
		// the basic Tuple we will usually compare against
		Tuple<Integer, Double> t1 = new Tuple<Integer, Double>(1, 1.1);
		
		// test the the Tupel is equals to itself
		assertTrue(t1.equals(t1));
		
		// test if other Tupels are recognized as different, or as equal
		Tuple<Integer, Double> t2 = new Tuple<Integer, Double>(2, 2.2);
		Tuple<Integer, Double> t3 = new Tuple<Integer, Double>(1, 1.1);

		assertFalse(t1.equals(t2));
		assertFalse(t2.equals(t1));
		assertTrue(t1.equals(t3));
		assertTrue(t3.equals(t1));
		
		// ensure that not only one of the two values is compared
		Tuple<Integer, Double> t4 = new Tuple<Integer, Double>(1, 2.2); // first entry is the same as t1
		assertFalse(t1.equals(t4));
		Tuple<Integer, Double> t5 = new Tuple<Integer, Double>(2, 1.1); // second entry is te same as t1
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
		assertFalse(t1.equals(null));
		assertFalse(t1.equals(Integer.valueOf(1)));
	}
	
}
