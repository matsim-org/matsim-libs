/* *********************************************************************** *
 * project: org.matsim.*
 * StandardAnalyzerTask.java
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

import java.io.IOException;
import java.util.Map;

import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.graph.spatial.generators.GravityEdgeCostFunction;

/**
 * @author illenberger
 *
 */
public class StandardAnalyzerTask extends playground.johannes.socialnetworks.graph.analysis.StandardAnalyzerTask {

	public StandardAnalyzerTask() {
		super();
		addTask(new DistanceTask());
//		addTask(new AcceptanceProbabilityTask());
		addTask(new EdgeCostsTask(new GravityEdgeCostFunction(1.6, 1, new CartesianDistanceCalculator())));
		
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph(args[0]);
		
		String output = null;
		if(args.length > 1) {
			output = args[1];
		}
		
		AnalyzerTask task = new StandardAnalyzerTask();
		if(output != null)
			task.setOutputDirectoy(output);
		
		Map<String, Double> stats = GraphAnalyzer.analyze(graph, task);
		
		if(output != null)
			GraphAnalyzer.writeStats(stats, output + "/stats.txt");
	
	}

}
