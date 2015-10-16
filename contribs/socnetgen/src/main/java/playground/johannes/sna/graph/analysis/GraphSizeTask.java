/* *********************************************************************** *
 * project: org.matsim.*
 * GraphSizeTask.java
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

import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.sna.graph.Graph;

/**
 * An AnalyzerTask that counts the number of vertices and edges in a graph.
 * 
 * @author illenberger
 * 
 */
public class GraphSizeTask extends AnalyzerTask {

//	private static final Logger logger = Logger.getLogger(GraphSizeTask.class);

	public static final String NUM_VERTICES = "n_vertex";

	public static final String NUM_EDGES = "n_edge";

	/**
	 * Counts the number of vertices and edges in a graph.
	 * 
	 * @param graph
	 *            a graph.
	 * @param stats
	 *            a map where the results of the analysis are stored.
	 */
//	@Override
//	public void analyze(Graph graph, Map<String, Double> stats) {
////		int n_vertex = graph.getVertices().size();
////		int n_edge = graph.getEdges().size();
////		stats.put(NUM_VERTICES, new Double(n_vertex));
////		stats.put(NUM_EDGES, new Double(n_edge));
////		logger.info(String.format("%1$s = %2$s, %3$s = %4$s", NUM_VERTICES, n_vertex, NUM_EDGES, n_edge));
//	}

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		stats.addValue(graph.getVertices().size());
		statsMap.put(NUM_VERTICES, stats);
		printStats(stats, NUM_VERTICES);
		
		stats = new DescriptiveStatistics();
		stats.addValue(graph.getEdges().size());
		statsMap.put(NUM_EDGES, stats);
		printStats(stats, NUM_EDGES);
	}

}
