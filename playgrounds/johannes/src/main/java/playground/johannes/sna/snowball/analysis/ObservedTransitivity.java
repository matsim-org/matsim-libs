/* *********************************************************************** *
 * project: org.matsim.*
 * ObservedTransitivity.java
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
package playground.johannes.sna.snowball.analysis;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.graph.analysis.Transitivity;
import playground.johannes.sna.snowball.SampledGraph;
import playground.johannes.sna.snowball.SampledVertex;

/**
 * A class that provides functionality to analyze triangular configurations in a
 * graph from a snowball sample.
 * 
 * @author illenberger
 * 
 */
public class ObservedTransitivity extends Transitivity {

	private static ObservedTransitivity instance;
	
	public static ObservedTransitivity getInstance() {
		if(instance == null) {
			instance = new ObservedTransitivity();
		}
		return instance;
	}
	
	/**
	 * Calculates the local clustering coefficients for all sampled vertices.
	 * 
	 * @see {@link Transitivity#localClusteringCoefficients(Collection)}
	 * 
	 * @param a
	 *            collection of sampled vertices.
	 * @return a object-double map with all sampled vertices and their local
	 *         clustering coefficient.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <V extends Vertex> TObjectDoubleHashMap<V> localClusteringCoefficients(Collection<V> vertices) {
		return (TObjectDoubleHashMap<V>) super.localClusteringCoefficients(SnowballPartitions
				.<SampledVertex> createSampledPartition((Collection<SampledVertex>) vertices));
	}

	/**
	 * Returns a descriptive statistics object containing the local clustering
	 * coefficient of all sampled vertices that have been sampled up to and
	 * including the next to last iteration.
	 * 
	 * @param vertices
	 *            a set of sampled vertices
	 * @return a descriptive statistics object.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DescriptiveStatistics statistics(Set<? extends Vertex> vertices) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		int iteration = SnowballStatistics.getInstance().lastIteration((Set<? extends SampledVertex>) vertices);
		
		TObjectDoubleHashMap<? extends Vertex> coefficients = localClusteringCoefficients(vertices);
		TObjectDoubleIterator<? extends Vertex> it = coefficients.iterator();
		
		for(int i = 0; i < coefficients.size(); i++) {
			it.advance();
			if(((SampledVertex)it.key()).getIterationSampled() <= iteration - 1) {
				stats.addValue(it.value());
			}
		}
		
		return stats;
	}

	/**
	 * Calculates the global clustering coefficient including only sampled
	 * vertices.
	 * 
	 * @param graph
	 *            a sampled graph
	 * @return the global clustering coefficient.
	 */
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
					
					n_tripples += n_2;
					n_triangles += n_3; 
				}
			}
		}
		 
		return n_triangles/n_tripples;
	}
}
