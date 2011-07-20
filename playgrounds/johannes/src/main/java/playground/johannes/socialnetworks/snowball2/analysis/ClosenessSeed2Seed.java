/* *********************************************************************** *
 * project: org.matsim.*
 * ClosenessSeed2Seed.java
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
package playground.johannes.socialnetworks.snowball2.analysis;

import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;

import playground.johannes.socialnetworks.graph.analysis.Centrality;

/**
 * @author illenberger
 *
 */
public class ClosenessSeed2Seed extends AnalyzerTask {

	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> results) {
		SampledGraph sampledGraph = (SampledGraph) graph;
		Set<? extends Vertex> seeds = SnowballPartitions.createSampledPartition(sampledGraph.getVertices(), 0);// FIXME
		Set<? extends Vertex> egos = SnowballPartitions.createSampledPartition(sampledGraph.getVertices());
		/*
		 * seed 2 seed
		 */
		Centrality centrality = new Centrality();
		centrality.init(sampledGraph, seeds, seeds, false);
		
		DescriptiveStatistics closeness = centrality.closenessDistribution();		
		results.put("closeness_s2s", closeness);
		
		DescriptiveStatistics apl = centrality.getAPL();
		results.put("apl_s2s", apl);
		/*
		 * ego 2 ego
		 */
		centrality = new Centrality();
		centrality.init(sampledGraph, egos, egos, false);
		
		closeness = centrality.closenessDistribution();		
		results.put("closeness_e2e", closeness);
		
		apl = centrality.getAPL();
		results.put("apl_e2e", apl);
		/*
		 * all
		 */
		centrality = new Centrality();
		centrality.init(sampledGraph, false);
		
		closeness = centrality.closenessDistribution();		
		results.put("closeness", closeness);
		
		apl = centrality.getAPL();
		results.put("apl", apl);
	}

}
