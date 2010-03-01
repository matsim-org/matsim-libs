/* *********************************************************************** *
 * project: org.matsim.*
 * Estimator3.java
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

import gnu.trove.TIntIntHashMap;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class Estimator3 implements Estimator, SamplerListener {

	private final int interval;
	
	private final int N;
	
	private int iteration;

	private double p_random;
	
	private double p_snowballNeighbor;
	
	private int lastIteration;
	
	public Estimator3(int interval, int N) {
		this.N = N;
		this.interval = interval;
	}
	
	@Override
	public double getProbability(SampledVertex vertex) {
		if(iteration == 0)
			return p_random;
		else
			return 1 - Math.pow(1 - p_snowballNeighbor, vertex.getNeighbours().size());
	}

	@Override
	public double getWeight(SampledVertex vertex) {
		return p_random/getProbability(vertex);
	}

	@Override
	public void update(SampledGraph graph) {
		iteration = 0;
		TIntIntHashMap sampleSize = new TIntIntHashMap();
//		Collection<SampledVertex> samples = new ArrayList<SampledVertex>(graph.getVertices().size());
		/*
		 * count samples per iteration
		 */
		for(Vertex vertex : graph.getVertices()) {
			sampleSize.adjustOrPutValue(((SampledVertex)vertex).getIterationSampled(), 1, 1);
			iteration = Math.max(iteration, ((SampledVertex)vertex).getIterationSampled());
//			samples.add((SampledVertex)vertex);
		}
		
		updatePRandom(sampleSize);
		updatePSnowballNeighbor(sampleSize);
	}

	private void updatePRandom(TIntIntHashMap samples) {
		int n_before = 0;
		for(int i = 0; i < iteration; i++)
			n_before += samples.get(i);
		
		int n = samples.get(iteration);
		
		p_random = n/(double)(N - n_before);
	}
	
	private void updatePSnowballNeighbor(TIntIntHashMap samples) {
		int n_before = 0;
		for(int i = 0; i < iteration-1; i++)
			n_before += samples.get(i);
		
		int n = samples.get(iteration-1);
		
		p_snowballNeighbor = n/(double)(N - n_before);
	}

	@Override
	public boolean afterSampling(Sampler<?, ?, ?> sampler) {
		if(sampler.getNumSampledVertices() % interval == 0) {
			update(sampler.getSampledGraph());
		}
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
