/* *********************************************************************** *
 * project: org.matsim.*
 * GraphStatistic.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes.snowball2;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.utils.io.IOUtils;

import playground.johannes.snowball.Histogram;
import edu.uci.ics.jung.graph.Graph;
import gnu.trove.TObjectDoubleHashMap;

/**
 * @author illenberger
 *
 */
public abstract class GraphStatistic {
	
	protected static final String TAB = "\t";
	
	protected static final String FLOAT_FORMAT = "%1.4f";
	
	protected static final String MIN_KEY = "min";
	
	protected static final String MAX_KEY = "max";
	
	protected static final String MEAN_KEY = "mean";
	
	protected static final String VARIANCE_KEY = "variance";
	
	protected static final String SKEWNESS_KEY = "skewness";
	
	protected static final String KURTOSIS_KEY = "kurtosis";

	protected String outputDir;
	
	private BufferedWriter statsWriter;
	
	private List<String> statsKeys;
	
	public GraphStatistic(String outputDir) {
		this.outputDir = outputDir;
		try {
			statsWriter = IOUtils.getBufferedWriter(outputDir + "/statistics.txt");
			dumpStatisticsHeader();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public abstract DescriptiveStatistics calculate(Graph g, int iteration, DescriptiveStatistics reference);

	protected List<String> getStatisticsKeys() {
		if(statsKeys == null) {
			statsKeys = new LinkedList<String>();
			statsKeys.add(MIN_KEY);
			statsKeys.add(MAX_KEY);
			statsKeys.add(MEAN_KEY);
			statsKeys.add(VARIANCE_KEY);
			statsKeys.add(SKEWNESS_KEY);
			statsKeys.add(KURTOSIS_KEY);
		}
		return statsKeys;
	}
	
	protected void dumpStatisticsHeader() {
		List<String> keys = getStatisticsKeys();
		try {
			statsWriter.write("iter");
			for(String key : keys) {
				statsWriter.write(TAB);
				statsWriter.write(key);
			}
			statsWriter.newLine();
			statsWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected void dumpStatistics(TObjectDoubleHashMap<String> statsMap, int iteration) {
		try {
			statsWriter.write(String.valueOf(iteration));
			for(String key : statsKeys) {
				statsWriter.write(TAB);
				statsWriter.write(String.format(Locale.US, FLOAT_FORMAT, statsMap.get(key)));
			}
			statsWriter.newLine();
			statsWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected TObjectDoubleHashMap<String> getStatisticsMap(DescriptiveStatistics stats) {
		TObjectDoubleHashMap<String> statsMap = new TObjectDoubleHashMap<String>();
		statsMap.put(MIN_KEY, stats.getMin());
		statsMap.put(MAX_KEY, stats.getMax());
		statsMap.put(MEAN_KEY, stats.getMean());
		statsMap.put(VARIANCE_KEY, stats.getVariance());
		statsMap.put(SKEWNESS_KEY, stats.getSkewness());
		statsMap.put(KURTOSIS_KEY, stats.getKurtosis());
		return statsMap;
	}
	
	protected void plotHistogram(double[] values, Histogram hist, int iteration) {
		hist.addAll(values);
		try {
			hist.plot(String.format("%1$s/%2$s.histogram.png", outputDir, iteration), "Histogram");
			hist.dumpRawData(String.format("%1$s/%2$s.histogram.txt", outputDir, iteration));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void plotHistogram(double[] values, double[] weights, Histogram hist, int iteration) {
		hist.addAll(values, weights);
		try {
			hist.plot(String.format("%1$s/%2$s.histogram.png", outputDir, iteration), "Histogram");
			hist.dumpRawData(String.format("%1$s/%2$s.histogram.txt", outputDir, iteration));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected double calcGammaExponent(double[] values, double fixedBinSize, double minVal) {
		double[] weights = new double[values.length];
		Arrays.fill(weights, 1.0);
		return calcGammaExponent(values, weights, fixedBinSize, minVal);
	}
	
	protected double calcGammaExponent(double[] values, double[] weights, double fixedBinSize, double minVal) {
		/*
		 * (1) Determine probability distribution.
		 */
		int numBins = 1000;//values.length / 10;
		double min = StatUtils.min(values);
		double max = StatUtils.max(values);
		
		double binSize;
		if(fixedBinSize > 0) {
			binSize = fixedBinSize;
			numBins = (int)Math.ceil((max - min)/binSize);
		}
		else
			binSize = (max - min)/(double)numBins;
		
		double[] bins = new double[numBins + 1];
		for(int i = 0; i < values.length; i++) {
			int idx = (int)Math.floor((values[i] - min)/binSize);
			bins[idx] += weights[i];
		}
		/*
		 * (2) Get value with the highest probability.
		 */
		int maxHeightBinIdx = -1;
		double maxHeight = Double.MIN_VALUE;
		for(int i = 0; i < bins.length; i++) {
			if(maxHeight < bins[i]) {
				maxHeight = bins[i];
				maxHeightBinIdx = i;
			}
		}
		double minPowerLawVal = (maxHeightBinIdx * binSize) + min;
		if(minPowerLawVal == 0)
			minPowerLawVal = Double.MIN_VALUE;
		
		//=======================================================
		if(minVal > 0)
			minPowerLawVal = minVal;
		/*
		 * (3) Calculate gamma with maximum likelihood estimator.
		 */
		double logsum = 0;
		double wsum = 0;
		int count = 0;
		for(int i = 0; i < values.length; i++) {
			if(values[i] >= minPowerLawVal) {
				logsum += Math.log((values[i]/minPowerLawVal)) * weights[i];
				wsum += weights[i];
//				wsum++;
				count++;
			}
		}
		System.out.println("Count = " +count+"; wsum = " + wsum);
		return 1 + (wsum/logsum);
	}
	
}
