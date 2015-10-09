/* *********************************************************************** *
 * project: org.matsim.*
 * FixedBordersDiscretizer.java
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

import org.apache.commons.math.stat.StatUtils;

import java.util.Arrays;

/**
 * A discretizer with predefined bin borders. Values are rounded up to the upper
 * bin border. A bin starts at the lower bin border (exclusive) and ends at the
 * upper bin border (inclusive).
 * 
 * @author illenberger
 * 
 */
public class FixedBordersDiscretizer implements Discretizer {

	private double[] borders;

	/**
	 * Creates a new discretizer with the borders specified in <tt>borders</tt>.
	 * 
	 * @param borders
	 *            an array with borders.
	 */
	public FixedBordersDiscretizer(double[] borders) {
		this.borders = borders;
		Arrays.sort(this.borders);
	}

	/**
	 * Creates a new discretizer with the borders specified in <tt>borders</tt>
	 * but additionally adds a lower border <tt>min[values] - 1E-10</tt> and an
	 * upper border <tt>max[values]</tt>, so that if only values contained in
	 * <tt>values</tt> are passed to {@link #discretize(double)} or
	 * {@link #binWidth(double)} both methods never return
	 * {@link Double#POSITIVE_INFINITY}.
	 * 
	 * @param values
	 *            an array of values.
	 * @param borders
	 *            an array with borders.
	 */
	public FixedBordersDiscretizer(double[] values, double[] borders) {
		double min = StatUtils.min(values);
		double max = StatUtils.max(values);

		min = min - 1E-10;

		double[] newBorders = new double[borders.length + 2];
		newBorders[0] = min;
		for (int i = 0; i < borders.length; i++)
			newBorders[i + 1] = borders[i];

		newBorders[newBorders.length - 1] = max;

		this.borders = newBorders;
		Arrays.sort(this.borders);
	}

	/**
	 * @param value
	 *            a value.
	 * @return the upper bin border of <tt>value</tt> or
	 *         {@link Double#POSITIVE_INFINITY} if <tt>value</tt> is greater
	 *         than the greatest border.
	 */
	@Override
	public double discretize(double value) {
		int idx = index(value);
		if (idx >= borders.length)
			return Double.POSITIVE_INFINITY;
		else
			return borders[idx];
	}

	/**
	 * @param value
	 *            a value.
	 * @returns the width of the bin of <tt>value</tt> or
	 *          {@link Double#POSITIVE_INFINITY} if <tt>value</tt> is less than
	 *          the smallest border or greater than the greatest border.
	 */
	@Override
	public double binWidth(double value) {
		int idx = index(value);
		if (idx == 0 || idx >= borders.length)
			return Double.POSITIVE_INFINITY;
		else
			return borders[idx] - borders[idx - 1];
	}

	/**
	 * @see {@link Discretizer#index(double)}
	 */
	@Override
	public int index(double value) {
		int idx = Arrays.binarySearch(borders, value);
		if (idx > -1) {
			return idx;
		} else {
			return -idx - 1;
		}
	}
}