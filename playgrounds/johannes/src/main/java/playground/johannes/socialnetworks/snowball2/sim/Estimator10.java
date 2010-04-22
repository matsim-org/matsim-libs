/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballEstimator.java
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

import gnu.trove.TObjectDoubleHashMap;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class Estimator10 implements BiasedDistribution {

	private final int N;
	
	private SampleStats stats;
	
	private TObjectDoubleHashMap<SampledVertex> probas;
	
	private double c;
	
	public Estimator10(int N) {
		this.N = N;
	}
	
	public void update(SampledGraph graph) {
		stats = new SampleStats(graph);
		probas = new TObjectDoubleHashMap<SampledVertex>();
		
		double p_sum = 0;
		for(Vertex vertex : graph.getVertices()) {
			SampledVertex v = (SampledVertex)vertex;
			if(v.isSampled()) {
				double p = getProbabilityIntern(v);
				probas.put(v, p);
				p_sum += 1/p;
			}
		}
		
		c = N/p_sum;
	}
	
	public double getProbabilityIntern(SampledVertex vertex) {
		int it = stats.getMaxIteration();
		if(it == 0)
			return stats.getNumSampled(0)/(double)N;
		else {
			int n = stats.getAccumulatedNumSampled(it - 1);
			double p_k = 1 - Math.pow(1 - n/(double)N, vertex.getNeighbours().size());
			double p = 1;
//			if(vertex.getIterationSampled() == it)
//				p = stats.getNumSampled(it)/((double)stats.getNumDetected(it - 1) * stats.getResonseRate());
			return p * p_k;
		}
	}
	
	@Override
	public double getWeight(SampledVertex vertex) {
		return 1 / getProbability(vertex) * c;
	}

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.snowball2.sim.BiasedDistribution#getProbability(org.matsim.contrib.sna.snowball.SampledVertex)
	 */
	@Override
	public double getProbability(SampledVertex vertex) {
		return probas.get(vertex);
	}
}
