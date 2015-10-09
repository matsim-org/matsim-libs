/* *********************************************************************** *
 * project: org.matsim.*
 * ComponentsTask.java
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
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.stats.TXTWriter;
import playground.johannes.sna.graph.Graph;

import java.io.IOException;
import java.util.Map;

/**
 * An AnalyzerTask that counts the number of components in a graph.
 * 
 * @author illenberger
 * 
 */
public class ComponentsTask extends ModuleAnalyzerTask<Components> {

	private static final Logger logger = Logger.getLogger(ComponentsTask.class);

	private static final String NUM_COMPONENTS = "n_components";

	/**
	 * Creates a new AnalyzerTask with {@link Components} object as the analyzer
	 * module.
	 */
	public ComponentsTask() {
		setModule(new Components());
	}

	/**
	 * Counts the number of components in <tt>graph</tt>
	 * 
	 * @param graph
	 *            a graph.
	 * @param stats
	 *            a map where the results of the analysis are stored.
	 */
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		DescriptiveStatistics distr = module.distribution(graph);
		double numComponents = distr.getN();
		singleValueStats(NUM_COMPONENTS, numComponents, statsMap);

		logger.info(String.format("%1$s disconnected components.", numComponents));
		
		if(outputDirectoryNotNull()) {
			TDoubleDoubleHashMap hist = Histogram.createHistogram(distr, new LinearDiscretizer(1.0), false);
			try {
				TXTWriter.writeMap(hist, "size", "n", String.format("%1$s/components.txt", getOutputDirectory()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

}
