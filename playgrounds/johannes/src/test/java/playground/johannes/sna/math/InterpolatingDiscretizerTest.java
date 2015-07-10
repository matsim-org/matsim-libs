/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.sna.math;

import junit.framework.TestCase;

/**
 * @author johannes
 *
 */
public class InterpolatingDiscretizerTest extends TestCase {

	public void test1() {
		double[] values = new double[10];
		values[0] = 5;
		values[1] = 2;
		values[2] = 5;
		values[3] = 5;
		values[4] = 2;
		values[5] = 6.8;
		values[6] = 2.3;
		values[7] = 10;
		values[8] = 2.3;
		values[9] = 10;
		
		InterpolatingDiscretizer discretizer = new InterpolatingDiscretizer(values);
		
		assertEquals(2.0, discretizer.discretize(-1));
		assertEquals(2.0, discretizer.discretize(2));
		assertEquals(2.0, discretizer.discretize(2.14));
		
		assertEquals(5.0, discretizer.discretize(5));
		assertEquals(5.0, discretizer.discretize(5.89));
		assertEquals(5.0, discretizer.discretize(4.15));
		
		assertEquals(10.0, discretizer.discretize(1000));
		
		assertEquals(6.8, discretizer.discretize(6.77));
	}
}
