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
package playground.johannes.socialnetworks.snowball2.analysis;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.Collection;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Transitivity;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;
import org.matsim.contrib.sna.snowball.sim.ProbabilityEstimator;

import playground.johannes.socialnetworks.snowball2.sim.deprecated.PopulationEstimator;
import playground.johannes.socialnetworks.statistics.EstimatedDistribution;

/**
 * @author illenberger
 *
 */
public class EstimatedTransitivity extends Transitivity {

	private final ProbabilityEstimator biasedDistribution;
	
	private final PopulationEstimator estimator;
	
	private boolean enableCache;
	
	private static int cachedSize;
	
	private static TObjectDoubleHashMap<SampledVertex> cachedValues;
	
	public EstimatedTransitivity(ProbabilityEstimator biasedDistr, PopulationEstimator estimator) {
		this.biasedDistribution = biasedDistr;
		this.estimator = estimator;
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
				int n_edge = countAdjacentEdges(vertex);
				double N_edge = n_sampled * n_notsampled + 0.5 * n_sampled * (n_sampled - 1);
				double p_edge = 0;
				if(N_edge > 0)
					p_edge = n_edge/N_edge;
				
				double n_edge_estim = n_edge + n_notsampled * (n_notsampled - 1) * 0.5 * p_edge;
				double c = 2 * n_edge_estim / (double) (k * (k - 1));
				
				coefficients.put(vertex, c);
			}
		}
		
		cachedSize = vertices.size();
		cachedValues = coefficients;
		
		return (TObjectDoubleHashMap<V>) coefficients;
	}

	@Override
	public Distribution localClusteringDistribution(Set<? extends Vertex> vertices) {
		Distribution distr = new EstimatedDistribution(estimator);
		TObjectDoubleHashMap<? extends Vertex> coefficients = localClusteringCoefficients(vertices);
		TObjectDoubleIterator<? extends Vertex> it = coefficients.iterator();
		for(int i = 0; i < coefficients.size(); i++) {
			it.advance();
			double p = biasedDistribution.getProbability((SampledVertex) it.key());
			if(p > 0)
				distr.add(it.value(), 1/p);
		}
		return distr;
	}

	@Override
	public double globalClusteringCoefficient(Graph graph) {
		return 0.0;
	}

}
