/* *********************************************************************** *
 * project: org.matsim.*
 * SeedAPLTask.java
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

import gnu.trove.TIntArrayList;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.analysis.AnalyzerTask;
import playground.johannes.sna.graph.matrix.AdjacencyMatrix;
import playground.johannes.sna.graph.matrix.Dijkstra;
import playground.johannes.sna.snowball.SampledGraph;
import playground.johannes.sna.snowball.SampledVertex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author illenberger
 *
 */
public class SeedAPLTask extends AnalyzerTask {

	public static String KEY = "seed_apl";
	
	private List<SampledVertex> seeds;
	
	@Override
	public void analyze(Graph g, Map<String, DescriptiveStatistics> results) {
		SampledGraph graph = (SampledGraph) g;
		if(seeds == null) {
			seeds = new ArrayList<SampledVertex>(graph.getVertices().size());
			for(SampledVertex vertex : graph.getVertices()) {
				Integer it = vertex.getIterationDetected();
				if(it != null && it == -1) {
					seeds.add(vertex);
				}
			}
		}
		
		AdjacencyMatrix<SampledVertex> y = new AdjacencyMatrix<SampledVertex>(graph);
		
		int[] seedIndices = new int[seeds.size()];
		for(int i = 0; i < seeds.size(); i++) {
			seedIndices[i] = y.getIndex(seeds.get(i));
		}
		
		Dijkstra dijkstra = new Dijkstra(y);
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		DescriptiveStatistics statsDirect = new DescriptiveStatistics();
		
		for(int i = 0; i < seedIndices.length; i++) {
			int idx_i = seedIndices[i];
			dijkstra.run(idx_i, -1);
			for(int j = i + 1; j < seedIndices.length; j++) {
				int idx_j = seedIndices[j];
				TIntArrayList path = dijkstra.getPath(idx_i, idx_j);
				if(path != null) {
					stats.addValue(path.size());
					/*
					 * filter indirect paths
					 */
					boolean indirect = false;
					for(int k = 1; k < path.size() - 1; k++) {
						SampledVertex v = y.getVertex(path.get(k));
						if(v.getSeed() != seeds.get(i) && v.getSeed() != seeds.get(j)) {
							indirect = true;
							break;
						}
					}
					
					if(!indirect)
						statsDirect.addValue(path.size());
				}
			}
		}
		
		results.put(KEY, stats);
		printStats(stats, KEY);
		
		String key2 = KEY + "_direct";
		results.put(key2, statsDirect);
		printStats(statsDirect, key2);
		
		try {
			writeHistograms(stats, new LinearDiscretizer(1.0), KEY, false);
			writeHistograms(statsDirect, new LinearDiscretizer(1.0), key2, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
