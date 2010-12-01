/* *********************************************************************** *
 * project: org.matsim.*
 * Correlations.java
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

/**
 * 
 */
package playground.johannes.statistics;

import gnu.trove.TDoubleDoubleHashMap;
import junit.framework.TestCase;
import playground.johannes.socialnetworks.statistics.Correlations;

/**
 * @author illenberger
 *
 */
public class CorrelationsTest extends TestCase {

	public void testCorrelationMean1() {
		double[] values1 = new double[]{1, 1, 2, 2, 3, 3, 3};
		double[] values2 = new double[]{5, 3, 1, 3, 7, 3, 2};
		
		TDoubleDoubleHashMap meanValues = Correlations.correlationMean(values1, values2);
		
		assertEquals(4.0, meanValues.get(1));
		assertEquals(2.0, meanValues.get(2));
		assertEquals(4.0, meanValues.get(3));
	}
	
	public void testCorrelationMean2() {
		double[] values1 = new double[]{1, 1.2, 2.999, 2.5, 3.0, 3.4632, 3.63};
		double[] values2 = new double[]{5, 3, 1, 3, 7, 3, 2};
		
		TDoubleDoubleHashMap meanValues = Correlations.correlationMean(values1, values2, 1.0);
		
		assertEquals(5.0, meanValues.get(1));
		assertEquals(3.0, meanValues.get(2));
		assertEquals(3.666, meanValues.get(3),0.001);
	}
	
	public void testCorrelationMean3() {
		double[] values1 = new double[]{1, 1.2, 2.999, 2.5, 3.0, 3.4632, 3.63};
		double[] values2 = new double[]{5, 3, 1, 3, 7, 3, 2};
		
		TDoubleDoubleHashMap meanValues = Correlations.correlationMean(values1, values2, 2.0);
		
		assertEquals(0.0, meanValues.get(0));
		assertEquals(4.0, meanValues.get(2));
		assertEquals(3.2, meanValues.get(4));
	}
}
