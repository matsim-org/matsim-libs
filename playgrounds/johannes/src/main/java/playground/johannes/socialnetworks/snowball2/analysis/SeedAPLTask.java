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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.graph.matrix.Dijkstra;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class SeedAPLTask extends AnalyzerTask {

	public static String KEY = "seed_apl";
	
	private List<Vertex> seeds;
	
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> results) {
		if(seeds == null) {
			seeds = new ArrayList<Vertex>(graph.getVertices().size());
			for(Vertex vertex : graph.getVertices()) {
				Integer it = ((SampledVertex)vertex).getIterationDetected();
				if(it != null && it == -1) {
					seeds.add(vertex);
				}
			}
		}
		
		AdjacencyMatrix<Vertex> y = new AdjacencyMatrix<Vertex>(graph);
		
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
					for(int k = 0; k < path.size() - 1; k++) {
						for(int l = 0; l < seedIndices.length; l++) {
							if(path.get(k) == seedIndices[l]) {
								indirect = true;
								break;
							}
						}
						if(indirect)
							break;
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
