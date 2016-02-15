/* *********************************************************************** *
 * project: org.matsim.*
 * BridgeVertexTask.java
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
package org.matsim.contrib.socnetgen.sna.snowball.analysis;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.socnetgen.sna.snowball.SampledEdge;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraph;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;

import java.util.Map;

/**
 * @author illenberger
 *
 */
public class BridgeEdgeTask extends AnalyzerTask {

	public static final String KEY = "n_bridge";
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> results) {
		SampledGraph graph = (SampledGraph) g;
		
		int count = 0;
		
		for(SampledEdge e : graph.getEdges()) {
			SampledVertex v1 = e.getVertices().getFirst();
			SampledVertex v2 = e.getVertices().getSecond();
			
			if(v1.getSeed() != v2.getSeed())
				count++;
		}
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		stats.addValue(count);
		
		printStats(stats, KEY);
		results.put(KEY, stats);
	}

}
