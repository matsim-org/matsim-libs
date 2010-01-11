/* *********************************************************************** *
 * project: org.matsim.*
 * Triangles.java
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
package playground.johannes.socialnetworks.graph.analysis;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.math.Distribution;


/**
 * @author illenberger
 *
 */
public class Transitivity {

	/**
	 * Calculates the distribution of local clustering coefficients.
	 * 
	 * @param vertices
	 *            a set of vertices.
	 *            
	 * @return the distribution of local clustering coefficients.
	 */
	public Distribution localClusteringDistribution(Set<? extends Vertex> vertices) {
		Distribution stats = new Distribution();
		stats.addAll(localClusteringCoefficients(vertices).getValues());
		return stats;
	}
	
	/**
	 * Calculates the local clustering coefficient of each vertex in
	 * <tt>vertices</tt>. The local clustering coefficient is defined as:
	 * <ul>
	 * <li>C = 0 if k = 0 or k = 1,
	 * <li>C = 2y/k*(k-1) if k > 1, where y is the number of edges connecting
	 * neighbors of the vertex.
	 * </ul>
	 * 
	 * @param vertices
	 *            a collection of vertices the local clustering coefficients are
	 *            to be calculated.
	 * @return an object-double map containing the vertex as key and the
	 *         clustering coefficient as value.
	 */
	public TObjectDoubleHashMap<? extends Vertex> localClusteringCoefficients(Collection<? extends Vertex> vertices) {
		TObjectDoubleHashMap<Vertex> cc = new TObjectDoubleHashMap<Vertex>();
		for(Vertex v : vertices) {
			int k = v.getEdges().size();
			if(k == 0 || k == 1) {
				cc.put(v, 0.0);
			} else {
				int edgecount = 0;
				Set<Vertex> n1s = new HashSet<Vertex>(v.getNeighbours());
				int n_Neighbours1 = v.getNeighbours().size();
				for(int i1 = 0; i1 < n_Neighbours1; i1++) {
					Vertex n1 = v.getNeighbours().get(i1);
					int n_Neighbours2 = n1.getNeighbours().size();
					for(int i2 = 0; i2 < n_Neighbours2; i2++) {
						Vertex n2 = n1.getNeighbours().get(i2);
						if (n2 != v) {
							if (n1s.contains(n2))
								edgecount++;
						}
					}
					n1s.remove(n1);
				}
				cc.put(v, 2 * edgecount / (double)(k*(k-1)));
			}
		}
		
		return cc;
	}
	
	/**
	 * Calculates the global clustering coefficient, which is defined as the
	 * three times the number of triangles over the number of connected
	 * tripples.
	 * 
	 * @param graph
	 *            the graph the global clustering coefficient is calculated for.
	 * @return the global clustering coefficient.
	 */
	public double globalClusteringCoefficient(Graph graph) {
		int n_tripples = 0;
		int n_triangles = 0;
		for(Vertex v : graph.getVertices()) {
			List<? extends Vertex> n1s = v.getNeighbours();
			for(int i = 0; i < n1s.size(); i++) {
				List<? extends Vertex> n2s = n1s.get(i).getNeighbours();
				for(int k = 0; k < n2s.size(); k++) {
					if (!n2s.get(k).equals(v)) {
						n_tripples++;
						if (n2s.get(k).getNeighbours().contains(v))
							n_triangles++;
					}
				}
			}
		}
		
		return n_triangles / (double)n_tripples;
	}
}
