/* *********************************************************************** *
 * project: org.matsim.*
 * Histogram.java
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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TDoubleFunction;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.util.Arrays;

/**
 * Utility functions to generate histograms.
 * 
 * @author illenberger
 * 
 */
public class Histogram {

	/**
	 * Creates a histogram from the values in <tt>stats</tt> discretized with
	 * <tt>discretizer</tt>. Correctly handles {@link DescriptivePiStatistics}
	 * objects.
	 * 
	 * @param stats
	 *            a descriptive statistics object
	 * @param discretizer
	 *            a discretizer
	 * @return a double-double map where the key denotes the bin and the value
	 *         the bin height.
	 */
	public static TDoubleDoubleHashMap createHistogram(DescriptiveStatistics stats, Discretizer discretizer, boolean reweight) {
		return createHistogram(stats, discretizer, reweight, false);
	}
	
	public static TDoubleDoubleHashMap createHistogram(DescriptiveStatistics stats, Discretizer discretizer, boolean reweight, boolean reverse) {
		if (stats instanceof DescriptivePiStatistics)
			return createHistogram((DescriptivePiStatistics) stats, discretizer, reweight, reverse);
		else
			return createHistogram(stats.getValues(), discretizer, reweight, reverse);
	}

	/**
	 * Creates a histogram from the values in <tt>stats</tt> discretized with
	 * <tt>discretizer</tt> and weighted with the inverse of the samples'
	 * pi-values.
	 * 
	 * @param stats
	 *            a descriptive statistics object
	 * @param discretizer
	 *            a discretizer
	 * @return a double-double map where the key denotes the bin and the value
	 *         the bin height.
	 */
	public static TDoubleDoubleHashMap createHistogram(DescriptivePiStatistics stats, Discretizer discretizer, boolean reweight) {
		return createHistogram(stats, discretizer, reweight, false);
	}
	
	public static TDoubleDoubleHashMap createHistogram(DescriptivePiStatistics stats, Discretizer discretizer, boolean reweight, boolean reverse) {
		double[] piValues = stats.getPiValues();
		double[] weights = new double[piValues.length];
		for (int i = 0; i < piValues.length; i++) {
			weights[i] = 1 / piValues[i];
		}

		return createHistogram(stats.getValues(), weights, discretizer, reweight, reverse);
	}

	/**
	 * Creates a histogram out of <tt>values</tt> discretized with
	 * <tt>discretizer</tt>.
	 * 
	 * @param values
	 *            the samples
	 * @param discretizer
	 *            a discretizer
	 * @return a double-double map where the key denotes the bin and the value
	 *         the bin height.
	 */
	public static TDoubleDoubleHashMap createHistogram(double[] values, Discretizer discretizer, boolean reweight) {
		return createHistogram(values, discretizer, reweight, false);
	}
	
	public static TDoubleDoubleHashMap createHistogram(double[] values, Discretizer discretizer, boolean reweight, boolean reverse) {
		double[] weights = new double[values.length];
		Arrays.fill(weights, 1.0);
		return createHistogram(values, weights, discretizer, reweight, reverse);
	}

	/**
	 * Creates a histogram out of <tt>values</tt> discretized with
	 * <tt>discretizer</tt> and weighted with the values in <tt>weights</tt>.
	 * 
	 * @param values
	 *            the samples
	 * @param weights
	 *            the weights
	 * @param discretizer
	 *            a discretizer
	 * @param reweight TODO
	 * @return a double-double map where the key denotes the bin and the value
	 *         the bin height.
	 */
	public static TDoubleDoubleHashMap createHistogram(double[] values, double[] weights, Discretizer discretizer, boolean reweight) {
		return createHistogram(values, weights, discretizer, reweight, false);
	}
	
	public static TDoubleDoubleHashMap createHistogram(double[] values, double[] weights, Discretizer discretizer, boolean reweight, boolean reverse) {
		TDoubleDoubleHashMap histogram = new TDoubleDoubleHashMap();
		for (int i = 0; i < values.length; i++) {
			double bin = discretizer.discretize(values[i]);
			double weight = weights[i];
			if(reweight) {
				if(reverse)
					weight = weights[i] * discretizer.binWidth(values[i]);
				else
					weight = weights[i] / discretizer.binWidth(values[i]);
			}
			
			histogram.adjustOrPutValue(bin, weight, weight);
		}
		
		return histogram;
	}
	
	/**
	 * Normalizes a histogram so that the sum of all bin heights equals 1.
	 * 
	 * @param histogram
	 *            a histogram
	 * @return a normalized histogram.
	 */
	public static TDoubleDoubleHashMap normalize(TDoubleDoubleHashMap histogram) {
		double sum = 0;
		double[] values = histogram.getValues();

		for (int i = 0; i < values.length; i++) {
			sum += values[i];
		}

		return normalize(histogram, sum);
	}
	
	public static TDoubleDoubleHashMap normalize(TDoubleDoubleHashMap histogram, double sum) {
		final double norm = 1 / sum;

		TDoubleFunction fct = new TDoubleFunction() {
			public double execute(double value) {
				return value * norm;
			}

		};

		histogram.transformValues(fct);

		return histogram;

	}
	
	public static double sum(TDoubleDoubleHashMap histogram) {
		TDoubleDoubleIterator it = histogram.iterator();
		double sum = 0;
		for(int i = 0; i < histogram.size(); i++) {
			it.advance();
			sum += it.value();
		}
		return sum;
	}
	
	public static TDoubleDoubleHashMap createCumulativeHistogram(TDoubleDoubleHashMap histogram) {
		double[] keys = histogram.keys();
		Arrays.sort(keys);
		double sum = 0;
		for(double key : keys) {
			double val = histogram.get(key);
			sum += val;
			histogram.put(key, sum);
		}
		
		return histogram;
	}
	
	public static TDoubleDoubleHashMap normalizeCumulative(TDoubleDoubleHashMap histogram) {
		return normalizeCumulative(histogram, 1.0);
	}
	
	public static TDoubleDoubleHashMap normalizeCumulative(TDoubleDoubleHashMap histogram, double sum) {
		double[] keys = histogram.keys();
		double max = StatUtils.max(keys);
		
		final double norm = sum/histogram.get(max);
		
		TDoubleFunction fct = new TDoubleFunction() {
			public double execute(double value) {
				return value * norm;
			}

		};

		histogram.transformValues(fct);

		return histogram;
	}
	
	public static TDoubleDoubleHashMap complementary(TDoubleDoubleHashMap histogram) {
		TDoubleFunction fct = new TDoubleFunction() {
			public double execute(double value) {
				return 1 - value;
			}

		};

		histogram.transformValues(fct);

		return histogram;
	}
}
