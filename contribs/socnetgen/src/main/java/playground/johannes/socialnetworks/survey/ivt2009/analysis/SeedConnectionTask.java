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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TIntArrayList;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.socnetgen.sna.graph.matrix.AdjacencyMatrix;
import org.matsim.contrib.socnetgen.sna.graph.matrix.Dijkstra;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertexDecorator;
import org.matsim.contrib.socnetgen.sna.snowball.analysis.SnowballPartitions;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.statistics.Correlations;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author illenberger
 *
 */
public class SeedConnectionTask extends AnalyzerTask {

	public static final String NUM_CONNECTS = "n_connects";

	public Set<List<SampledVertexDecorator<SocialVertex>>> pathSet = new HashSet<List<SampledVertexDecorator<SocialVertex>>>();
	@Override
	public void analyze(Graph graph, Map<String, DescriptiveStatistics> stats) {
		Set<SampledVertex> seedSet = (Set<SampledVertex>) SnowballPartitions.createSampledPartition((Set<? extends SampledVertex>)graph.getVertices(), 0);
		List<SampledVertex> seeds = new ArrayList<SampledVertex>(seedSet);
		
		AdjacencyMatrix<SampledVertex> y = new AdjacencyMatrix<SampledVertex>(graph);
		
		int[] seedIndices = new int[seeds.size()];
		for(int i = 0; i < seeds.size(); i++) {
			seedIndices[i] = y.getIndex(seeds.get(i));
		}
		
		int[][] connects = new int[seeds.size()][seeds.size()];
		int[][] directs = new int[seeds.size()][seeds.size()];
		Set<TIntArrayList> paths = new HashSet<TIntArrayList>();
		
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
					
					boolean indirect = false;
					for(int k = 1; k < path.size() - 1; k++) {
						SampledVertex v = y.getVertex(path.get(k));
						if(v.getSeed() != seeds.get(i) && v.getSeed() != seeds.get(j)) {
							indirect = true;
							break;
						}
					}
					if(!indirect) {
						directs[i][j] = 1;
						directs[j][i] = 1;
					}
				} else {
					connects[i][j] = -1;
					connects[j][i] = -1;
				}
			}
		}
		
		int nPaths = 0;
		int nDirectPaths = 0;
		int total = 0;
		int totalDirect = 0;
		for(int i = 0; i < connects.length; i++) {
			for(int j = i+1; j < connects.length; j++) {
				if(connects[i][j] > 0) {
					total += connects[i][j];
					nPaths++;
					
					if(directs[i][j] > 0) {
						totalDirect += connects[i][j];
						nDirectPaths++;
					}
				}
			}
		}
		
		try {
			dumpPaths(paths, y);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		DescriptiveStatistics ds = new DescriptiveStatistics();
		ds.addValue(new Double(nPaths));
		stats.put(NUM_CONNECTS, ds);
		
		ds = new DescriptiveStatistics();
		ds.addValue(nDirectPaths);
		stats.put("n_directPath", ds);
		
		ds = new DescriptiveStatistics();
		ds.addValue(total/(double)nPaths);
		stats.put("apl", ds);
		
		ds = new DescriptiveStatistics();
		ds.addValue(totalDirect/(double)nDirectPaths);
		stats.put("apl_d", ds);
		
		if(getOutputDirectory() != null) {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("%1$s/seedconnects.txt", getOutputDirectory())));
				
				for(int i = 0; i < connects.length; i++) {
					writer.write("\t&");
					SampledVertex v = y.getVertex(ids[i]);
					SampledVertexDecorator<SocialVertex> vertex = (SampledVertexDecorator<SocialVertex>) v;
					writer.write(String.valueOf(vertex.getDelegate().getPerson().getId().toString()));
				}
				writer.write("\\\\");
				writer.newLine();
				for(int i = 0; i < connects.length; i++) {
					SampledVertex v = y.getVertex(ids[i]);
					SampledVertexDecorator<SocialVertex> vertex = (SampledVertexDecorator<SocialVertex>) v;
					writer.write(String.valueOf(vertex.getDelegate().getPerson().getId().toString()));
					for(int j = 0; j < connects.length; j++) {
						writer.write("\t&");
						if(connects[i][j] < 0)
							writer.write("-");
						else {
							if(directs[i][j] > 0) {
								writer.write("\\bf{");
								writer.write(String.valueOf(connects[i][j]));
								writer.write("}");
							} else {
								writer.write(String.valueOf(connects[i][j]));
							}
						}
						
					}
					writer.write("\\\\");
					writer.newLine();
				}
				writer.close();
				
				TDoubleArrayList xVals = new TDoubleArrayList();
				TDoubleArrayList yVals = new TDoubleArrayList();
				TDoubleArrayList xVals2 = new TDoubleArrayList();
				TDoubleArrayList yVals2 = new TDoubleArrayList();
				for(int i = 0; i < connects.length; i++) {
					SampledVertex v = y.getVertex(ids[i]);
					int k = v.getNeighbours().size();
					int count = 0;
					int length = 0;
					for(int j = 0; j < connects.length; j++) {
						if(connects[i][j] > 0) {
							count++;
							length += connects[i][j]; 
						}
					}
					xVals.add(k);
					yVals.add(count);
					
					if(length > 0) {
						xVals2.add(k);
						yVals2.add(length/(double)count);
					}
					
				}
				
				TDoubleDoubleHashMap map = Correlations.mean(xVals.toNativeArray(), yVals.toNativeArray());
				StatsWriter.writeHistogram(map, "k", "connects", String.format("%1$s/connects_k.txt", getOutputDirectory()));
				
				map = Correlations.mean(xVals2.toNativeArray(), yVals2.toNativeArray());
				StatsWriter.writeHistogram(map, "k", "closeness", String.format("%1$s/closeness_k.txt", getOutputDirectory()));
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
