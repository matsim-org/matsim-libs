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
package playground.johannes.sna.snowball.analysis;

import playground.johannes.sna.snowball.SampledGraph;
import playground.johannes.sna.snowball.SampledVertex;

/**
 * Estimates the inclusion probability of a vertex based on its degree and the
 * number of sampled vertices. <br>
 * p_i = alpha * (1 - (1 - (n^{(it - 1)} / N))^{k_i}),<br>
 * where <tt>alpha</tt> denotes the response rate.
 * 
 * @author illenberger
 * 
 */
public class SimplePiEstimator implements PiEstimator {

	private final int N;

	private int[] numVertices;
	
	private double[] responseRate;
	
	private int lastIteration;

	/**
	 * Creates a new estimator.
	 * 
	 * @param N
	 *            the total population of vertices.
	 */
	public SimplePiEstimator(int N) {
		this.N = N;
	}

	/**
	 * @see {@link PiEstimator#update(SampledGraph)}
	 */
	public void update(SampledGraph graph) {
		numVertices = SnowballStatistics.getInstance().numVerticesSampledTotal(graph.getVertices());
		responseRate = SnowballStatistics.getInstance().responseRateTotal(graph.getVertices());
		lastIteration = SnowballStatistics.getInstance().lastIteration(graph.getVertices());
	}

	@Override
	public double probability(SampledVertex vertex) {
		return probability(vertex, lastIteration);
	}

	/**
	 * Estimates the inclusion probability of a vertex based on its degree and
	 * the number of vertices sampled.
	 * 
	 * @param vertex
	 *            a sampled vertex
	 */
	public double probability(SampledVertex vertex, int iteration) {
		if (iteration == 0)
			/*
			 * In the 0th iteration we have random sampling.
			 */
			return numVertices[0] / (double) N;
		
		else {
			int n = numVertices[iteration - 1];
			/*
			 * inclusion probability
			 */
			double p_k = 1 - Math.pow(1 - n / (double) N, vertex.getNeighbours().size());

			return responseRate[iteration] * p_k;
		}
	}
}