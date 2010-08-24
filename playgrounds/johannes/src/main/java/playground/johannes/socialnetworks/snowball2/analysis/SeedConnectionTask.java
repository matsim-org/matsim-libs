/* *********************************************************************** *
 * project: org.matsim.*
 * SeedConnectionTask.java
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
package playground.johannes.socialnetworks.snowball2.analysis;

import gnu.trove.TIntArrayList;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.graph.matrix.Dijkstra;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class SeedConnectionTask extends AnalyzerTask {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		Set<SampledVertex> seedSet = (Set<SampledVertex>) SnowballPartitions.createSampledPartition((Set<? extends SampledVertex>)graph.getVertices(), 0);
		List<SampledVertex> seeds = new ArrayList<SampledVertex>(seedSet);
		
		int[][] connects = new int[seeds.size()][seeds.size()];
//		Arrays.fill(connects, -1);
		
		AdjacencyMatrix<SampledVertex> y = new AdjacencyMatrix<SampledVertex>(graph);
		Dijkstra dijkstra = new Dijkstra(y);
		
		for(int i = 0; i < seeds.size(); i++) {
			int i_idx = y.getIndex(seeds.get(i));
			for(int j = i+1; j < seeds.size(); j++) {
				int j_idx = y.getIndex(seeds.get(j));
				dijkstra.run(i_idx, j_idx);
				TIntArrayList path = dijkstra.getPath(i_idx, j_idx);
				if(path != null) {
					connects[i][j] = path.size();
					connects[j][i] = path.size();
				} else {
					connects[i][j] = -1;
					connects[j][i] = -1;
				}
			}
		}

		if(getOutputDirectory() != null) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/seedconnects.txt", getOutputDirectory())));
				
				for(int i = 0; i < connects.length; i++) {
					for(int j = 0; j < connects.length; j++) {
						if(connects[i][j] < 0)
							writer.write("-");
						else
							writer.write(String.valueOf(connects[i][j]));
						
						writer.write("\t");
					}
					writer.newLine();
				}
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
