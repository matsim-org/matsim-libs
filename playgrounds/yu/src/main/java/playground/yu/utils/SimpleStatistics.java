/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleStatistics.java
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

/**
 * 
 */
package playground.yu.utils;

/**
 * offers a few simple statistics function
 * 
 * @author yu
 * 
 */
public class SimpleStatistics {
	public static double getAverage(double[] array, int firstIdx, int lastIdx) {
		double sum = 0.0;
		for (int i = firstIdx; i <= lastIdx; i++)
			sum += array[i];

		return sum / (double) (lastIdx - firstIdx + 1);
	}

	public static double getVariance(double[] array, int firstIdx, int lastIdx) {
		double N = lastIdx - firstIdx + 1, avg = getAverage(array, firstIdx,
				lastIdx);
		double sum = 0.0;
		for (int i = firstIdx; i <= lastIdx; i++)
			sum += array[i] * array[i];

		return sum / N - avg * avg;
	}
}
