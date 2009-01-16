/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractBarabasiAlbertGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.graph.generators;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import playground.johannes.graph.Edge;
import playground.johannes.graph.Graph;
import playground.johannes.graph.GraphStatistics;
import playground.johannes.graph.PlainGraph;
import playground.johannes.graph.SparseEdge;
import playground.johannes.graph.SparseVertex;
import playground.johannes.graph.Vertex;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class BarabasiAlbertGenerator<G extends Graph, V extends Vertex, E extends Edge> {

	private GraphFactory<G, V, E> factory;
	
	public BarabasiAlbertGenerator(GraphFactory<G, V, E> factory) {
		this.factory = factory;
	}
	
	public Graph genertate(int m_0, int m, int t, long randomSeed) {
		if(m_0 < 1)
			throw new IllegalArgumentException("Number of initial nodes (m_0) must be m_0 >= 2!");
		if(m > m_0)
			throw new IllegalArgumentException("Number of edges to attach per time step (m) must not be greater the number of initial nodes (m_0)!");
		
		Random random = new Random();
		/*
		 * Initialize graph.
		 */
		G g = factory.createGraph();
		List<V> vertices = new ArrayList<V>(m_0 + (m*t));
		V previous = null;
		for(int i = 0; i < m_0; i++) {
			V v = factory.addVertex(g);
			if(v == null)
				throw new RuntimeException("Vertex must nor be null!");
			
			vertices.add(v);
			
			if(previous != null) {
				E e = factory.addEdge(g, v, previous);
				if(e == null)
					throw new RuntimeException("Edge must not be null!");
			}
			previous = v;
		}
		/*
		 * Evolve graph.
		 */
		for(int i = 0; i < t; i++) {
			int sum_k = g.getEdges().size();
			/*
			 * Insert a new vertex.
			 */
			V v = factory.addVertex(g);
			/*
			 * Connect the new vertex to m existing vertices.
			 */
			List<V> targets = new ArrayList<V>(m);
			for(int k = 0; k < m; k++) {
				V target = null;
				while(target == null) {
					/*
					 * Draw a random vertex.
					 */
					target = vertices.get(random.nextInt(vertices.size()));
					/*
					 * Calculate attach probability.
					 */
					double p = target.getEdges().size()/(double)sum_k;
					if(random.nextDouble() <= p && !targets.contains(target)) {
						targets.add(target);
						break;
					} else
						/*
						 * Dismiss vertex.
						 */
						target = null;
				}
			}
			vertices.add(v);
			/*
			 * Insert edges.
			 */
			int count = targets.size();
			for(int k = 0; k < count; k++)
				factory.addEdge(g, v, targets.get(k));
		}
		
		return g;
	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
		BarabasiAlbertGenerator<PlainGraph, SparseVertex, SparseEdge> generator = new BarabasiAlbertGenerator<PlainGraph, SparseVertex, SparseEdge>(new PlainGraphFactory());
		Graph g = generator.genertate(10, 3, 1000, 0);
		WeightedStatistics stats = GraphStatistics.getDegreeDistribution(g);
		System.out.println("Graph has " + g.getVertices().size() + " vertices, " + g.getEdges().size() + " edges. <k> = " + GraphStatistics.getDegreeStatistics(g).getMean());
		WeightedStatistics.writeHistogram(stats.absoluteDistribution(), "/Users/fearonni/Desktop/hist.txt");
	}
}
