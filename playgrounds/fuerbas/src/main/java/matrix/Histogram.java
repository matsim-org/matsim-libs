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
package matrix;

import gnu.trove.TDoubleArrayList;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
	
	public static void main(String args[]) throws NumberFormatException, IOException {
		double betw = 0.;
		int ii = 0;
		BufferedReader br = new BufferedReader(new FileReader(args[0]));	//provide file with results from MatrixCentrality in format: Matsim Id "TAB" Betweenness
		while (br.ready()) {
			br.readLine();
			ii++;
		}
		br.close();
		
		double[] samples = new double[ii+1];
		
		int jj = 0;

		BufferedReader br2 = new BufferedReader(new FileReader(args[0]));
		while (br2.ready()) {
			String aLine = br2.readLine();
			String splitLine[] = aLine.split("\t");
			betw = Double.parseDouble(splitLine[1]);
			samples[jj]=betw;
			jj++;
		}
		br2.close();
		
		Histogram hist = new Histogram(samples);
		hist.convertToEqualCount(1, 1.0);
		double k1 = 0.;
		for (int i=0; i<1000; i++) {
			k1 += hist.share(i);
		}
		System.out.println("Betw < 1000: "+k1);
		k1 = 0.;
		for (int i=1000; i<5000; i++) {
			k1 += hist.share(i);
		}
		System.out.println("1000 < Betw < 5000: "+k1);
		k1 = 0.;
		for (int i=5000; i<15000; i++) {
			k1 += hist.share(i);
		}
		System.out.println("5000 < Betw < 15000: "+k1);
		k1 = 0.;
		for (int i=15000; i<30000; i++) {
			k1 += hist.share(i);
		}
		System.out.println("15000 < Betw < 30000: "+k1);
		k1 = 0.;
		for (int i=30000; i<100000; i++) {
			k1 += hist.share(i);
		}
		System.out.println("30000 < Betw < 100000: "+k1);
		k1 = 0.;
		for (int i=15000; i<30000; i++) {
			k1 += hist.share(i);
		}
		System.out.println("100000 < Betw < 1000000: "+k1);
		k1 = 0.;
		for (int i=15000; i<30000; i++) {
			k1 += hist.share(i);
		}
		System.out.println("1000000 < Betw < 1000000000: "+k1);
	}
}
