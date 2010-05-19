/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeCostsTask.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.spatial.generators.EdgeCostFunction;

/**
 * @author illenberger
 *
 */
public class EdgeCostsTask extends ModuleAnalyzerTask<EdgeCosts> {

	private static final Logger logger = Logger.getLogger(EdgeCostsTask.class);
	
	public static final String MEAN_EDGE_COSTS = "cost_mean";
	
	public static final String MIN_EDGE_COSTS = "cost_min";
	
	public static final String MAX_EDGE_COSTS = "cost_max";
	
	public static final String MEAN_EDGE_COSTS_SUM = "cost_i_mean";
	
	public static final String MIN_EDGE_COSTS_SUM = "cost_i_min";
	
	public static final String MAX_EDGE_COSTS_SUM = "cost_i_max";
	
	public EdgeCostsTask(EdgeCostFunction costFunction) {
		setModule(new EdgeCosts(costFunction));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		Distribution distr = module.distribution((Set<? extends SpatialVertex>)graph.getVertices());
		
		double c_mean = distr.mean();
		double c_min = distr.min();
		double c_max = distr.max();
		stats.put(MEAN_EDGE_COSTS, c_mean);
		stats.put(MIN_EDGE_COSTS, c_min);
		stats.put(MAX_EDGE_COSTS, c_max);
		
		logger.info(String.format("cost_mean=%1$.4f, cost_min=%2$.4f, cost_max=%3$.4f", c_mean, c_min, c_max));
		
		if(getOutputDirectory() != null) {
			try {
				writeHistograms(distr, (c_max-c_min)/100.0, false, "costs");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		distr = module.vertexCostsSum((Set<? extends SpatialVertex>)graph.getVertices());
		
		double sum_mean = distr.mean();
		double sum_min = distr.min();
		double sum_max = distr.max();
		stats.put(MEAN_EDGE_COSTS_SUM, sum_mean);
		stats.put(MIN_EDGE_COSTS_SUM, sum_min);
		stats.put(MAX_EDGE_COSTS_SUM, sum_max);
		
		logger.info(String.format("cost_i_mean=%1$.4f, cost_i_min=%2$.4f, cost_i_max=%3$.4f", sum_mean, sum_min, sum_max));
		
		if(getOutputDirectory() != null) {
			try {
				writeHistograms(distr, (sum_max-sum_min)/20.0, false, "costs_i");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
