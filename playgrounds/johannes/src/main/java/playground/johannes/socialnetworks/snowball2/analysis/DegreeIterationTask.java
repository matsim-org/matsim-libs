/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeIterationTask.java
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

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.analysis.ModuleAnalyzerTask;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class DegreeIterationTask extends ModuleAnalyzerTask<Degree> {

	private static final Logger logger = Logger.getLogger(DegreeIterationTask.class);
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		TIntObjectHashMap<Set<SampledVertex>> verticesIt = new TIntObjectHashMap<Set<SampledVertex>>();
		TIntObjectHashMap<Set<SampledVertex>> verticesItAcc = new TIntObjectHashMap<Set<SampledVertex>>();
		
		for(Vertex vertex : graph.getVertices()) {
			int it = ((SampledVertex)vertex).getIterationSampled();
			
			Set<SampledVertex> vertices = verticesIt.get(it); 
			if(vertices == null) {
				vertices = new HashSet<SampledVertex>();
				verticesIt.put(it, vertices);
			}
			vertices.add((SampledVertex)vertex);
			
//			for(int it2 = 0; it2 <= it; it2++) {
//				vertices = verticesItAcc.get(it2); 
//				if(vertices == null) {
//					vertices = new HashSet<SampledVertex>();
//					verticesItAcc.put(it2, vertices);
//				}
//				vertices.add((SampledVertex)vertex);
//			}
		}
		verticesIt.remove(-1);
		int keys[] = verticesIt.keys();
		Arrays.sort(keys);
		for(int it : keys) {
			Set<SampledVertex> vertices = new HashSet<SampledVertex>();
			if(it > 0)
				vertices.addAll(verticesItAcc.get(it - 1));
			vertices.addAll(verticesIt.get(it));
			verticesItAcc.put(it, vertices);
		}
		
		TIntDoubleHashMap kIt = new TIntDoubleHashMap();
		TIntDoubleHashMap kItAcc = new TIntDoubleHashMap();
		
		
		for(int it : keys) {
			Set<SampledVertex> vertices = verticesIt.get(it);
			kIt.put(it,module.distribution(vertices).mean());
			
			vertices = verticesItAcc.get(it);
			kItAcc.put(it, module.distribution(vertices).mean());
		}
		
		if(getOutputDirectory() == null) {
			logger.warn("No output directory specified!");
		} else {
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputDirectory() + "/k_it.txt"));
				writer.write("it\tk_mean\tk_mean_acc");
				writer.newLine();
				for(int it : keys) {
					writer.write(String.valueOf(it));
					writer.write("\t");
					writer.write(String.valueOf(kIt.get(it)));
					writer.write("\t");
					writer.write(String.valueOf(kItAcc.get(it)));
					writer.newLine();
				}
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
