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
package org.matsim.contrib.socnetgen.sna.snowball.analysis;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;
import org.matsim.contrib.common.stats.DescriptivePiStatistics;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.Transitivity;
import org.matsim.contrib.socnetgen.sna.math.DescriptivePiStatisticsFactory;
import org.matsim.contrib.socnetgen.sna.snowball.SampledGraph;
import org.matsim.contrib.socnetgen.sna.snowball.SampledVertex;

import java.util.Collection;
import java.util.Set;


/**
 * A class that provides functionality to estimate triangular configurations in a
 * graph.
 * 
 * @author illenberger
 *
 */
public class EstimatedTransitivity extends Transitivity {

	private final PiEstimator piEstimator;

	private DescriptivePiStatisticsFactory facory;
	
	private boolean estimEdges;
	
	private boolean enableCache;
	
	private static int cachedSize;
	
	private static TObjectDoubleHashMap<SampledVertex> cachedValues;
	
	/**
	 * Creates a new estimated transitivity object with <tt>piEstimator</tt> as
	 * the pi-estimator and <tt>factor</tt> as the factor for creating new
	 * {@link DescriptivePiStatistics} object that do the actual estimation of
	 * properties. If the switch <tt>estimEdges</tt> is set to <tt>true</tt>
	 * missing edges between neighbours are also estimated.
	 * 
	 * @param piEstimator
	 *            the pi-estimator
	 * @param factory
	 *            a factory for creating new instance of
	 *            {@link DescriptivePiStatistics}
	 * @param estimEdges
	 *            <tt>true</tt> if missing edges between neighbours are to be
	 *            estimated.
	 */
	public EstimatedTransitivity(PiEstimator piEstimator, DescriptivePiStatisticsFactory factory, boolean estimEdges) {
		this.piEstimator = piEstimator;
		this.facory = factory;
		this.estimEdges = estimEdges;
	}

	/**
	 * Enables caching functionality. If
	 * {@link #localClusteringCoefficients(Collection)} is called multiple times
	 * with a set of vertices of same size the calculation is done only once.
	 * 
	 * @param flag <tt>true</tt> to enable caching.
	 */
	@Deprecated
	public void enableCaching(boolean flag) {
		enableCache = flag;
	}

	/**
	 * Estimates the local clustering coefficient for all sampled vertices.
	 * 
	 * @param vertices
	 *            a set of sampled vertices
	 * @return a object-double map with all sampled vertices and their estimated
	 *         local clustering coefficients.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <V extends Vertex> TObjectDoubleHashMap<V> localClusteringCoefficients(Collection<V> vertices) {
		if(enableCache && vertices.size() == cachedSize) {
			return (TObjectDoubleHashMap<V>) cachedValues;
		}
		
		Set<SampledVertex> sampledVertices = SnowballPartitions.<SampledVertex>createSampledPartition((Collection<SampledVertex>)vertices);
		
		TObjectDoubleHashMap<SampledVertex> coefficients = new TObjectDoubleHashMap<SampledVertex>();
		
		for(SampledVertex vertex : sampledVertices) {
			int k = vertex.getEdges().size();
			if (k == 0 || k == 1) {
				coefficients.put(vertex, 0.0);
			} else {
				double c = 0;
				int n_edge = countAdjacentEdges(vertex);
				
				if (estimEdges) {
					c = 2 * estimateAdjacentEdges(vertex, n_edge) / (double) (k * (k - 1));
				} else {
					c = 2 * n_edge / (double) (k * (k - 1));
				}
				
				coefficients.put(vertex, c);
			}
		}
		
		cachedSize = vertices.size();
		cachedValues = coefficients;
		
		return (TObjectDoubleHashMap<V>) coefficients;
	}

	protected double estimateAdjacentEdges(SampledVertex vertex, int n_edge) {
		int n = vertex.getNeighbours().size();
		int n_sampled = 0;
		for (Vertex neighbor : vertex.getNeighbours()) {
			if (((SampledVertex) neighbor).isSampled())
				n_sampled++;
		}

		int n_notsampled = n - n_sampled;

		double N_edge = n_sampled * n_notsampled + 0.5 * n_sampled * (n_sampled - 1);
		double p_edge = 0;
	
		if (N_edge > 0)
			p_edge = n_edge / N_edge;

		double n_edge_estim = n_edge + n_notsampled * (n_notsampled - 1) * 0.5 * p_edge;
		
		return n_edge_estim;
	}
	
	/**
	 * Returns a descriptive statistics object containing the estimated local
	 * clustering coefficients of vertices sampled in or up to the next to last
	 * iteration each associated a pi-value.
	 * 
	 * @param vertices a set of sampled vertices
	 * @return a descriptive statistics object.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DescriptivePiStatistics statistics(Set<? extends Vertex> vertices) {
		DescriptivePiStatistics stats = facory.newInstance();
		int iteration = SnowballStatistics.getInstance().lastIteration((Set<? extends SampledVertex>) vertices);
		
		TObjectDoubleHashMap<? extends Vertex> values = localClusteringCoefficients(vertices);
		TObjectDoubleIterator<? extends Vertex> it = values.iterator();
		
		for (int i = 0; i < values.size(); i++) {
			it.advance();
			if (((SampledVertex) it.key()).getIterationSampled() <= iteration - 1) {
				double p = piEstimator.probability((SampledVertex) it.key(), iteration - 1);
				if (p > 0)
					stats.addValue(it.value(), p);
			}
		}
		return stats;
	}

	@Override
	public double globalClusteringCoefficient(Graph graph) {
		double n_tripples = 0;
		double n_triangles = 0;

		SampledGraph sampledGraph = (SampledGraph) graph;
		int iteration = SnowballStatistics.getInstance().lastIteration(sampledGraph.getVertices());
		
		for(SampledVertex v : sampledGraph.getVertices()) {
			if(v.isSampled() && v.getIterationSampled() < iteration) {
				int k = v.getNeighbours().size();
				if(k > 1) {
					int n_2 = k*(k-1)/2;
					double n_3 = countAdjacentEdges(v);
					if(estimEdges)
						n_3 = estimateAdjacentEdges(v, (int)n_3);
					
					double p = piEstimator.probability(v, iteration - 1);
					
					n_tripples += n_2 * 1/p;
					n_triangles += n_3 * 1/p; 
				}
				 
			}
		}
		 
		return n_triangles/n_tripples;
	}
	
}
