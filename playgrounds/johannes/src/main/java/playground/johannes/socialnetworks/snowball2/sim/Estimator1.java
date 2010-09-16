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

import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.sim.ProbabilityEstimator;

/**
 * Estimates the inclusion probability of a vertex based on its degree and the
 * number of sampled vertices. <br>
 * p_i = 1 - (1 - (n^{(it - 1)} / N))^{k_i}
 * 
 * @author illenberger
 * 
 */
public class Estimator1 implements ProbabilityEstimator {

	private final int N;

	private SampleStats stats;

	/**
	 * Creates a new estimator.
	 * 
	 * @param N
	 *            the total population of vertices.
	 */
	public Estimator1(int N) {
		this.N = N;
	}

	/**
	 * @see {@link ProbabilityEstimator#update(SampledGraph)}
	 */
	public void update(SampledGraph graph) {
		stats = new SampleStats(graph);
	}

	/**
	 * Estimates the inclusion probability of a vertex based on its degree and
	 * the number of vertices sampled.
	 * 
	 * @param vertex
	 *            a sampled vertex
	 */
	public double getProbability(SampledVertex vertex) {
//		int it = vertex.getIterationSampled();//stats.getMaxIteration();
		int it = stats.getMaxIteration();
		
		if (it == 0)
			/*
			 * In the 0th iteration we have random sampling.
			 */
			return stats.getNumSampled(0) / (double) N;
		
		else {
			int n = stats.getAccumulatedNumSampled(it - 1);
			/*
			 * inclusion probability
			 */
			double p_k = 1 - Math.pow(1 - n / (double) N, vertex.getNeighbours().size());
//			/*
//			 * response rate
//			 */
//			double p = 1;
//			if (vertex.getIterationSampled() == it)
//				p = stats.getNumSampled(it) / ((double) stats.getNumDetected(it - 1) * stats.getResonseRate());
			
//			return p_k;
			return stats.getResponseRate(it) * p_k;
		}
	}
}