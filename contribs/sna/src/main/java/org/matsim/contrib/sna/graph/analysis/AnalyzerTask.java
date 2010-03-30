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
package org.matsim.contrib.sna.graph.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.math.Distribution;

/**
 * An AnalyzerTask performs specific analysis on a graph and returns the results
 * in a standardized output format.
 * 
 * @author illenberger
 * 
 */
public abstract class AnalyzerTask {

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

	/**
	 * Performs specific analysis on a graph and returns the results stored in a
	 * map, where the key denotes the indicator (e.g. "k_mean") and the value
	 * the numerical value of the indicator.
	 * 
	 * @param graph
	 *            a graph
	 * @param stats
	 *            a map where the results of the analysis are stored.
	 */
	abstract public void analyze(Graph graph, Map<String, Double> stats);

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
		}
	}
}
