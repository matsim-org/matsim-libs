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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntDoubleIterator;
import gnu.trove.TIntIntHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.AnalyzerTask;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.sim.ProbabilityEstimator;

/**
 * @author illenberger
 * 
 */
public class EstimatorTask extends AnalyzerTask {

	private final ProbabilityEstimator estimator;

	public EstimatorTask(ProbabilityEstimator estimator) {
		this.estimator = estimator;
	}

	@Override
	public void analyze(Graph graph, Map<String, Double> stats) {
		TIntDoubleHashMap probas = new TIntDoubleHashMap(graph.getVertices().size());
		TIntIntHashMap counts = new TIntIntHashMap(graph.getVertices().size());

		double N_estim = 0;
		for (Vertex vertex : graph.getVertices()) {
			if (((SampledVertex) vertex).isSampled()) {
				int k = vertex.getNeighbours().size();
				double p = estimator.getProbability((SampledVertex) vertex);

				probas.adjustOrPutValue(k, p, p);
				counts.adjustOrPutValue(k, 1, 1);

				if (p > 0)
					N_estim += 1 / p;
			}
		}
		stats.put("N_estim", N_estim);
		
		double M_estim = 0;
		for (Edge edge : graph.getEdges()) {
			SampledVertex v_i = (SampledVertex) edge.getVertices().getFirst();
			SampledVertex v_j = (SampledVertex) edge.getVertices().getSecond();
			if (v_i.isSampled() && v_j.isSampled()) {
				double p_i = estimator.getProbability(v_i);
				double p_j = estimator.getProbability(v_j);
				if (p_i > 0 && p_j > 0) {
					M_estim += 1 / ((p_i + p_j) - (p_i * p_j));
//					M_estim += 1/(p_i * p_j);
				}
			}
		}
		
		stats.put("M_estim", M_estim);
		
		TIntDoubleIterator it = probas.iterator();
		for (int i = 0; i < probas.size(); i++) {
			it.advance();
			it.setValue(it.value() / counts.get(it.key()));
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
			for (int key : keys) {
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
