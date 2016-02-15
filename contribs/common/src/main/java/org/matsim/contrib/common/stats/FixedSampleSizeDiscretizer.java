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

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TDoubleIntHashMap;
import org.apache.commons.math.stat.StatUtils;

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
	 * would contain approximately <tt>size</tt> samples from <tt>samples</tt>.
	 * 
	 * Samples are sorted into bins in ascending order. If there are not
	 * sufficient (less than <tt>size</tt>) samples to fill a further bin, the
	 * remaining samples are sorted into the last bin. That is, the last bin is
	 * the only bin that may contain more than <tt>size</tt> samples.
	 * 
	 * @param samples
	 *            an array with samples.
	 * @param size
	 *            the number of samples per bin.
	 * @return a new discretizer.
	 */
	public static FixedBordersDiscretizer create(double[] samples, int size) {
		TDoubleArrayList borders;

		double min = Double.MAX_VALUE;
		double max = - Double.MAX_VALUE;

		TDoubleIntHashMap hist = new TDoubleIntHashMap(samples.length);
		for (int i = 0; i < samples.length; i++) {
			hist.adjustOrPutValue(samples[i], 1, 1);
			min = Math.min(min, samples[i]);
			max = Math.max(max, samples[i]);
		}

		double keys[] = hist.keys();
		Arrays.sort(keys);
		borders = new TDoubleArrayList(keys.length);

		borders.add(min - 1E-10);

		int binsize = 0;
		int n = 0;
		for (int i = 0; i < keys.length; i++) {
			int nBin = hist.get(keys[i]);
			binsize += nBin;
			n += nBin;
			if (binsize >= size && i > 0) { // sufficient samples for the
											// current bin
				if (samples.length - n >= binsize) { // sufficient remaining
														// samples to fill the
														// next bin
					borders.add(keys[i]);
					binsize = 0;
				}
			}
		}

		if (binsize > 0)
			borders.add(max);

		return new FixedBordersDiscretizer(borders.toArray());
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
		int newsize = (int) Math.ceil(samples.length / (double) bins);
		newsize = Math.max(newsize, size);
		return create(samples, newsize);
	}
}
