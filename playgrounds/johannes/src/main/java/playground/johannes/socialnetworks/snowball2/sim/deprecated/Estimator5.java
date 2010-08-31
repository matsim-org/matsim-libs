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

import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;

import java.util.TreeSet;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.sim.ProbabilityEstimator;

import playground.johannes.socialnetworks.snowball2.sim.SampleStats;
import playground.johannes.socialnetworks.statistics.Histogram;

/**
 * @author illenberger
 *
 */
public class Estimator5 implements ProbabilityEstimator {

	private SampleStats stats;
	
	private TIntObjectHashMap<Histogram> map;
	
	private TreeSet<Integer> keys;
	
	private final int N;
	
	private int kMax;
	
	public Estimator5(int N) {
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
			
			Integer k_upper = keys.ceiling(k);
			Integer k_lower = keys.floor(k);
			
			int diff1 = Integer.MAX_VALUE;
			if(k_upper != null)
				diff1 = k_upper - k;
			
			int diff2 = Integer.MAX_VALUE;
			if(k_lower != null)
				diff2 = k - k_lower;
			
			Histogram hist;
			if(diff1 < diff2)
				hist = map.get(k_upper);
			else
				hist = map.get(k_lower);
			
			double q = 0;
			for(int k_n = 1; k_n <= kMax; k_n++) {
				double p_k_n = 1 - Math.pow(1 - stats.getAccumulatedNumSampled(it - 2)/(double)N, k_n);
				q += hist.share(k_n) * p_k_n;
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void update(SampledGraph graph) {
		stats = new SampleStats(graph);
		keys = new TreeSet<Integer>();
		TIntObjectHashMap<TIntArrayList> tmp = new TIntObjectHashMap<TIntArrayList>();

		for(Vertex vertex : graph.getVertices()) {
			
				int k = vertex.getNeighbours().size();
				
				kMax = Math.max(k, kMax);
				TIntArrayList values = tmp.get(k);
				if(values == null) {
					values = new TIntArrayList();
					tmp.put(k, values);
				}
				for(Vertex neighbor : vertex.getNeighbours()) {
					if(((SampledVertex)neighbor).isSampled()) {
						int k_n = neighbor.getNeighbours().size();
						values.add(k_n);
						kMax = Math.max(k_n, kMax);
					}
				}
				if(values.size() == 0) {
					tmp.remove(k);
				} else
					keys.add(k);
			
		}
		
		map = new TIntObjectHashMap<Histogram>();
		TIntObjectIterator<TIntArrayList> it = tmp.iterator();
		for(int i = 0; i < tmp.size(); i++) {
			it.advance();
			double[] vals = new double[it.value().size()];
			for(int k = 0; k < vals.length; k++)
				vals[k] = it.value().get(k);
			
			Histogram hist = new Histogram(vals);
			hist.convertToEqualCount(5, 1.0);
			map.put(it.key(), hist);
		}
	}

}
