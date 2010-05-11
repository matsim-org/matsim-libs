/* *********************************************************************** *
 * project: org.matsim.*
 * Estimator11.java
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

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.list.FixedSizeList;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.statistics.Discretizer;
import playground.johannes.socialnetworks.statistics.FixedSampleSizeDiscretizer;

import visad.data.netcdf.UnsupportedOperationException;

/**
 * @author illenberger
 *
 */
public class Estimator11 implements BiasedDistribution {

	private SampleStats stats;
	
	private SampledGraph graph;
	
	private final int N;
	
	private TIntObjectHashMap<TObjectDoubleHashMap<SampledVertex>> probaItMap = new TIntObjectHashMap<TObjectDoubleHashMap<SampledVertex>>();
	
	private TIntObjectHashMap<TIntIntHashMap> numNeighborKIt = new TIntObjectHashMap<TIntIntHashMap>();
	
	private TIntObjectHashMap<TIntDoubleHashMap> probaNeighborKIt = new TIntObjectHashMap<TIntDoubleHashMap>();
	
	private Map<SampledVertex, TIntIntHashMap> deltaVertex = new HashMap<SampledVertex, TIntIntHashMap>();
	
	private Discretizer discretizer;
	
	public Estimator11(int N) {
		this.N = N;
	}
	
	@Override
	public double getProbability(SampledVertex vertex) {
		return getProbability(vertex, stats.getMaxIteration());
	}

	private double getProbability(SampledVertex vertex, int it) {
		TObjectDoubleHashMap<SampledVertex> probas = probaItMap.get(it);
		if(probas == null) {
			probas = new TObjectDoubleHashMap<SampledVertex>();
			probaItMap.put(it, probas);
		}
		
		double p = probas.get(vertex);
		
		if(p == 0) {
			if(it == 0)
				p = stats.getNumSampled(0)/(double)N;
			
			else if(it == 1) {
				int n = stats.getAccumulatedNumSampled(it - 1);	
				p = 1 - Math.pow(1 - n/(double)N, vertex.getNeighbours().size());
				
			} else {
				int k = vertex.getNeighbours().size();
				
				TIntIntHashMap numK = numNeighborKIt.get(it);
				if(numK == null) {
					numK = new TIntIntHashMap();
					numNeighborKIt.put(it, numK);
				}
				
				int num_k = 0;
				if(numK.contains(k)) {
					num_k = numK.get(k);
				} else {
					num_k = numWithNeighborK(it, k);
					numK.put(k, num_k);
				}
				
				TIntDoubleHashMap probaK = probaNeighborKIt.get(it);
				if(probaK == null) {
					probaK = new TIntDoubleHashMap();
					probaNeighborKIt.put(it, probaK);
				}
				
				double proba_k = 0;
				if(probaK.contains(k)) {
					proba_k = probaK.get(k);
				} else {
					proba_k = probaNeighborK(k, it);
					probaK.put(k, proba_k);
				}
				
				p = 1 - Math.pow(1 - num_k / proba_k, k);
			}
			
			probas.put(vertex, p);
		}
		
		return p;
	}
	
	private int numWithNeighborK(int it, int k) {
		int count = 0;
		
		for(Vertex v : graph.getVertices()) {
			
			SampledVertex vertex = (SampledVertex)v;
			if(vertex.isSampled() && vertex.getIterationSampled() < it) {
				
				count += hasNeighborK(vertex, k);
				
			}
			
		}
		
		return count;
	}
	
	private double probaNeighborK(int k, int it) {
//		int k = vertex.getNeighbours().size();
		double p_sum = 0;
		
		for(Vertex v : graph.getVertices()) {
			
			SampledVertex sv = (SampledVertex)v;
			if(sv.isSampled() && sv.getIterationSampled() < it) {
				
				int delta = hasNeighborK(sv, k);
				
				if(delta > 0) {
					double p = getProbability(sv, it-1);
					p_sum += delta/p;
				}
			}
		}
		
		return p_sum;
	}
	
	final double binsize = 10.0;
	
	private int hasNeighborK(SampledVertex vertex, int k) {
		TIntIntHashMap deltas = deltaVertex.get(vertex);
		if(deltas == null) {
			deltas = new TIntIntHashMap();
			deltaVertex.put(vertex, deltas);
		}
		
		int delta = 0;
		if(deltas.contains(k))
			delta = deltas.get(k);
		else {
			int bin = discretize(k);
			for(Vertex n : vertex.getNeighbours()) {
				SampledVertex neighbor = (SampledVertex)n;
				if(neighbor.isSampled()) {
					int k_n = neighbor.getNeighbours().size(); 
					if(discretize(k_n) == bin) {
						delta = 1;
						break;
					}
				}
			}
			
			deltas.put(k, delta);
		}
		
		return delta;
	}
	
	private int discretize(int k) {
//		int bin = (int) Math.floor(Math.log(k)/Math.log(2.0));
//		return Math.max(bin, 0);
		return (int) discretizer.discretize(k);
	}
	
	@Override
	public double getWeight(SampledVertex vertex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(SampledGraph graph) {
		stats = new SampleStats(graph);
		this.graph = graph;
		
		TDoubleArrayList degrees = new TDoubleArrayList(graph.getVertices().size());
		for(Vertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled()) {
				degrees.add(vertex.getNeighbours().size());
			}
		}
		discretizer = new FixedSampleSizeDiscretizer(degrees.toNativeArray(), 300);
	}

}
