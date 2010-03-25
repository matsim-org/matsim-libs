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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectIterator;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

import playground.johannes.socialnetworks.statistics.EstimatedDistribution;

/**
 * @author illenberger
 *
 */
public class Estimator6 implements BiasedDistribution {
	
	private TDoubleDoubleHashMap kMap;
	
	private SampleStats stats;
	
	private final int N;
	
	
	
	public Estimator6(int N) {
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
			double k_neighbor = kMap.get(k);
			double p_neighbor = 1 - Math.pow(1 - stats.getAccumulatedNumSampled(it - 2)/(double)N, k_neighbor);
			
			double p_k = 1 - Math.pow(1 - p_neighbor, k);
			
			double p = 1;
			if(vertex.getIterationSampled() == it)
				p = stats.getNumSampled(it)/(double)stats.getNumDetected(it - 1);
			
			return p * p_k;
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
		
		TIntObjectHashMap<TIntArrayList> tmp = new TIntObjectHashMap<TIntArrayList>();

		for(Vertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled()) {
				int k = vertex.getNeighbours().size();
				TIntArrayList values = tmp.get(k);
				if(values == null) {
					values = new TIntArrayList();
					tmp.put(k, values);
				}
				for(Vertex neighbor : vertex.getNeighbours()) {
					if(((SampledVertex)neighbor).isSampled()) {
						values.add(neighbor.getNeighbours().size());
					}
				}
			}
		}

		kMap = new TDoubleDoubleHashMap();
		TIntObjectIterator<TIntArrayList> it = tmp.iterator();
		for(int i = 0; i < tmp.size(); i++) {
			it.advance();
			TIntArrayList list = it.value();
//			double[] dList = new double[list.size()];
			Distribution distr = new EstimatedDistribution(null);
			for(int k = 0; k < list.size(); k++) {
//				dList[k] = list.get(k);
				int val = list.get(k);
				distr.add(val, 1/getProbabilityEstim1(val));
			}
			kMap.put(it.key(), distr.mean());
//			kMap.put(it.key(), StatUtils.geometricMean(dList));
		}
		
		try {
			Distribution.writeHistogram(kMap, String.format("/Users/jillenberger/Work/work/socialnets/snowball/output/%1$s.k-k.txt", stats.getMaxIteration()));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public double getProbabilityEstim1(int k) {
		int it = stats.getMaxIteration();
		if(it == 0)
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
