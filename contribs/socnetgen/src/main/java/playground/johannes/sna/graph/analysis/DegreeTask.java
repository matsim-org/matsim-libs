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
package playground.johannes.sna.graph.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.sna.graph.Graph;

import java.io.IOException;
import java.util.Map;

/**
 * An AnalyzerTaks that calculated degree related measurements.
 * 
 * @author illenberger
 * 
 */
public class DegreeTask extends ModuleAnalyzerTask<Degree> {

	public static final String KEY = "k";

	public static final String MEAN_DEGREE = "k_mean";

	public static final String MIN_DEGREE = "k_min";

	public static final String MAX_DEGREE = "k_max";

	public static final String DEGREE_CORRELATION = "r_k";

	/**
	 * Creates a new DegreeTask with an instance of {@link Degree} used for
	 * analysis.
	 */
	public DegreeTask() {
		setKey(KEY);
		setModule(Degree.getInstance());
	}

	/**
	 * Determines the degree distribution and the degree correlation of a graph.
	 * Writes the histogram of the degree distribution into the output directory
	 * (if specified).
	 * 
	 * @param graph
	 *            a graph.
	 * @param stats
	 *            a map where the results of the analysis are stored.
	 */
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		DescriptiveStatistics stats = module.statistics(graph.getVertices());
		printStats(stats, key);
		statsMap.put(key, stats);
		if (outputDirectoryNotNull()) {
			try {
				writeHistograms(stats, new LinearDiscretizer(1.0), key, false);
				writeHistograms(stats, new LinearDiscretizer(5.0), key + "_5", false);
				writeHistograms(stats, key, 13, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		stats = new DescriptiveStatistics();
		stats.addValue(module.assortativity(graph));
		statsMap.put("r_" + key, stats);
		printStats(stats, "r_" + key);
	}

}
