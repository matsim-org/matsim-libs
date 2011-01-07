/* *********************************************************************** *
 * project: org.matsim.*
 * EstimatedTransitivity.java
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

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.Collection;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Transitivity;
import org.matsim.contrib.sna.math.DescriptivePiStatistics;
import org.matsim.contrib.sna.math.DescriptivePiStatisticsFactory;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.PiEstimator;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;


/**
 * @author illenberger
 *
 */
public class EstimatedTransitivity2 extends Transitivity {

	private final PiEstimator biasedDistribution;
	
//	private final PopulationEstimator estimator;
	
	private boolean enableCache;
	
	private static int cachedSize;
	
	private static TObjectDoubleHashMap<SampledVertex> cachedValues;
	
	private DescriptivePiStatisticsFactory facory;
	
	public EstimatedTransitivity2(PiEstimator piEstimator, DescriptivePiStatisticsFactory factory, boolean estimEdges) {
		this.biasedDistribution = piEstimator;
		this.facory = factory;
//		this.estimEdges = estimEdges;
	}	
	public void enableCaching(boolean flag) {
		enableCache = flag;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <V extends Vertex> TObjectDoubleHashMap<V> localClusteringCoefficients(Collection<V> vertices) {
		if(enableCache && vertices.size() == cachedSize) {
			return (TObjectDoubleHashMap<V>) cachedValues;
		}
		
		TObjectDoubleHashMap<SampledVertex> coefficients = new TObjectDoubleHashMap<SampledVertex>();
		Set<SampledVertex> sampledVertices = SnowballPartitions.<SampledVertex>createSampledPartition((Collection<SampledVertex>)vertices);
		/*
		 * calc p_edge
		 */
		int N_edge = 0;
		int n_edge = 0;
		for(SampledVertex vertex : sampledVertices) {
			int n = vertex.getNeighbours().size();
			int n_sampled = 0;
			for(Vertex neighbor : vertex.getNeighbours()) {
				if(((SampledVertex)neighbor).isSampled())
					n_sampled++;
			}
			
			int n_notsampled = n - n_sampled;
			n_edge += countAdjacentEdges(vertex);
			N_edge += n_sampled * n_notsampled + 0.5 * n_sampled * (n_sampled - 1);
		}
		double p_edge = n_edge/(double)N_edge;
		/*
		 * calc clustering coeff
		 */
		for(SampledVertex vertex : sampledVertices) {
			int k = vertex.getEdges().size();
			if (k == 0 || k == 1) {
				coefficients.put(vertex, 0.0);
			} else {
				int n = vertex.getNeighbours().size();
				int n_sampled = 0;
				for(Vertex neighbor : vertex.getNeighbours()) {
					if(((SampledVertex)neighbor).isSampled())
						n_sampled++;
				}
				
				int n_notsampled = n - n_sampled;
				n_edge = countAdjacentEdges(vertex);
//				double N_edge = n_sampled * n_notsampled + 0.5 * n_sampled * (n_sampled - 1);
//				double p_edge = 0;
//				if(N_edge > 0)
//					p_edge = n_edge/N_edge;
//				
				
				double n_edge_estim = n_edge + n_notsampled * (n_notsampled - 1) * 0.5 * p_edge;
				double c = 2 * n_edge_estim / (double) (k * (k - 1));
				
				if(c > 1)
					System.err.println();
				
				coefficients.put(vertex, c);
			}
		}
		
		cachedSize = vertices.size();
		cachedValues = coefficients;
		
		return (TObjectDoubleHashMap<V>) coefficients;
	}

	@Override
	public DescriptivePiStatistics localClusteringDistribution(Set<? extends Vertex> vertices) {
		DescriptivePiStatistics stats = facory.newInstance();
		TObjectDoubleHashMap<? extends Vertex> coefficients = localClusteringCoefficients(vertices);
		TObjectDoubleIterator<? extends Vertex> it = coefficients.iterator();
		for(int i = 0; i < coefficients.size(); i++) {
			it.advance();
			double p = biasedDistribution.probability((SampledVertex) it.key());
			if(p > 0)
				stats.addValue(it.value(), p);
		}
		return stats;
	}

}
