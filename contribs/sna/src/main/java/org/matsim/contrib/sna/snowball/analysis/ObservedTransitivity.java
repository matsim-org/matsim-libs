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
package org.matsim.contrib.sna.snowball.analysis;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Transitivity;
import org.matsim.contrib.sna.snowball.SampledVertex;

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
	public DescriptiveStatistics localClusteringDistribution(Set<? extends Vertex> vertices) {
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
		int n_tripples = 0;
		int n_triangles = 0;
		for (Vertex v : graph.getVertices()) {
			
			if (((SampledVertex) v).isSampled()) {
				List<? extends Vertex> n1s = v.getNeighbours();

				for (int i = 0; i < n1s.size(); i++) {
					SampledVertex n1 = (SampledVertex) n1s.get(i);

					if (n1.isSampled()) {
						List<? extends Vertex> n2s = n1.getNeighbours();

						for (int k = 0; k < n2s.size(); k++) {
							SampledVertex n2 = (SampledVertex) n2s.get(k);

							if (n2.isSampled() && !n2.equals(v)) {
								n_tripples++;
								if (n2.getNeighbours().contains(v))
									n_triangles++;
							}
						}
					}
				}
			}
		}

		return n_triangles / (double) n_tripples;
	}
}
