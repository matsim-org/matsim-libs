/* *********************************************************************** *
 * project: org.matsim.*
 * NormalizedEstimator.java
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

import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.PiEstimator;

/**
 * This estimator normalizes the estimates from a delegate estimator such that
 * <code>\sum_i{1/p_i} = N</code>.
 * 
 * @author illenberger
 * 
 */
public class NormalizedEstimator implements PiEstimator {

	private PiEstimator delegate;

	private final double N;

	private TIntDoubleHashMap konst = new TIntDoubleHashMap();

	private int currentIt;
	
	/**
	 * Creates a new estimator.
	 * 
	 * @param delegate
	 *            a delegate estimator
	 * @param N
	 *            the size of the total population of vertices
	 */
	public NormalizedEstimator(PiEstimator delegate, int N) {
		this.delegate = delegate;
		this.N = N;
	}

	/**
	 * Returns the estimate of the delegate estimator multiplied with
	 * <code>\sum_i{1/p_i} / N</code>.
	 * 
	 * @param vertex
	 *            a sampled vertex
	 * @return the estimate of the delegate estimator multiplied with
	 *         <code>\sum_i{1/p_i} / N</code>.
	 */
	@Override
	public double probability(SampledVertex vertex) {
		return konst.get(currentIt) * delegate.probability(vertex);
	}

	/**
	 * Updates the delegate estaimtor and calculates the normalization constant.
	 * 
	 * @param graph
	 *            a sampled graph
	 */
	@Override
	public void update(SampledGraph graph) {
		SampleStats stats = new SampleStats(graph);
		currentIt = stats.getMaxIteration();
		
		delegate.update(graph);

		double sum = 0;
		for (SampledVertex vertex : graph.getVertices()) {
			if (vertex.isSampled()) {
				double p = delegate.probability(vertex);
				if(p > 0)
					sum += 1 / p;
			}
		}

		konst.put(currentIt, sum / (double) N);
		
	}

	@Override
	public double probability(SampledVertex vertex, int iteration) {
		return konst.get(Math.max(0, iteration - 1)) * delegate.probability(vertex, iteration);
	}

}
