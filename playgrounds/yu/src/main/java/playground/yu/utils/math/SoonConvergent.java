/* *********************************************************************** *
 * project: org.matsim.*
 * SoonConvergent.java
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

/**
 * just a naive method to judge whether a double array would probably be
 * convergent a foreseeable while, this class might be used after preparatory
 * iterations (warm up). Only the necessary conditions for convergence is
 * described here.
 * 
 * @author yu
 * 
 */
public class SoonConvergent {
	/**
	 * @param amplitudeCriterion
	 *            criterion for the difference between the highest and lowest
	 *            value in each half of the array, the absolute value of the
	 *            second difference may be smaller than the absolute value of
	 *            the first difference * this amplitudeCriterion, z.B. 0.7, 0.6
	 *            ...
	 * @param avgValueCriterion
	 *            the average value of the second half of the array may not
	 *            exceed a rang of +/- avgValueCriterion with the average value
	 *            of the first half as the center, should stand in the range of
	 *            (0,1), should be a positive value <0.2, e.g. 0.1, 0.05
	 * @param values
	 *            a double array, please had better ensure that the array length
	 *            is a even number
	 * @return boolean value, whether the array values would be soon convergent
	 */
	public static boolean wouldBe(double amplitudeCriterion,
			double avgValueCriterion, double[] values) {

		int size1 = values.length / 2;

		double min1 = SimpleStatistics.min(values, 0, size1 - 1)//
		, max1 = SimpleStatistics.max(values, 0, size1 - 1)//
		, min2 = SimpleStatistics.min(values, size1, values.length - 1)//
		, max2 = SimpleStatistics.max(values, size1, values.length - 1);

		boolean firstCondition = Math.abs(max2 - min2) <= amplitudeCriterion
				* Math.abs(max1 - min1);

		if (firstCondition) {
			return true;
		} else {
			System.out
					.println("+++++BSE:\tfirst convergency condition -\tfalse\n+++++BSE:\tMath.abs(\t"
							+ max2
							+ "\t-\t"
							+ min2
							+ "\t) <=\t"
							+ amplitudeCriterion
							+ "\t* Math.abs(\t"
							+ max1
							+ "\t-\t" + min1 + "\t)");
			double avg1 = SimpleStatistics.average(values, 0, size1 - 1)//
			, avg2 = SimpleStatistics.average(values, size1, values.length - 1);

			boolean secondCondition = false;
			if (avg1 != 0d) {
				if (Math.abs(avg1) < 0.5 || Math.abs(avg2) < 0.5) {
					// avgValueCriterion *= 2d / (Math.abs(avg1) +
					// Math.abs(avg2));
					return true;
				}
				secondCondition = Math.abs((avg2 - avg1) / avg1) <= avgValueCriterion;
				if (Math.abs((avg2 - avg1) / avg1) > 0.7) {
					System.err
							.println("\n+++++BSE:\tMath.abs((avg2 - avg1) / avg1)>0.7-->"
									+ "\n+++++BSE:\t1. maybe the proparatoryIteration too short, or"
									+ "\n+++++BSE:\t2. the initialStepSize to small, so it converges too slowly or"
									+ "\n+++++BSE:\t3. the calibrated parameter stands too next to 0, so e.g. the abs(avg1) is very small."
									+ "\n+++++BSE:\tavg1 =\t"
									+ avg1
									+ "\n+++++BSE:\tavg2 =\t"
									+ avg2
									+ "\n+++++BSE:\tMath.abs((avg2 - avg1) / avg1) =\t"
									+ Math.abs((avg2 - avg1) / avg1));
				}
			} else {
				secondCondition = Math.abs(avg2) <= avgValueCriterion;
			}

			return secondCondition;
		}
	}
}
