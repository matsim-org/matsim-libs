/* *********************************************************************** *
 * project: org.matsim.*
 * Consistent.java
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
package playground.yu.utils.math;

import org.matsim.core.utils.collections.Tuple;

/**
 * * just a naive method to judge whether a double {@code Tuple} would probably
 * be consistent, this class might be used after preparatory iterations (warm
 * up). Only the necessary conditions for consistency is described here.
 * 
 * @author C
 * 
 */
public class Consistent {
	/**
	 * @param criterion
	 *            double criterion for consistency, the absolute value of the
	 *            current/second element in {@code Tuple} should stay in a range
	 *            of +/-amplitudeCriterion* absolute value of the last/first
	 *            element with the last/first element as center, and should be a
	 *            positive value e.g. 0.1
	 * 
	 * @param values
	 *            a {@code Tuple}
	 * @return whether the 2 elements in a {@code Tuple} could show the
	 *         consistency
	 */
	public static boolean wouldBe(double criterion, Tuple<Double, Double> values) {
		double vLast = values.getFirst(), vCurrent = values.getSecond();
		if (vLast != 0d) {
			return Math.abs((vCurrent - vLast) / vLast) <= criterion;
		}
		return Math.abs(vCurrent) <= criterion;
	}
}
