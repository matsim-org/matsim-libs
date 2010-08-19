/* *********************************************************************** *
 * project: org.matsim.*
 * Estimator9.java
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
package playground.johannes.socialnetworks.snowball2.sim.deprecated;

import gnu.trove.TIntIntHashMap;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.snowball2.sim.ProbabilityEstimator;
import playground.johannes.socialnetworks.snowball2.sim.SampleStats;

/**
 * @author illenberger
 *
 */
public class Estimator9 implements ProbabilityEstimator {

	private TIntIntHashMap numNeighbors;
	
	private SampleStats stats;
	
	private double C;
	
	private final int N;
	
	public Estimator9(int N) {
		this.N = N;
	}
	
	@Override
	public double getProbability(SampledVertex vertex) {
		int it = stats.getMaxIteration();
		int k = vertex.getNeighbours().size();
		
		if(it == 0) {
			return stats.getAccumulatedNumSampled(it)/(double)N;
		} else if(it == 1) {
			int n = stats.getAccumulatedNumSampled(it - 1);
			return 1 - Math.pow(1 - n/(double)N, k);
		} else {
			double p_w = C * numNeighbors.get(k) / (double)N;
			return 1 - Math.pow(1 - p_w, k);
		}
	}

	
	public double getWeight(SampledVertex vertex) {
		return 0;
	}

	@Override
	public void update(SampledGraph graph) {
		stats = new SampleStats(graph);
		
		numNeighbors = new TIntIntHashMap();
		for(Vertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled() && ((SampledVertex)vertex).getIterationSampled() < stats.getMaxIteration()) {
				for(Vertex neighbor : vertex.getNeighbours()) {
					if(((SampledVertex)neighbor).isSampled()) {// && ((SampledVertex)neighbor).getIterationSampled() < stats.getMaxIteration()) {
						numNeighbors.adjustOrPutValue(neighbor.getNeighbours().size(), 1, 1);
					}
				}
			}
		}
		
		double sum = 0;
		for(Vertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled() && ((SampledVertex)vertex).getIterationSampled() < stats.getMaxIteration()) {
				sum += numNeighbors.get(vertex.getNeighbours().size());
			}
		}
		
		C = Math.pow(stats.getAccumulatedNumSampled(stats.getMaxIteration() - 1), 2) / sum;
	}

}
