/* *********************************************************************** *
 * project: org.matsim.*
 * Estimator2.java
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

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class Estimator2 extends Estimator1 {

	private final int interval;
	
	private final int N;
	
	private double p_random;
	
	public Estimator2(int N, int interval) {
		super(N);
		this.N = N;
		this.interval = interval;
	}
	
	@Override
	public double getWeight(SampledVertex vertex) {
		return p_random/getProbability(vertex);
	}

	@Override
	public void update(SampledGraph graph) {
		int count = 0;
		for(Vertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled())
				count++;
		}
		
		p_random = count/(double)N;

		super.update(graph);
	}

	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler) {
		if(sampler.getNumSampledVertices() % interval == 0) {
			update(sampler.getSampledGraph());
		}
		return true;
	}

}
