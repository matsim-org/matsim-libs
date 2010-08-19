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
package playground.johannes.socialnetworks.snowball2.sim.deprecated;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.Arrays;
import java.util.TreeSet;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.snowball2.sim.ProbabilityEstimator;
import playground.johannes.socialnetworks.snowball2.sim.SampleStats;

/**
 * @author illenberger
 *
 */
public class Estimator7 implements ProbabilityEstimator {

	private SampleStats stats;
	
	private TIntObjectHashMap<TDoubleDoubleHashMap> histograms;
	
	private final int N;
	
	public Estimator7(int N) {
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
			if(isIsolated(vertex)) {
				return stats.getAccumulatedNumSampled(it)/(double)N;
			}
			
			TDoubleDoubleHashMap hist = histograms.get(k);
			double q = 0;
			TDoubleDoubleIterator iter = hist.iterator();
			for(int k_n = 0; k_n < hist.size(); k_n++) {
				iter.advance();
				double p_k_n = 1 - Math.pow(1 - stats.getAccumulatedNumSampled(it - 2)/(double)N, iter.key());
				q += iter.value() * p_k_n;
			}
			double p_k = 1 - Math.pow(1 - q, k);
			
			double p = 1;
			if(vertex.getIterationSampled() == it)
				p = stats.getNumSampled(it)/(double)stats.getNumDetected(it - 1);
			
			return p_k * p;
		}
	}

	private boolean isIsolated(SampledVertex vertex) {
		if(vertex.getIterationSampled() == 0) {
			for(Vertex neighbour : vertex.getNeighbours()) {
				if(((SampledVertex)neighbour).isSampled())
					return false;
			}
			
			return true;
		} else {
			return false;
		}
	}
	
	
	public double getWeight(SampledVertex vertex) {
		return 1/getProbability(vertex);
	}

	@Override
	public void update(SampledGraph graph) {
		stats = new SampleStats(graph);
		TreeSet<Integer> keys = new TreeSet<Integer>();
		
		int kMax = 0;
		TIntObjectHashMap<TObjectDoubleHashMap<Vertex>> kSamples = new TIntObjectHashMap<TObjectDoubleHashMap<Vertex>>();

		for(Vertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled() == true && ((SampledVertex)vertex).getIterationSampled() < stats.getMaxIteration()) {
				int k = vertex.getNeighbours().size();
				kMax = Math.max(k, kMax);
				
				TObjectDoubleHashMap<Vertex> values = kSamples.get(k);
				if(values == null) {
					values = new TObjectDoubleHashMap<Vertex>();
					kSamples.put(k, values);
				}
				
				for(Vertex neighbor : vertex.getNeighbours()) {
					if(((SampledVertex)neighbor).isSampled()) {
						int k_n = neighbor.getNeighbours().size();
						values.put(neighbor, getProbabilityEstim1(neighbor.getNeighbours().size()));
						kMax = Math.max(k_n, kMax);
					}
				}
				
				if(values.size() == 0)
					kSamples.remove(k);
				else
					keys.add(k);
			}
		}
		
		histograms = new TIntObjectHashMap<TDoubleDoubleHashMap>();
		TIntObjectIterator<TObjectDoubleHashMap<Vertex>> itHist = kSamples.iterator();
		for(int i = 0; i < kSamples.size(); i++) {
			itHist.advance();
			TObjectDoubleHashMap<Vertex> vals = itHist.value();
			TObjectDoubleIterator<Vertex> itProba = vals.iterator();
			
			Distribution distr = new Distribution();
			for(int j = 0; j < vals.size(); j++) {
				itProba.advance();
				distr.add(itProba.key().getNeighbours().size(), 1/itProba.value());
			}
			histograms.put(itHist.key(), distr.normalizedDistribution());
		}
		
		for(int k = 1; k < (kMax + 1); k++) {
//			if(k == 4)
//				System.out.println();
			if(!histograms.containsKey(k)) {
				Integer k_upper = keys.ceiling(k);
				Integer k_lower = keys.floor(k);
				
				int diff_upper = Integer.MAX_VALUE;
				if(k_upper != null)
					diff_upper = k_upper - k;
				
				int diff_lower = Integer.MAX_VALUE;
				if(k_lower != null)
					diff_lower = k - k_lower;
				
				TDoubleDoubleHashMap hist;
				if(diff_upper < diff_lower) {
					hist = histograms.get(k_upper);
				} else {
					hist = histograms.get(k_lower);
				}
				
				histograms.put(k, hist);
			}
		}
	}
	
	public double getProbabilityEstim1(int k) {
		int it = stats.getMaxIteration()-1;
		if(it <= 0)
			return stats.getNumSampled(0)/(double)N;
		else {
			int n = stats.getAccumulatedNumSampled(it - 1);
			double p_k = 1 - Math.pow(1 - n/(double)N, k);
			double p = 1;
//			if(vertex.getIterationSampled() == it)
//				p = stats.getNumSampled(it)/((double)stats.getNumDetected(it - 1) * stats.getResonseRate());
			return p * p_k;
		}
	}
}
