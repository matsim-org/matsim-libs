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
package playground.johannes.socialnetworks.snowball;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.GraphProjection;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.core.utils.io.IOUtils;

import playground.johannes.socialnetworks.statistics.Distribution;


/**
 * @author illenberger
 *
 */
public abstract class GraphPropertyEstimator {
	
	protected static final String TAB = "\t";
	
	protected static final String FLOAT_FORMAT = "%1.14f";
	
	protected static final String MIN_KEY = "min";
	
	protected static final String MAX_KEY = "max";
	
	protected static final String MEAN_KEY = "mean";
	
	protected static final String VARIANCE_KEY = "variance";
	
	protected static final String SKEWNESS_KEY = "skewness";
	
	protected static final String KURTOSIS_KEY = "kurtosis";

	protected String outputDir;
	
	private List<String> files;
	
	private BufferedWriter obsWriter;
	
	private BufferedWriter estimWriter;
	
	private List<String> statsKeys;
	
	public GraphPropertyEstimator(String outputDir) {
		this.outputDir = outputDir;
		files = new ArrayList<String>();
		
	}

	public List<String> getFiles() {
		return files;
	}
	
	public abstract DescriptiveStatistics calculate(GraphProjection<SampledGraph, SampledVertex, SampledEdge> graph, int iteration);

	protected void openStatsWriters(String name) {
		try {
			String path = String.format("%1$s/%2$s.observed.txt", outputDir, name);
			files.add(path);
			obsWriter = IOUtils.getBufferedWriter(path);
			
			path = String.format("%1$s/%2$s.estimated.txt", outputDir, name);
			files.add(path);
			estimWriter = IOUtils.getBufferedWriter(path);
			
			dumpStatisticsHeader();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		}
	}
	
	protected BufferedWriter openWriter(String filename) throws IOException {
		String path = String.format("%1$s/%2$s", outputDir, filename);
		BufferedWriter writer = IOUtils.getBufferedWriter(path);
		dumpStatisticsHeader(writer);
		files.add(path);
		return writer;
	}
	
	protected List<String> getStatisticsKeys() {
		if(statsKeys == null) {
			statsKeys = new LinkedList<String>();
			statsKeys.add(MEAN_KEY);
			statsKeys.add(MIN_KEY);
			statsKeys.add(MAX_KEY);
			statsKeys.add(VARIANCE_KEY);
			statsKeys.add(SKEWNESS_KEY);
			statsKeys.add(KURTOSIS_KEY);
		}
		return statsKeys;
	}
	
	protected void dumpStatisticsHeader() {
		dumpStatisticsHeader(obsWriter);
		dumpStatisticsHeader(estimWriter);
	}
	
	private void dumpStatisticsHeader(BufferedWriter writer) {
		List<String> keys = getStatisticsKeys();
		try {
			writer.write("iter");
			for(String key : keys) {
				writer.write(TAB);
				writer.write(key);
			}
			writer.newLine();
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void dumpObservedStatistics(TObjectDoubleHashMap<String> statsMap, int iteration) {
		dumpStatistics(statsMap, iteration, obsWriter);
	}
	
	protected void dumpEstimatedStatistics(TObjectDoubleHashMap<String> statsMap, int iteration) {
		dumpStatistics(statsMap, iteration, estimWriter);
	}
	
	protected void dumpStatistics(TObjectDoubleHashMap<String> statsMap, int iteration, BufferedWriter statsWriter) {
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
		statsMap.put(MEAN_KEY, stats.getMean());
		statsMap.put(MIN_KEY, stats.getMin());
		statsMap.put(MAX_KEY, stats.getMax());
		statsMap.put(VARIANCE_KEY, stats.getVariance());
		statsMap.put(SKEWNESS_KEY, stats.getSkewness());
		statsMap.put(KURTOSIS_KEY, stats.getKurtosis());
		return statsMap;
	}
	
	protected TObjectDoubleHashMap<String> getStatisticsMap(Distribution stats) {
		TObjectDoubleHashMap<String> statsMap = new TObjectDoubleHashMap<String>();
		statsMap.put(MEAN_KEY, stats.mean());
		statsMap.put(MIN_KEY, stats.min());
		statsMap.put(MAX_KEY, stats.max());
		statsMap.put(VARIANCE_KEY, stats.variance());
		statsMap.put(SKEWNESS_KEY, stats.skewness());
		statsMap.put(KURTOSIS_KEY, stats.kurtosis());
		return statsMap;
	}
	
	protected void dumpFrequency(Frequency freq, int iteration, String name) {
		try {
			BufferedWriter aWriter = IOUtils.getBufferedWriter(String.format("%1$s/%2$s.%3$s.histogram.absolute.txt", outputDir, iteration, name));
			BufferedWriter nWriter = IOUtils.getBufferedWriter(String.format("%1$s/%2$s.%3$s.histogram.normalized.txt", outputDir, iteration, name));
			
			aWriter.write("bin\tcount");
			aWriter.newLine();
			
			nWriter.write("bin\tpercentage");
			nWriter.newLine();
			
			for(Iterator<?> it = freq.valuesIterator(); it.hasNext();) {
				Object bin = it.next();
				
				aWriter.write(bin.toString());
				aWriter.write(TAB);
				aWriter.write(String.format(Locale.US, FLOAT_FORMAT, (float)freq.getCount(bin)));
				aWriter.newLine();
				
				nWriter.write(bin.toString());
				nWriter.write(TAB);
				nWriter.write(String.format(Locale.US, FLOAT_FORMAT, (float)freq.getPct(bin)));
				nWriter.newLine();
			}
			
			aWriter.close();
			nWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void dumpFrequency(Distribution freq, int iteration, String name) {
		try {
			BufferedWriter aWriter = IOUtils.getBufferedWriter(String.format("%1$s/%2$s.%3$s.histogram.absolute.txt", outputDir, iteration, name));
			BufferedWriter nWriter = IOUtils.getBufferedWriter(String.format("%1$s/%2$s.%3$s.histogram.normalized.txt", outputDir, iteration, name));
			
			aWriter.write("bin\tcount");
			aWriter.newLine();
			
			nWriter.write("bin\tpercentage");
			nWriter.newLine();
			
			TDoubleDoubleHashMap aDistr = freq.absoluteDistribution();
			TDoubleDoubleHashMap nDistr = freq.normalizedDistribution();
			double[] keys = aDistr.keys();
			Arrays.sort(keys);
			
			for(double key : keys) {
				aWriter.write(String.format(Locale.US, FLOAT_FORMAT, key));
				aWriter.write(TAB);
				aWriter.write(String.format(Locale.US, FLOAT_FORMAT, aDistr.get(key)));
				aWriter.newLine();
				
				nWriter.write(String.format(Locale.US, FLOAT_FORMAT, key));
				nWriter.write(TAB);
				nWriter.write(String.format(Locale.US, FLOAT_FORMAT, nDistr.get(key)));
				nWriter.newLine();
			}
			
			aWriter.close();
			nWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	protected void plotHistogram(double[] values, Histogram hist, int iteration) {
//		hist.addAll(values);
//		try {
//			hist.plot(String.format("%1$s/%2$s.histogram.png", outputDir, iteration), "Histogram");
//			hist.dumpRawData(String.format("%1$s/%2$s.histogram.txt", outputDir, iteration));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	protected void plotHistogram(double[] values, double[] weights, Histogram hist, int iteration) {
//		hist.addAll(values, weights);
//		try {
//			hist.plot(String.format("%1$s/%2$s.histogram.png", outputDir, iteration), "Histogram");
//			hist.dumpRawData(String.format("%1$s/%2$s.histogram.txt", outputDir, iteration));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
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

	@SuppressWarnings("unchecked")
	protected double getLocalResponseRate(VertexDecorator<SampledVertex> v, double globalResponseRate) {
		int numNonResponse = 0;
		int unknown = 0;
		for (Vertex vertex : v.getNeighbours()) {
			VertexDecorator<SampledVertex> n = (VertexDecorator<SampledVertex>) vertex;
			if (n.getDelegate().isRequested()) {
				if (n.getDelegate().isNonResponding())
					numNonResponse++;
			} else {
				unknown++;
			}
		}
		return 1 - ((numNonResponse + unknown * (1 - globalResponseRate)) / (double)v.getNeighbours().size());
	}
}
