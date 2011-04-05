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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.graph.matrix.Dijkstra;
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
				Integer it = ((SampledVertex)vertex).getIterationSampled();
				if(it != null && it == 0) {
					seeds.add(vertex);
				}
			}
		}
		
		AdjacencyMatrix<Vertex> y = new AdjacencyMatrix<Vertex>(graph);
		Dijkstra dijkstra = new Dijkstra(y);
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		for(int i = 0; i < seeds.size(); i++) {
			int idx_i = y.getIndex(seeds.get(i));
			dijkstra.run(idx_i, -1);
			for(int j = i + 1; j < seeds.size(); j++) {
				int idx_j = y.getIndex(seeds.get(j));
				TIntArrayList path = dijkstra.getPath(idx_i, idx_j);
				if(path != null) {
					stats.addValue(path.size());
				}
			}
		}
		
		results.put(KEY, stats);
		printStats(stats, KEY);
	}

}
