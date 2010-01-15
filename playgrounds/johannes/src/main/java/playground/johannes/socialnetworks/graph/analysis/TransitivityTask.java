/* *********************************************************************** *
 * project: org.matsim.*
 * TransitivityTask.java
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.math.Distribution;

/**
 * @author illenberger
 *
 */
public class TransitivityTask extends AbstractGraphAnalyzerTask {
	
	public static final Logger logger = Logger.getLogger(TransitivityTask.class);

	public static final String MEAN_LOCAL_CLUSTERING = "c_local_mean";
	
	public static final String MIN_LOCAL_CLUSTERING = "c_local_min";
	
	public static final String MAX_LOCAL_CLUSTERING = "c_local_max";
	
	public static final String GLOBAL_CLUSTERING_COEFFICIENT = "c_global";
	
	public TransitivityTask(String output) {
		super(output);
	}
	
	@Override
	public void analyze(Graph graph, Map<String, Object> analyzers, Map<String, Double> stats) {
		Transitivity transitivity;
		Object obj = analyzers.get(this.getClass().getCanonicalName());
		if(obj == null)
			transitivity = new Transitivity();
		else {
			transitivity = (Transitivity) obj;
			logger.info("Using analyzer class " + transitivity.getClass().getCanonicalName());
		}
		
		Distribution distr = transitivity.localClusteringDistribution(graph.getVertices());
		double c_mean = distr.mean();
		double c_max = distr.max();
		double c_min = distr.min();
		stats.put(MEAN_LOCAL_CLUSTERING, c_mean);
		stats.put(MAX_LOCAL_CLUSTERING, c_max);
		stats.put(MIN_LOCAL_CLUSTERING, c_min);
		
		double c_global = transitivity.globalClusteringCoefficient(graph);
		stats.put(GLOBAL_CLUSTERING_COEFFICIENT, c_global);
		
		logger.info(String.format("c_local_mean = %1$.4f, c_local_max = %2$.4f, c_local_min = %3$.4f, c_global = %4$.4f.", c_mean, c_max, c_min, c_global));
		
		if(getOutputDirectory() != null) {
			try {
				writeHistograms(distr, 0.05, false, "c");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
