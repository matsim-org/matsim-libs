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
package playground.johannes.socialnetworks.snowball2.sim.deprecated;

import gnu.trove.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author illenberger
 *
 */
public class Histogram {

	private double[] samples;
	
//	private double[] weights;
	
	private List<TDoubleArrayList> binSamples;
	
	private double[] binCounts;
	
	private double[] binWidths;
	
	private double[] lowerBorders;
	
	private double normConstant;
	
	public Histogram(double[] samples) {
		this.samples = samples;
	}
	
	
	public void convertToEqualCount(int minCount, double minBinSize) {
		if(samples.length == 0)
			return;
		/*
		 * sort samples
		 */
		Arrays.sort(samples);
		/*
		 * create new bins
		 */
		binSamples = new ArrayList<TDoubleArrayList>((int)Math.ceil(samples.length/(double)minCount));
		/*
		 * set the previous sample to a very small number
		 */
		double prevSample = - Double.MAX_VALUE;
		TDoubleArrayList currentBin = new TDoubleArrayList();
		
		for(int i = 0; i < samples.length; i++) {
			double currentSample = samples[i];
			/*
			 * do not put samples of same value into different bins!
			 */
			if(currentSample > prevSample) {
				if(currentBin.size() >= minCount) {
					/*
					 * bins is full, add it to the bins and create a new one
					 */
					binSamples.add(currentBin);
					currentBin = new TDoubleArrayList();
				}
			}
			currentBin.add(samples[i]);
			prevSample = currentSample;
		}
		/*
		 * add the last bin to the bins
		 */
		binSamples.add(currentBin);
		/*
		 * get the lower bin borders
		 */
		lowerBorders = new double[binSamples.size()+1];
		for(int i = 0; i < binSamples.size(); i++) {
			lowerBorders[i] = binSamples.get(i).get(0);
		}
		if(lowerBorders.length > 1)
			lowerBorders[lowerBorders.length-1] = samples[samples.length - 1] + minBinSize;
		
		binCounts = new double[binSamples.size()];
		binWidths = new double[binSamples.size()];
		double wSum = 0;
		for(int i = 0; i < binSamples.size(); i++) {
			TDoubleArrayList bin = binSamples.get(i);
//			for(int k = 0; k < bin.size(); k++)
			binCounts[i] = bin.size();
			binWidths[i] = lowerBorders[i + 1] - lowerBorders[i];
			wSum += binCounts[i];///binWidths[i];
		}
		normConstant = 1/wSum;
	}
	
	public double share(double value) {
		int idx = getIndex(value);
		if(idx < 0 || idx >= binCounts.length)
			return 0.0;
		else {
			return binCounts[idx]/binWidths[idx] * normConstant;
		}
			
	}
	
	private int getIndex(double x) {
		int idx = Arrays.binarySearch(lowerBorders, x);
		if(idx >= 0) {
			return idx;
		} else {
			return -idx - 2;
		}
	}
	
	public static void main(String args[]) {
		double[] sampels = new double[]{1,1,1,2,2,2,3,3,4,4};
		Histogram hist = new Histogram(sampels);
		hist.convertToEqualCount(3, 1.0);
		System.out.println(hist.share(-0.9));
	}
}
