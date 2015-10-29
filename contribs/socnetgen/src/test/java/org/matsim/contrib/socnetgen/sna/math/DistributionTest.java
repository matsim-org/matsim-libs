/* *********************************************************************** *
 * project: org.matsim.*
 * DistributionTest.java
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
package org.matsim.contrib.socnetgen.sna.math;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import junit.framework.TestCase;

/**
 * @author illenberger
 *
 */
public class DistributionTest extends TestCase {

	public void test1() {
		Distribution distr = new Distribution();
		
		for(int i = 1; i < 11; i++) {
			distr.add(i);
			distr.add(i);
		}
		
		assertEquals(5.5, distr.mean());
		assertEquals(1.0, distr.min());
		assertEquals(10.0, distr.max());
		
		assertEquals(8.25, distr.variance());
		assertEquals(0.5222, distr.coefficientOfVariance(), 0.0001);
		assertEquals(1.77, distr.kurtosis(), 0.01);
		assertEquals(0.0, distr.skewness());
		
		TDoubleDoubleHashMap hist = distr.absoluteDistribution();
		TDoubleDoubleIterator it = hist.iterator();
		for(int i = 0; i < hist.size(); i++) {
			it.advance();
			assertEquals(2.0, it.value());
		}
	}
	
	public void test2() {
		Distribution distr = new Distribution();
		
		for(int i = 1; i < 11; i++) {
			if(i == 5)
				distr.add(i, 2);
			else
				distr.add(i);
		}
		
		assertEquals(5.454, distr.mean(), 0.001);
		assertEquals(7.520, distr.variance(), 0.001);
	}
}
