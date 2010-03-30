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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.TreeSet;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class Estimator7 implements BiasedDistribution {

	private SampleStats stats;
	
	private TIntObjectHashMap<TDoubleDoubleHashMap> map;
	
	private TreeSet<Integer> keys;
	
	private final int N;
	
	private int kMax;
	
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
			
			Integer k_upper = keys.ceiling(k);
			Integer k_lower = keys.floor(k);
			
			int diff1 = Integer.MAX_VALUE;
			if(k_upper != null)
				diff1 = k_upper - k;
			
			int diff2 = Integer.MAX_VALUE;
			if(k_lower != null)
				diff2 = k - k_lower;
			
			TDoubleDoubleHashMap hist;
			if(diff1 < diff2) {
				hist = map.get(k_upper);
			} else {
				hist = map.get(k_lower);
			}
			
			double q = 0;
//			for(int k_n = 1; k_n <= kMax; k_n++) {
//				double p_k_n = 1 - Math.pow(1 - stats.getAccumulatedNumSampled(it - 2)/(double)N, k_n);
//				q += hist.share(k_n) * p_k_n;
//			}
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
	
	@Override
	public double getWeight(SampledVertex vertex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void update(SampledGraph graph) {
		stats = new SampleStats(graph);
		keys = new TreeSet<Integer>();
		
		TIntObjectHashMap<TObjectDoubleHashMap<Vertex>> tmp = new TIntObjectHashMap<TObjectDoubleHashMap<Vertex>>();

		for(Vertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled() == true && ((SampledVertex)vertex).getIterationSampled() < stats.getMaxIteration()) {
				int k = vertex.getNeighbours().size();
				
				kMax = Math.max(k, kMax);
				TObjectDoubleHashMap<Vertex> values = tmp.get(k);
				if(values == null) {
					values = new TObjectDoubleHashMap<Vertex>();
					tmp.put(k, values);
				}
				for(Vertex neighbor : vertex.getNeighbours()) {
					if(((SampledVertex)neighbor).isSampled()) {
						int k_n = neighbor.getNeighbours().size();
						values.put(neighbor, getProbabilityEstim1(neighbor.getNeighbours().size()));
						kMax = Math.max(k_n, kMax);
					}
				}
				if(values.size() == 0) {
					tmp.remove(k);
				} else
					keys.add(k);
			}
		}
		
		map = new TIntObjectHashMap<TDoubleDoubleHashMap>();
		TIntObjectIterator<TObjectDoubleHashMap<Vertex>> it = tmp.iterator();
		for(int i = 0; i < tmp.size(); i++) {
			it.advance();
//			System.out.println(String.format("k=%1$s, count=%2$s.", it.key(), it.value().size()));
			TObjectDoubleHashMap<Vertex> vals = it.value();
			Distribution distr = new Distribution();
			TObjectDoubleIterator<Vertex> it2 = vals.iterator();
			for(int k = 0; k < vals.size(); k++) {
				it2.advance();
				distr.add(it2.key().getNeighbours().size(), 1/(double)it2.value());
			}
			
			map.put(it.key(), distr.normalizedDistribution());
		}
		
//		try {
//		BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("/Users/jillenberger/Work/work/socialnets/snowball/output/%1$s.degreeCorrelaion.txt", stats.getMaxIteration())));
//		
//		int[] keyList = map.keys();
//		Arrays.sort(keyList);
//		
//		for (int k : keyList) {
//			writer.write(String.valueOf(k));
//			writer.write("\t");
//		}
//		writer.newLine();
//
//		for (int k = 1; k < kMax; k++) {
//			for (int i : keyList) {
//				TDoubleDoubleHashMap values = map.get(i);
//				writer.write(String.valueOf(values.get(k)));
//				writer.write("\t");
//			}
//			writer.newLine();
//		}
//		writer.close();
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
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
	
//	public double getProbabilityEstim8(SampledVertex vertex) {
//		int it = stats.getMaxIteration() - 1;
//		int k = vertex.getNeighbours().size();
//		
//		if(it == 0) {
//			return stats.getAccumulatedNumSampled(it)/(double)N;
//		} else if(it == 1) {
//			int n = stats.getAccumulatedNumSampled(it - 1);
//			return 1 - Math.pow(1 - n/(double)N, k);
//		} else {
//			double prod = 1;
//			for(int i = 0; i < vertex.getNeighbours().size(); i++) {
//				SampledVertex neighbour = (SampledVertex) vertex.getNeighbours().get(i);
//				double q = 0;
//				if(neighbour.isSampled()) {
//					q = 1 - Math.pow(1 - stats.getAccumulatedNumSampled(it - 2)/(double)N, neighbour.getNeighbours().size());
//				} else {
//					q = stats.getAccumulatedNumSampled(it - 1)/(double)N;
//				}
//				prod *= 1 - q;
//			}
//			
//			return 1 - prod;
//		}
//	}

}
