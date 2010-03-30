/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeTask.java
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

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.math.Distribution;

/**
 * An AnalyzerTaks that calculated degree related measurements.
 * 
 * @author illenberger
 * 
 */
public class DegreeTask extends ModuleAnalyzerTask<Degree> {

	private static final Logger logger = Logger.getLogger(DegreeTask.class);

	public static final String MEAN_DEGREE = "k_mean";

	public static final String MIN_DEGREE = "k_min";

	public static final String MAX_DEGREE = "k_max";

	public static final String DEGREE_CORRELATION = "r_k";

	/**
	 * Creates a new DegreeTask with an instance of {@link Degree} used for
	 * analysis.
	 */
	public DegreeTask() {
		setModule(new Degree());
	}

	/**
	 * Calculated the mean degree, the maximum degree, the minimum degree and
	 * the degree correlation of a graph. Writes the histogram of the degree
	 * distribution into the output directory (if specified).
	 * 
	 * @param graph
	 *            a graph.
	 * @param stats
	 *            a map where the results of the analysis are stored.
	 */
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		Distribution distr = module.distribution(graph.getVertices());
		double k_mean = distr.mean();
		double k_min = distr.min();
		double k_max = distr.max();
		stats.put(MEAN_DEGREE, k_mean);
		stats.put(MAX_DEGREE, k_max);
		stats.put(MIN_DEGREE, k_min);
		logger.info(String.format("k_mean = %1$.4f, k_max = %2$s, k_min = %3$s.", k_mean, k_max, k_min));

		double r_k = module.assortativity(graph);
		stats.put(DEGREE_CORRELATION, r_k);
		logger.info(String.format("r_k = %1$.4f", r_k));

		if (getOutputDirectory() != null) {
			try {
				writeHistograms(distr, 1.0, false, "k");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
