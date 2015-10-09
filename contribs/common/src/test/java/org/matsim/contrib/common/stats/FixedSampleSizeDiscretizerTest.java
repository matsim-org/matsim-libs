/* *********************************************************************** *
 * project: org.matsim.*
 * FixedSampleSizeDiscretizer.java
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
public class FixedSampleSizeDiscretizerTest extends TestCase {

	public void test() {
		double[] samples = new double[]{1,2,3,4,6,7,8,9,10,12}; 
		Discretizer d = FixedSampleSizeDiscretizer.create(samples, 3);
		
		assertEquals(7.0, d.discretize(3.4));
		assertEquals(12.0, d.discretize(10.0));
		assertEquals(12.0, d.discretize(11.0));
		assertEquals(3.0, d.discretize(1.0));
		
		assertEquals(2.0, d.binWidth(1.0), 0.0001);
		assertEquals(4.0, d.binWidth(4.5), 0.0001);
		assertEquals(5.0, d.binWidth(12.0), 0.0001);
		assertEquals(Double.POSITIVE_INFINITY, d.binWidth(100.0));
	}
}
