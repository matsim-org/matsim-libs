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
package playground.johannes.socialnetworks.snowball2.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.Transitivity;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;

/**
 * @author illenberger
 * 
 */
public class ObservedTransitivity extends Transitivity {

	@SuppressWarnings("unchecked")
	@Override
	public <V extends Vertex> TObjectDoubleHashMap<V> localClusteringCoefficients(Collection<V> vertices) {
		return (TObjectDoubleHashMap<V>) super.localClusteringCoefficients(SnowballPartitions
				.<SampledVertex> createSampledPartition((Collection<SampledVertex>) vertices));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Distribution localClusteringDistribution(Set<? extends Vertex> vertices) {
		return super.localClusteringDistribution(SnowballPartitions
				.<SampledVertex> createSampledPartition((Collection<SampledVertex>) vertices));
	}

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
