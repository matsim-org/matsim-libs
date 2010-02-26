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

import java.util.ArrayList;
import java.util.Collection;

import gnu.trove.TIntIntHashMap;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class Estimator1 implements Estimator, SamplerListener {

	private final int N;
	
	private int n;
	
	private int iteration;
	
	private double norm;
	
	private int lastIteration;
	
	public Estimator1(int N) {
		this.N = N;
	}
	
	public void update(SampledGraph graph) {
		iteration = 0;
		TIntIntHashMap sampleSize = new TIntIntHashMap();
		Collection<SampledVertex> samples = new ArrayList<SampledVertex>(graph.getVertices().size());
		/*
		 * count samples per iteration
		 */
		for(Vertex vertex : graph.getVertices()) {
			sampleSize.adjustOrPutValue(((SampledVertex)vertex).getIterationSampled(), 1, 1);
			iteration = Math.max(iteration, ((SampledVertex)vertex).getIterationSampled());
			samples.add((SampledVertex)vertex);
		}
		/*
		 * count samples
		 */
		n = 0;
		if(iteration == 0) {
			n = sampleSize.get(0);
		} else {
			for(int i = 0; i < iteration; i++) {
				n += sampleSize.get(i);
			}
		}
		/*
		 * calculate normalization constant
		 */
		double wsum = 0;
		for(SampledVertex vertex : samples) {
			wsum += 1/getProbability(vertex);
		}
		
		norm = samples.size()/wsum;
	}
	
	public double getProbability(SampledVertex vertex) {
		if(iteration == 0)
			return n/(double)N;
		else
			return 1 - Math.pow(1 - n/(double)N, vertex.getNeighbours().size());
	}
	
	@Override
	public double getWeight(SampledVertex vertex) {
		return 1 / getProbability(vertex) * norm;
	}

	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler) {
		return true;
	}

	@Override
	public boolean beforeSampling(Sampler<?, ?, ?> sampler) {
		if(sampler.getIteration() > lastIteration) {
			update(sampler.getSampledGraph());
			lastIteration = sampler.getIteration();
		}
		return true;
	}
}
