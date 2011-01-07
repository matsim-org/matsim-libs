/* *********************************************************************** *
 * project: org.matsim.*
 * Estimator4.java
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

import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.PiEstimator;

import playground.johannes.socialnetworks.snowball2.sim.SampleStats;

/**
 * @author illenberger
 *
 */
public class Estimator4 implements PiEstimator {

	private final int N;
	
	private SampleStats stats;
	
	public Estimator4(int N) {
		this.N = N;
	}
	
	@Override
	public double probability(SampledVertex vertex) {
		int it = stats.getMaxIteration();
		int k = vertex.getNeighbours().size();
		
		if(it == 0) {
			return stats.getAccumulatedNumSampled(it)/(double)N;
		} else if(it == 1) {
			int n = stats.getAccumulatedNumSampled(it - 1);
			return 1 - Math.pow(1 - n/(double)N, k);
		} else {
			double prod = 1;
			for(int i = 0; i <= it; i++)
				prod *= (1 - getIterationProbability(vertex, i));
			
			double p_k = 1 - prod;
			
			double p = 1;
			if(vertex.getIterationSampled() == it)
				p = stats.getNumSampled(it)/(double)stats.getNumDetected(it - 1);
			
			return p * p_k; 
		}
	}

	private double getIterationProbability(SampledVertex vertex, int it) {
		if(it == 0) {
			return stats.getAccumulatedNumSampled(it)/(double)N;
		} else if(it == 1) {
			int n = stats.getAccumulatedNumSampled(it - 1);
			return 1 - Math.pow(1 - n/(double)N, vertex.getNeighbours().size());
		} else {
			int n_i = stats.getNumSampled(it - 1);
			int N_i = N - (stats.getAccumulatedNumSampled(it - 2) + stats.getNumSampled(it));
			
			return 1 - Math.pow(1 - n_i/(double)N_i, vertex.getNeighbours().size());
		}
		
	}


	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.snowball2.sim.BiasedDistribution#update(org.matsim.contrib.sna.snowball.SampledGraph)
	 */
	@Override
	public void update(SampledGraph graph) {
		stats = new SampleStats(graph);

	}



	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.snowball.analysis.PiEstimator#probability(org.matsim.contrib.sna.snowball.SampledVertex, int)
	 */
	@Override
	public double probability(SampledVertex vertex, int iteration) {
		// TODO Auto-generated method stub
		return 0;
	}

}
