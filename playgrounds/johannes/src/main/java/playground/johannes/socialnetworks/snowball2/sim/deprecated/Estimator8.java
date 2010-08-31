/* *********************************************************************** *
 * project: org.matsim.*
 * Estimator8.java
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
import org.matsim.contrib.sna.snowball.sim.ProbabilityEstimator;

import playground.johannes.socialnetworks.snowball2.sim.SampleStats;

/**
 * @author illenberger
 *
 */
public class Estimator8 implements ProbabilityEstimator {

	private final int N;
	
	private SampleStats stats;
	
	public Estimator8(int N) {
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
			double prod = 1;
			for(int i = 0; i < vertex.getNeighbours().size(); i++) {
				SampledVertex neighbour = (SampledVertex) vertex.getNeighbours().get(i);
				double q = 0;
				if(neighbour.isSampled()) {
					q = 1 - Math.pow(1 - stats.getAccumulatedNumSampled(it - 2)/(double)N, neighbour.getNeighbours().size());
				} else {
					q = stats.getAccumulatedNumSampled(it - 1)/(double)N;
				}
				prod *= 1 - q;
			}
			
			return 1 - prod;
		}
	}

	
	public double getWeight(SampledVertex vertex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void update(SampledGraph graph) {
		stats = new SampleStats(graph);

	}

}
