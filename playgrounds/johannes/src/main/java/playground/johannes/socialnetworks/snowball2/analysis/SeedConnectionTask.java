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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.sna.graph.matrix.Dijkstra;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class SeedConnectionTask extends AnalyzerTask {

	public static final String NUM_CONNECTS = "n_connects";

	public Set<List<SampledVertexDecorator<SocialVertex>>> pathSet = new HashSet<List<SampledVertexDecorator<SocialVertex>>>();
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		Set<SampledVertex> seedSet = (Set<SampledVertex>) SnowballPartitions.createSampledPartition((Set<? extends SampledVertex>)graph.getVertices(), 0);
		List<SampledVertex> seeds = new ArrayList<SampledVertex>(seedSet);
		
		int[][] connects = new int[seeds.size()][seeds.size()];
		Set<TIntArrayList> paths = new HashSet<TIntArrayList>();
		AdjacencyMatrix<SampledVertex> y = new AdjacencyMatrix<SampledVertex>(graph);
		Dijkstra dijkstra = new Dijkstra(y);
		
		int[] ids = new int[seeds.size()];
		
		for(int i = 0; i < seeds.size(); i++) {
			int i_idx = y.getIndex(seeds.get(i));
			ids[i] = i_idx;
			for(int j = i+1; j < seeds.size(); j++) {
				int j_idx = y.getIndex(seeds.get(j));
				dijkstra.run(i_idx, j_idx);
				TIntArrayList path = dijkstra.getPath(i_idx, j_idx);
				if(path != null) {
					connects[i][j] = path.size();
					connects[j][i] = path.size();
		
					
					path.insert(0, i_idx);
					paths.add(path);
				} else {
					connects[i][j] = -1;
					connects[j][i] = -1;
				}
			}
		}
		
		int nConnects = 0;
		for(int i = 0; i < connects.length; i++) {
			for(int j = i+1; j < connects.length; j++) {
				if(connects[i][j] > 0)
					nConnects++;
			}
		}
		try {
			dumpPaths(paths, y);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		stats.put(NUM_CONNECTS, new Double(nConnects));
		
		if(getOutputDirectory() != null) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/seedconnects.txt", getOutputDirectory())));
				
				for(int i = 0; i < connects.length; i++) {
					writer.write("\t");
					SampledVertex v = y.getVertex(ids[i]);
					SampledVertexDecorator<SocialVertex> vertex = (SampledVertexDecorator<SocialVertex>) v;
					writer.write(String.valueOf(vertex.getDelegate().getPerson().getId().toString()));
				}
				writer.newLine();
				for(int i = 0; i < connects.length; i++) {
					SampledVertex v = y.getVertex(ids[i]);
					SampledVertexDecorator<SocialVertex> vertex = (SampledVertexDecorator<SocialVertex>) v;
					writer.write(String.valueOf(vertex.getDelegate().getPerson().getId().toString()));
					for(int j = 0; j < connects.length; j++) {
						writer.write("\t");
						if(connects[i][j] < 0)
							writer.write("-");
						else
							writer.write(String.valueOf(connects[i][j]));
						
						
					}
					writer.newLine();
				}
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void dumpPaths(Set<TIntArrayList> paths, AdjacencyMatrix<SampledVertex> y) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/paths.txt", getOutputDirectory())));
		
		for(TIntArrayList path : paths) {
			List<SampledVertexDecorator<SocialVertex>> list = new ArrayList<SampledVertexDecorator<SocialVertex>>();
			for(int i = 0; i < path.size(); i++) {
				SampledVertex v = y.getVertex(path.get(i));
				SampledVertexDecorator<SocialVertex> vertex = (SampledVertexDecorator<SocialVertex>) v;
				writer.write(String.valueOf(vertex.getDelegate().getPerson().getId().toString()));
				writer.write("(");
				writer.write(String.valueOf(vertex.getIterationSampled()));
				writer.write(")\t");
				
				list.add(vertex);
			}
			writer.newLine();
			pathSet.add(list);
		}
		writer.close();
	}

}
