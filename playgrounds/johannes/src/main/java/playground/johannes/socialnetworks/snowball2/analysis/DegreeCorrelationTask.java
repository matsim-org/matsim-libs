/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeCorrelationTask.java
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
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.math.stat.StatUtils;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.snowball.SampledVertex;


/**
 * @author illenberger
 *
 */
public class DegreeCorrelationTask extends ModuleAnalyzerTask<Degree> {

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.graph.analysis.AnalyzerTask#analyze(org.matsim.contrib.sna.graph.Graph, java.util.Map)
	 */
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		TIntObjectHashMap<TIntArrayList> map = new TIntObjectHashMap<TIntArrayList>();
		int maxValues = 0;
		for(Vertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled()) {
				int k = vertex.getNeighbours().size();
				TIntArrayList values = map.get(k);
				if(values == null) {
					values = new TIntArrayList();
					map.put(k, values);
				}
				for(Vertex neighbor : vertex.getNeighbours()) {
					if(((SampledVertex)neighbor).isSampled()) {
						values.add(neighbor.getNeighbours().size());
					}
				}
				
				maxValues = Math.max(maxValues, values.size());
			}
		}
		
		if(getOutputDirectory() != null) {
			int[] keys = map.keys();
			Arrays.sort(keys);
			
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/degreeCorrelaion.txt"));
				for (int k : keys) {
					writer.write(String.valueOf(k));
					writer.write("\t");
				}
				writer.newLine();

				for (int i = 0; i < maxValues; i++) {
					for (int k : keys) {
						TIntArrayList values = map.get(k);
						if(values.size() > i) {
							writer.write(String.valueOf(values.get(i)));
						} else {
							writer.write("NA");
						}
						writer.write("\t");
					}
					writer.newLine();
				}
				writer.close();
				
				
				writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/degreeAvr.txt"));
				writer.write("k_i\tk_j_mean");
				writer.newLine();
				for (int k : keys) {
					writer.write(String.valueOf(k));
					writer.write("\t");
					TIntArrayList values = map.get(k);
					double sum = 0;
					for(int i = 0; i < values.size(); i++)
						sum += values.get(i);
					
					writer.write(String.valueOf(sum/(double)values.size()));
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
