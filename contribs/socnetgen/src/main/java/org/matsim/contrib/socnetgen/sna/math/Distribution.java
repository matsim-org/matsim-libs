/* *********************************************************************** *
 * project: org.matsim.*
 * WeightedStatistics.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.math;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleFunction;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

/**
 * Representation of an empirical discrete distribution. Allows to apply weights
 * to samples.
 * 
 * @author illenberger
 * 
 */
public class Distribution {

	private TDoubleArrayList values = new TDoubleArrayList();
	
	private TDoubleArrayList weights = new TDoubleArrayList();
	
	/**
	 * Creates an empty distribution.
	 */
	public Distribution() {
	}
	
	/**
	 * Creates a distribution containing the values of <tt>values</tt>.
	 * 
	 * @param values a set of samples.
	 */
	public Distribution(double[] values) {
		addAll(values);
	}
	
	/**
	 * Adds a sample to the distribution (with weight 1.0).
	 * 
	 * @param value s sample.
	 */
	public void add(double value) {
		add(value, 1.0);
	}
	
	/**
	 * Add a weighted sample to the distribution.
	 * 
	 * @param value a sample.
	 * @param weigth the weight.
	 */
	public void add(double value, double weigth) {
		values.add(value);
		weights.add(weigth);
	}
	
	/**
	 * Adds all samples form <tt>vals</tt> to the distribution (with weight 1.0).
	 * 
	 * @param vals a set of samples.
	 */
	public void addAll(double[] vals) {
		double[] wghts = new double[vals.length];
		Arrays.fill(wghts, 1.0);
		addAll(vals, wghts);
	}

	/**
	 * Adds all samples from <tt>vals</tt> to the distribution weight with the
	 * weights in <tt>weights</tt>. Both arrays must have same length.
	 * 
	 * @param vals a set of samples.
	 * @param wghts the weights.
	 */
	public void addAll(double[] vals, double[] wghts) {
		values.add(vals);
		weights.add(wghts);
	}
	
	/**
	 * Returns the set of samples.
	 * 
	 * @return the set of samples.
	 */
	public double[] getValues() {
		return values.toNativeArray();
	}
	
	/**
	 * Returns the weights.
	 * 
	 * @return the weights.
	 */
	public double[] getWeights() {
		return weights.toNativeArray();
	}
	
	/**
	 * Returns the maximum value of all samples.
	 * 
	 * @return the maximum value of all samples.
	 */
	public double max() {
		int size = values.size();
		double max = - Double.MAX_VALUE;
		for(int i = 0; i < size; i++)
			max = Math.max(max, values.get(i));
		return max;
	}
	
	/**
	 * Returns the minimum value of all samples.
	 * 
	 * @return the minimum value of all samples.
	 */
	public double min() {
		int size = values.size();
		double min = Double.MAX_VALUE;
		for(int i = 0; i < size; i++)
			min = Math.min(min, values.get(i));
		return min;
	}
	
	/**
	 * Returns the (weighted) mean of all samples.
	 * 
	 * @return the (weighted) mean of all samples.
	 */
	public double mean() {
		int size = values.size();
		double vsum = 0;
		double wsum = 0;
		for(int i = 0; i < size; i++) {
			vsum += values.get(i) * weights.get(i);
			wsum += weights.get(i);
		}
		
		return vsum/wsum;
	}
	
	/**
	 * Returns the (weighted) variance of all samples.
	 * 
	 * @return the (weighted) variance of all samples.
	 */
	public double variance() {
		double mu = mean();
		double devsum = 0;
		double wsum = 0;
		int size = values.size();
		for(int i = 0; i < size; i++) {
			double dev = values.get(i) - mu;
			devsum += dev * dev * weights.get(i);
			wsum += weights.get(i);
		}
		
		return devsum/wsum;
	}
	
	/**
	 * Returns the coefficient of variance.
	 * 
	 * @return the coefficient of variance.
	 */
	public double coefficientOfVariance() {
		return Math.sqrt(variance()) / mean();
	}
	
	/**
	 * Returns the (weighted) skewness of all samples.
	 * 
	 * @return the (weighted) skewness of all samples.
	 */
	public double skewness() {
		double mu = mean();
		double devsum = 0;
		double wsum = 0;
		int size = values.size();
		for(int i = 0; i < size; i++) {
			double dev = values.get(i) - mu;
			devsum += dev * dev * dev * weights.get(i);
			wsum += weights.get(i);
		}
		double mu3 = devsum/wsum;
		double s3 = Math.pow(variance(), 3.0/2.0);
		
		return mu3/s3;
	}
	
	/**
	 * Returns the (weighted) kurtosis (not excess kurtosis) of all samples.
	 * 
	 * @return the (weighted) kurtosis (not excess kurtosis) of all samples.
	 */
	public double kurtosis() {
		double mu = mean();
		double devsum = 0;
		double wsum = 0;
		int size = values.size();
		for(int i = 0; i < size; i++) {
			double dev = values.get(i) - mu;
			devsum += Math.pow(dev, 4.0d) * weights.get(i);
			wsum += weights.get(i);
		}
		double mu4 = devsum/wsum;
		double s4 = Math.pow(variance(), 4.0/2.0);
		
		return mu4/s4;
	}
	
	/**
	 * Returns a histogram of all samples.
	 * 
	 * @return a histogram of all samples.
	 */
	public TDoubleDoubleHashMap absoluteDistribution() {
		TDoubleDoubleHashMap freq = new TDoubleDoubleHashMap();
		int size = values.size();
		for(int i = 0; i < size; i++) {
			double cumWeight = freq.get(values.get(i));
			cumWeight += weights.get(i);
			freq.put(values.get(i), cumWeight);
		}
		return freq;
	}

	/**
	 * Returns a histogram of all samples where samples are aggregated into
	 * bins of width <tt>binsize</tt>. The key of the bin is the upper bin
	 * border.
	 * 
	 * @param binsize
	 *            the width of bins.
	 * @return a histogram of all samples.
	 */
	public TDoubleDoubleHashMap absoluteDistribution(double binsize) {
		TDoubleDoubleHashMap freq = new TDoubleDoubleHashMap();
		int size = values.size();
		for(int i = 0; i < size; i++) {
			double val = Math.ceil(values.get(i) / binsize) * binsize;
			double cumWeight = freq.get(val);
			cumWeight += weights.get(i);
			 
			freq.put(val, cumWeight);
		}
		return freq;
	}

	/**
	 * Returns a histogram of all samples where samples are aggregated into
	 * bins with logarithmically scaled width. The key of the bin is the upper bin
	 * border.
	 * 
	 * @param descretization a constant to decretize samples before they are aggregated into log bins.
	 * @return a histogram of all samples.
	 */
	public TDoubleDoubleHashMap absoluteDistributionLog10(double descretization) {
		TDoubleDoubleHashMap freq = new TDoubleDoubleHashMap();
		int size = values.size();
		for(int i = 0; i < size; i++) {
			double bin_idx = Math.ceil(Math.log10(values.get(i)/descretization));
			bin_idx = Math.max(bin_idx, 0.0);
			double binWidth = Math.pow(10, bin_idx) - Math.pow(10, bin_idx-1);
			binWidth = Math.max(1.0, binWidth);
			freq.adjustOrPutValue(Math.pow(10, bin_idx)*descretization, weights.get(i)/binWidth, weights.get(i)/binWidth);
		}
		return freq;
	}
	
	/**
	 * Returns a histogram of all samples where samples are aggregated into
	 * bins with log2 scaled width. The key of the bin is the upper bin
	 * border.
	 * 
	 * @param descretization a constant to discretize samples before they are aggregated into log bins.
	 * @return a histogram of all samples.
	 */
	public TDoubleDoubleHashMap absoluteDistributionLog2(double descretization) {
		TDoubleDoubleHashMap freq = new TDoubleDoubleHashMap();
		int size = values.size();
		for(int i = 0; i < size; i++) {
			double bin = Math.ceil(Math.log(values.get(i)/descretization)/Math.log(2.0));
			bin = Math.max(bin, 0.0);
			double binWidth = Math.pow(2, bin) - Math.pow(2, bin-1);
			binWidth = Math.max(1.0, binWidth);
			freq.adjustOrPutValue(Math.pow(2, bin)*descretization, weights.get(i)/binWidth, weights.get(i)/binWidth);
		}
		return freq;
	}

	/**
	 * Returns a histogram where samples are first discretized into equal size
	 * bins of width <tt>discretization</tt> and the further discretized into
	 * log-scaled bins.
	 * 
	 * @param discretization
	 *            bin width of equal size bins
	 * @param base
	 *            log base
	 * @return a histogram.
	 */
	public TDoubleDoubleHashMap absoluteDistributionLog(double discretization, double base) {
		TDoubleDoubleHashMap freq = new TDoubleDoubleHashMap();
		int size = values.size();
		for(int i = 0; i < size; i++) {
			double bin = Math.ceil(Math.log(values.get(i)/discretization)/Math.log(base));
			bin = Math.max(bin, 0.0);
			double binWidth = Math.pow(base, bin) - Math.pow(base, bin-1);
			binWidth = Math.max(1.0, binWidth);
			freq.adjustOrPutValue(Math.pow(base, bin)*discretization, weights.get(i)/binWidth, weights.get(i)/binWidth);
		}
		return freq;
	}
	
	/**
	 * Returns a histogram where samples are aggregated into bins where each bin
	 * contains approximately the same number of samples.
	 * 
	 * @param minSize
	 *            the approximately number of samples per bin
	 * @return a histogram.
	 */
	public TDoubleDoubleHashMap absoluteDistributionFixed(int minSize) {
		Discretizer discretizer = FixedSampleSizeDiscretizer.create(values.toNativeArray(), minSize);
		TDoubleDoubleHashMap freq = new TDoubleDoubleHashMap();
		int size = values.size();
		for(int i = 0; i < size; i++) {
			double val = values.get(i);
			double bin = discretizer.discretize(val);
			double binWidth = discretizer.binWidth(val);
			freq.adjustOrPutValue(bin, weights.get(i)/binWidth, weights.get(i)/binWidth);
		}
		return freq;
	}
	/**
	 * Returns a histogram of all samples where the values are normalized so
	 * that the sum of all samples equals one.
	 * 
	 * @return a normalized histogram of all samples.
	 */
	public TDoubleDoubleHashMap normalizedDistribution() {
		return normalizedDistribution(absoluteDistribution());
	}
	
	/**
	 * Returns a histogram of all samples where the values are first aggregated
	 * into bins of width <tt>binsize</tt> and then normalized so that the sum
	 * of all discretized samples equals one.
	 * 
	 * @return a normalized histogram of all samples.
	 */
	public TDoubleDoubleHashMap normalizedDistribution(double binsize) {
		return normalizedDistribution(absoluteDistribution(binsize));
	}
	
	/**
	 * Creates a normalized histogram out of any other histogram.
	 * 
	 * @param distr the source histogram.
	 * @return a normalized histogram.
	 */
	public TDoubleDoubleHashMap normalizedDistribution(TDoubleDoubleHashMap distr) {
		double sum = 0;
		double[] values = distr.getValues();
		
		for(int i = 0; i < values.length; i++) {
			sum += values[i];
		}
		final double norm = 1/sum;
		TDoubleFunction fct = new TDoubleFunction() {
			public double execute(double value) {
				return value * norm;
			}
		
		};
		
		distr.transformValues(fct);
		return distr;
	}

	/**
	 * Writes a histogram as a TAB-separated file with two columns: the first
	 * column indicates the bin key (e.g. upper bin border), the second column
	 * displays the count/share values.
	 * 
	 * @param distr a histogram.
	 * @param filename the filename.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void writeHistogram(TDoubleDoubleHashMap distr, String filename) throws FileNotFoundException, IOException {
		BufferedWriter aWriter = IOUtils.getBufferedWriter(filename);
		aWriter.write("bin\tcount");
		aWriter.newLine();
		double[] keys = distr.keys();
		Arrays.sort(keys);
		for(double key : keys) {
			aWriter.write(String.valueOf(key));
			aWriter.write("\t");
			aWriter.write(String.valueOf(distr.get(key)));
			aWriter.newLine();
		}
		aWriter.close();
	}
	
	/**
	 * Calculates the mean square error of two distributions.
	 * 
	 * @param estimation the estimated distribution.
	 * @param observation the observed distribution.
	 * @return the mean square error.
	 */
	public static double meanSquareError(TDoubleDoubleHashMap estimation, TDoubleDoubleHashMap observation) {
		double square_sum = 0;
		double[] keys = estimation.keys();
		for(double bin : keys) {
			double X = estimation.get(bin);
			
			if(!observation.containsKey(bin))
				throw new IllegalArgumentException("The observed distribution has no value for " + bin);
			
			double x = observation.get(bin);
			double diff = X - x;
			square_sum += diff * diff;
		}
		
		return square_sum/(double)keys.length;
	}
}
