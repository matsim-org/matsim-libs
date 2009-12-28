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
package playground.johannes.socialnetworks.graph.generators;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.Vertex;

import playground.johannes.socialnetworks.graph.io.GraphMLWriter;


/**
 * @author illenberger
 *
 */
public class BarabasiAlbertGenerator<G extends Graph, V extends Vertex, E extends Edge> {
	
	public static final Logger logger = Logger.getLogger(BarabasiAlbertGenerator.class);

	private GraphBuilder<G, V, E> builder;
	
	public BarabasiAlbertGenerator(GraphBuilder<G, V, E> factory) {
		this.builder = factory;
	}
	
	public Graph generate(int m_0, int m, int t, long randomSeed) {
		if(m_0 < 1)
			throw new IllegalArgumentException("Number of initial nodes (m_0) must be m_0 >= 2!");
		if(m > m_0)
			throw new IllegalArgumentException("Number of edges to attach per time step (m) must not be greater the number of initial nodes (m_0)!");
		
		Random random = new Random(randomSeed);
		/*
		 * Initialize graph.
		 */
		G g = builder.createGraph();
		List<V> vertices = new ArrayList<V>(m_0 + (m*t));
		V previous = null;
		for(int i = 0; i < m_0; i++) {
			V v = builder.addVertex(g);
			if(v == null)
				throw new RuntimeException("Vertex must nor be null!");
			
			vertices.add(v);
			
			if(previous != null) {
				E e = builder.addEdge(g, v, previous);
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
			V v = builder.addVertex(g);
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
				builder.addEdge(g, v, targets.get(k));
		}
		
		return g;
	}
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
		int m_0 = Integer.parseInt(args[1]);
		int m = Integer.parseInt(args[2]);
		int t = Integer.parseInt(args[3]);
		long seed = (long)(Math.random() * 1000);
		if(args.length > 4)
			seed = Long.parseLong(args[4]);
		
		BarabasiAlbertGenerator<SparseGraph, SparseVertex, SparseEdge> generator = new BarabasiAlbertGenerator<SparseGraph, SparseVertex, SparseEdge>(new SparseGraphBuilder());
		Graph g = generator.generate(m_0, m, t, seed);
		GraphMLWriter writer = new GraphMLWriter();
		writer.write(g, args[0]);
	}
}
