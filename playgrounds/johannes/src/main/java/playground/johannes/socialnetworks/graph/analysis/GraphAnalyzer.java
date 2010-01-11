/* *********************************************************************** *
 * project: org.matsim.*
 * GraphAnalyzer.java
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
package playground.johannes.socialnetworks.graph.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.math.Distribution;

/**
 * @author illenberger
 *
 */
public class GraphAnalyzer {
	
	public static final Logger logger = Logger.getLogger(GraphAnalyzer.class);
	
	public static final String MEAN_DEGREE = "k_mean";
	
	public static final String MIN_DEGREE = "k_min";
	
	public static final String MAX_DEGREE = "k_max";
	
	public static final String DEGREE_CORRELATION = "r_k";
	
	public static final String MEAN_LOCAL_CLUSTERING = "c_local_mean";
	
	public static final String MIN_LOCAL_CLUSTERING = "c_local_min";
	
	public static final String MAX_LOCAL_CLUSTERING = "c_local_max";
	
	public static final String GLOBAL_CLUSTERING_COEFFICIENT = "c_global";

	public Map<String, Double> analyze(Graph graph, GraphPropertyFactory factory) {
		Map<String, Double> stats = new HashMap<String, Double>();
		/*
		 * Degree
		 */
		Degree degree = factory.getDegree();
		Distribution distr = degree.distribution(graph.getVertices()); 
		double k_mean = distr.mean();
		double k_min = distr.min();
		double k_max = distr.max();
		stats.put(MEAN_DEGREE, k_mean);
		stats.put(MAX_DEGREE, k_max);
		stats.put(MIN_DEGREE, k_min);
		logger.info(String.format("k_mean = %1$.4f, k_max = %2$s, k_min = %3$s.", k_mean, k_max, k_min));
		
		double r_k = degree.assortativity(graph);
		stats.put(DEGREE_CORRELATION, r_k);
		logger.info(String.format("r_k = %1$.4f", r_k));
		/*
		 * Transitivity
		 */
		Transitivity transitivity = factory.getTransitivity();
		distr = transitivity.localClusteringDistribution(graph.getVertices());
		double c_mean = distr.mean();
		double c_max = distr.max();
		double c_min = distr.min();
		stats.put(MEAN_LOCAL_CLUSTERING, c_mean);
		stats.put(MAX_LOCAL_CLUSTERING, c_max);
		stats.put(MIN_LOCAL_CLUSTERING, c_min);
		
		double c_global = transitivity.globalClusteringCoefficient(graph);
		stats.put(GLOBAL_CLUSTERING_COEFFICIENT, c_global);
		
		logger.info(String.format("c_local_mean = %1$.4f, c_local_max = %2$.4f, c_local_min = %3$.4f, c_global = %4$.4f.", c_mean, c_max, c_min, c_global));
		
		return stats;
	}
	
	public void writeStats(Map<String, Double> stats, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		writer.write("propert\tvalue");
		writer.newLine();
		for(Entry<String, Double> entry : stats.entrySet()) {
			writer.write(entry.getKey());
			writer.write("\t");
			writer.write(entry.getValue().toString());
			writer.newLine();
		}
		writer.close();
	}
}
