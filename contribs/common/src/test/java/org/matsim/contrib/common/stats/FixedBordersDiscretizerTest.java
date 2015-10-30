/* *********************************************************************** *
 * project: org.matsim.*
 * FixedBordersDiscretizerTest.java
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
package org.matsim.contrib.common.stats;

import junit.framework.TestCase;

/**
 * @author illenberger
 *
 */
public class FixedBordersDiscretizerTest extends TestCase {

	public void test() {
		Discretizer d = new FixedBordersDiscretizer(new double[]{1.5, 2.3, 4.9});
		
		assertEquals(1.5, d.discretize(1.0));
		assertEquals(2.3, d.discretize(1.6));
		assertEquals(2.3, d.discretize(2.3));
		assertEquals(4.9, d.discretize(2.4));
		assertEquals(4.9, d.discretize(4.9));
		assertEquals(Double.POSITIVE_INFINITY, d.discretize(5.0));
		
		assertEquals(0.8, d.binWidth(2.0), 0.0001);
		assertEquals(0.8, d.binWidth(2.3), 0.0001);
		assertEquals(2.6, d.binWidth(2.4), 0.0001);
		
		assertEquals(Double.POSITIVE_INFINITY, d.binWidth(0.0), 0.0001);
		assertEquals(Double.POSITIVE_INFINITY, d.binWidth(1.5), 0.0001);
		assertEquals(Double.POSITIVE_INFINITY, d.binWidth(5.0), 0.0001);
	}
	
	public void test2() {
		double[] borders = new double[]{1.5, 2.3, 4.9};
		double[] values = new double[]{1.0, 5.0};
		
		Discretizer d = new FixedBordersDiscretizer(values, borders);
		
		assertEquals(1.5, d.discretize(1.0));
		assertEquals(5.0, d.discretize(5.0));
		
		assertEquals(0.5, d.binWidth(1.0), 0.0001);
		assertEquals(0.1, d.binWidth(5.0), 0.0001);
	}
}
