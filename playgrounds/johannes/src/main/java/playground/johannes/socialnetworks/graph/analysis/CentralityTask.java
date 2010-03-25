/* *********************************************************************** *
 * project: org.matsim.*
 * CentralityTask.java
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
public class CentralityTask extends ModuleAnalyzerTask<Centrality> {

	private static final Logger logger = Logger.getLogger(CentralityTask.class);
	
	public static final String MEAN_CLOSENESS = "close_mean";
	
	public static final String MIN_CLOSENESS = "close_min";
	
	public static final String MAX_CLOSENESS = "close_max";
	
	public static final String MEAN_BETWEENNESS = "between_mean";
	
	public static final String MIN_BETWEENNESS = "between_min";
	
	public static final String MAX_BETWEENNESS = "between_max";
	
	public static final String DIAMETER = "diameter";
	
	public static final String RADIUS = "radius";
	
	public CentralityTask() {
		setModule(new Centrality());
	}
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		module.init(graph);
		
		Distribution cDistr = module.closenessDistribution();
		double c_mean = cDistr.mean();
		double c_min = cDistr.min();
		double c_max = cDistr.max();
		stats.put(MEAN_CLOSENESS, c_mean);
		stats.put(MIN_CLOSENESS, c_min);
		stats.put(MAX_CLOSENESS, c_max);
		logger.info(String.format("close_mean = %1$.4f, close_min = %2$.4f, close_max = %3$.4f", c_mean, c_min, c_max));
		
		Distribution bDistr = module.vertexBetweennessDistribution();
		double b_mean = bDistr.mean();
		double b_min = bDistr.min();
		double b_max = bDistr.max();
		stats.put(MEAN_BETWEENNESS, b_mean);
		stats.put(MIN_BETWEENNESS, b_min);
		stats.put(MAX_BETWEENNESS, b_max);
		logger.info(String.format("between_mean = %1$s, between_min = %2$s, between_max = %4$s", b_mean, b_min, b_max));
		
		stats.put(DIAMETER, new Double(module.diameter()));
		stats.put(RADIUS, new Double(module.radius()));
		logger.info(String.format("diameter = %1$s, radius = %2$s", module.diameter(), module.radius()));
		
		if(getOutputDirectory() != null) {
			try {
				writeHistograms(cDistr, 1.0, false, "close");
				writeHistograms(bDistr, 1.0, false, "between");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

}
