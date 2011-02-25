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
package org.matsim.contrib.sna.math;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleIntHashMap;

import java.util.Arrays;

/**
 * A discretizer with fixed bin borders defined such that each bin would contain
 * approximately the same number of samples given at construction.
 * 
 * @author illenberger
 * 
 */
public class FixedSampleSizeDiscretizer {

	/**
	 * Creates a new discretizer with bin borders defined such that each bin
	 * would contain approximately contain <tt>size</tt> samples from
	 * <tt>samples</tt>.
	 * 
	 * @param samples
	 *            an array with samples.
	 * @param size
	 *            the number of samples per bin.
	 * @return a new discretizer.
	 */
	public static FixedBordersDiscretizer create(double[] samples, int size) {
		TDoubleArrayList borders;
		Arrays.sort(samples);
		TDoubleIntHashMap hist = new TDoubleIntHashMap(samples.length);
		for (int i = 0; i < samples.length; i++) {
			hist.adjustOrPutValue(samples[i], 1, 1);
		}

		double keys[] = hist.keys();
		Arrays.sort(keys);
		borders = new TDoubleArrayList(keys.length);

		borders.add(samples[0] - 1E-10);

		int binsize = 0;
		for (int i = 0; i < keys.length; i++) {
			binsize += hist.get(keys[i]);
			if (binsize >= size && i > 0) {
				borders.add(keys[i]);
				binsize = 0;
			}
		}

		if (binsize > 0)
			borders.add(samples[samples.length - 1]);

		return new FixedBordersDiscretizer(borders.toNativeArray());
	}

	/**
	 * Creates a new discretizer with bin borders defined such that samples are
	 * roughly equally distributed over <tt>bins</tt> number of bins, but at
	 * least <tt>size</tt> samples per bin.
	 * 
	 * @param samples
	 *            an array with samples.
	 * @param size
	 *            the minimum number of samples per bin.
	 * @param bins
	 *            the number of bins.
	 * @return a new discretizer.
	 */
	public static FixedBordersDiscretizer create(double[] samples, int size, int bins) {
		int newsize = samples.length / bins;
		newsize = Math.max(newsize, size);
		return create(samples, newsize);
	}
}
