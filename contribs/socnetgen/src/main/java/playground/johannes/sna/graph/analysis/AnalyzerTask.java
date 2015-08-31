/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractGraphAnalyzerTast.java
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
package playground.johannes.sna.graph.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;

import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.math.Discretizer;
import playground.johannes.sna.math.Distribution;
import playground.johannes.sna.math.FixedSampleSizeDiscretizer;
import playground.johannes.sna.math.Histogram;
import playground.johannes.sna.util.TXTWriter;

/**
 * An AnalyzerTask performs specific analysis on a graph and returns the results
 * in a standardized output format.
 * 
 * @author illenberger
 * 
 */
public abstract class AnalyzerTask {

	private static final Logger logger = Logger.getLogger(AnalyzerTask.class);
	
	private String output;

	/**
	 * Sets the output directory that can be used to write additional output
	 * data.
	 * 
	 * @param output
	 *            a path to an existing directory.
	 */
	public void setOutputDirectoy(String output) {
		this.output = output;
	}

	/**
	 * Returns the output directory that can be used to write additional output
	 * data.
	 * 
	 * @return the output directory that can be used to write additional output
	 *         data.
	 */
	protected String getOutputDirectory() {
		return output;
	}

//	/**
//	 * Performs specific analysis on a graph and returns the results stored in a
//	 * map, where the key denotes the indicator (e.g. "k_mean") and the value
//	 * the numerical value of the indicator.
//	 * 
//	 * @param graph
//	 *            a graph
//	 * @param stats
//	 *            a map where the results of the analysis are stored.
//	 */
//	public void analyze(Graph graph, Map<String, Double> stats) {
//	
//	}
	
	public abstract void analyze(Graph graph, Map<String, DescriptiveStatistics> results);

	protected void printStats(DescriptiveStatistics stats, String key) {
		logger.info(String.format("Statistics for property %1$s:\n\tmean = %2$.4f, min = %3$.4f, max = %4$.4f, N = %5$s, Var = %6$.4f", 
				key, stats.getMean(), stats.getMin(), stats.getMax(), stats.getN(), stats.getVariance()));
	}
	
	/**
	 * Creates a histogram out of a distribution and writes it into a text file.
	 * 
	 * @param distr
	 *            a distribution
	 * @param binsize
	 *            the size of the bins in the histogram
	 * @param log
	 *            indicates whether the histogram should also be generated with
	 *            logarithmically scaled bins.
	 * @param name
	 *            the file name (will be written into the output directory)
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected void writeHistograms(Distribution distr, double binsize, boolean log, String name)
			throws FileNotFoundException, IOException {
		Distribution.writeHistogram(distr.absoluteDistribution(binsize), String.format("%1$s/%2$s.txt", output, name));
		Distribution.writeHistogram(distr.normalizedDistribution(binsize), String.format("%1$s/%2$s.share.txt", output,
				name));

		if (log) {
			Distribution.writeHistogram(distr.absoluteDistributionLog10(binsize), String.format("%1$s/%2$s.log10.txt",
					output, name));
			Distribution.writeHistogram(distr.absoluteDistributionLog2(binsize), String.format("%1$s/%2$s.log2.txt",
					output, name));

			Distribution.writeHistogram(distr.normalizedDistribution(distr.absoluteDistributionLog10(binsize)), String
					.format("%1$s/%2$s.share.log10.txt", output, name));
			Distribution.writeHistogram(distr.normalizedDistribution(distr.absoluteDistributionLog2(binsize)), String
					.format("%1$s/%2$s.share.log2.txt", output, name));
			
			if (distr.getValues().length > 0) {
				Distribution.writeHistogram(distr.normalizedDistribution(distr.absoluteDistributionFixed(100)),
						String.format("%1$s/%2$s.share.fixed.txt", output, name));
			} else {
				logger.warn("Cannot create historgram. No sampled.");
			}
		
		}
	}
	
	protected TDoubleDoubleHashMap writeHistograms(DescriptiveStatistics stats, String name, int bins, int minsize) throws IOException {
		double[] values = stats.getValues();
		if (values.length > 0) {
			TDoubleDoubleHashMap hist = Histogram.createHistogram(stats,
					FixedSampleSizeDiscretizer.create(values, minsize, bins), true);
			TXTWriter.writeMap(hist, name, "p",
					String.format("%1$s/%2$s.n%3$s.nonorm.txt", getOutputDirectory(), name, values.length / bins));
			Histogram.normalize(hist);
			TXTWriter.writeMap(hist, name, "p",
					String.format("%1$s/%2$s.n%3$s.txt", getOutputDirectory(), name, values.length / bins));
			
			return hist;
		} else {
			logger.warn("Cannot create histogram. No samples.");
			return null;
		}
	}
	
	protected TDoubleDoubleHashMap writeCumulativeHistograms(DescriptiveStatistics stats, String name, int bins, int minsize) throws IOException {
		double[] values = stats.getValues();
		if (values.length > 0) {
			TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, FixedSampleSizeDiscretizer.create(values, minsize, bins), false, false);
			hist = Histogram.createCumulativeHistogram(hist);
			Histogram.normalizeCumulative(hist);
			Histogram.complementary(hist);
			TXTWriter.writeMap(hist, name, "P",	String.format("%1$s/%2$s.n%3$s.cum.txt", getOutputDirectory(), name, values.length / bins));
			
			return hist;
		} else {
			logger.warn("Cannot create histogram. No samples.");
			return null;
		}
	}
	
	protected TDoubleDoubleHashMap writeHistograms(DescriptiveStatistics stats, Discretizer discretizer, String name, boolean reweight) throws IOException {
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, discretizer, reweight);
		TXTWriter.writeMap(hist, name, "n", String.format("%1$s/%2$s.txt", output, name)); 
		Histogram.normalize(hist);
		TXTWriter.writeMap(hist, name, "p", String.format("%1$s/%2$s.share.txt", output, name));
		
		return hist;
	}
	
	protected void writeRawData(DescriptiveStatistics stats, String name) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/%2$s.raw.txt", getOutputDirectory(), name)));
			for(double val : stats.getSortedValues()) {
				writer.write(String.valueOf(val));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected boolean outputDirectoryNotNull() {
		if(getOutputDirectory() != null)
			return true;
		else {
			logger.warn("No output directory specified.");
			return false;
		}
	}
	
	protected DescriptiveStatistics singleValueStats(String key, double value, Map<String, DescriptiveStatistics> stats) {
		DescriptiveStatistics ds = new DescriptiveStatistics();
		ds.addValue(value);
		stats.put(key, ds);
		return ds;
	}
}
