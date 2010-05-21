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
package org.matsim.contrib.sna.graph.analysis;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;

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
	public void analyze(Graph graph, Map<String, Double> stats) {
		int numComponents = module.countComponents(graph);
		stats.put(NUM_COMPONENTS, new Double(numComponents));

		logger.info(String.format("%1$s disconnected components.", numComponents));
	}

}
