/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeLengthSumTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;

/**
 * @author illenberger
 *
 */
public class EdgeLengthSumTask extends ModuleAnalyzerTask<AbstractSpatialProperty> {

	public EdgeLengthSumTask() {
		setModule(new EdgeLengthSum());
		setKey("d_sum");
	}
	
	public EdgeLengthSumTask(EdgeLengthSum module) {
		setModule(module);
		setKey("d_sum");
	}
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
	}

	@Override
	public void analyzeStats(Graph graph, Map<String, DescriptiveStatistics> statsMap) {
		DescriptiveStatistics stats = module.statistics(graph.getVertices());
		statsMap.put(key, stats);
		printStats(stats, key);
		
		if(outputDirectoryNotNull()) {
			try {
				writeHistograms(stats, key, 100, 20);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
