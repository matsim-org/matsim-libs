/* *********************************************************************** *
 * project: org.matsim.*
 * FixedSampleSizeDiscretizerTest.java
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
package playground.johannes.graph;

import java.util.Random;

import junit.framework.TestCase;
import playground.johannes.socialnetworks.statistics.Discretizer;
import playground.johannes.socialnetworks.statistics.FixedSampleSizeDiscretizer;

/**
 * @author illenberger
 *
 */
public class FixedSampleSizeDiscretizerTest extends TestCase {

	public void test() {
		Random random = new Random(0);
		double samples[] = new double[100]; 
		for(int i = 0; i < 100; i++)
			samples[i] = random.nextInt(100);
		
		Discretizer discretizer = new FixedSampleSizeDiscretizer(samples, 10);
		
		assertEquals(12.0, discretizer.discretize(10));
		assertEquals(19.0, discretizer.discretize(13));
		assertEquals(29.0, discretizer.discretize(29));
		assertEquals(98.0, discretizer.discretize(89));
	}
}
