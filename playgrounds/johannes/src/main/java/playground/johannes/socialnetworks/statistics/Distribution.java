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
package playground.johannes.socialnetworks.statistics;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleFunction;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.core.utils.io.IOUtils;

/**
 * @author illenberger
 *
 */
public class Distribution {

	private TDoubleArrayList values = new TDoubleArrayList();
	
	private TDoubleArrayList weights = new TDoubleArrayList();
	
	public Distribution() {
		
	}
	
	public Distribution(double[] values) {
		addAll(values);
	}
	
	public void add(double value) {
		add(value, 1.0);
	}
	
	public void add(double value, double weigth) {
		values.add(value);
		weights.add(weigth);
	}
	
	public void addAll(double[] vals) {
		double[] wghts = new double[vals.length];
		Arrays.fill(wghts, 1.0);
		addAll(vals, wghts);
	}
	
	public void addAll(double[] vals, double[] wghts) {
		values.add(vals);
		weights.add(wghts);
	}
	
	public double[] getValues() {
		return values.toNativeArray();
	}
	
	public double[] getWeights() {
		return weights.toNativeArray();
	}
	
	public double max() {
		int size = values.size();
		double max = - Double.MAX_VALUE;
		for(int i = 0; i < size; i++)
			max = Math.max(max, values.get(i));
		return max;
	}
	
	public double min() {
		int size = values.size();
		double min = Double.MAX_VALUE;
		for(int i = 0; i < size; i++)
			min = Math.min(min, values.get(i));
		return min;
	}
	
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
	
	public double varianceCoefficient() {
		return Math.sqrt(variance()) / mean();
	}
	
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
	
	public TDoubleDoubleHashMap normalizedDistribution() {
		return normalizedDistribution(absoluteDistribution());
	}
	
	public TDoubleDoubleHashMap normalizedDistribution(double binsize) {
		return normalizedDistribution(absoluteDistribution(binsize));
	}
	
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
