/* *********************************************************************** *
 * project: org.matsim.*
 * EstimatorTask.java
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
package playground.johannes.socialnetworks.snowball2.sim;

import gnu.trove.TIntDoubleHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.snowball.SampledVertex;


/**
 * @author illenberger
 *
 */
public class EstimatorTask extends AnalyzerTask {

	private final BiasedDistribution estimator;
	
	public EstimatorTask(BiasedDistribution estimator) {
		this.estimator = estimator;
	}
	
	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		TIntDoubleHashMap probas = new TIntDoubleHashMap();
		
		for(Vertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled()) {
				probas.put(vertex.getNeighbours().size(), estimator.getProbability((SampledVertex) vertex));
			}
		}
		
		writeValues(probas, "proba");	
	}

	private void writeValues(TIntDoubleHashMap values, String valName) {
		int[] keys = values.keys();
		Arrays.sort(keys);
		
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(String.format("%1$s/%2$s.txt", getOutputDirectory(), valName)));
			writer.write("k\t");
			writer.write(valName);
			writer.newLine();
			for(int key : keys) {
				writer.write(String.valueOf(key));
				writer.write("\t");
				writer.write(String.valueOf(values.get(key)));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
