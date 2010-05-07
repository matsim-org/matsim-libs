/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractErdosRenyiGenerator.java
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
package org.matsim.contrib.sna.graph.generators;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.io.GraphMLWriter;

/**
 * Random graph generator that connects vertices with constant probability.
 * 
 * @author illenberger
 * 
 */
public class ErdosRenyiGenerator<G extends Graph, V extends Vertex, E extends Edge> {

	private GraphBuilder<G, V, E> builder;

	/**
	 * Creates a new random graph generatr.
	 * 
	 * @param builder
	 *            a graph builder to create new graphs.
	 */
	public ErdosRenyiGenerator(GraphBuilder<G, V, E> builder) {
		this.builder = builder;
	}

	/**
	 * Creates a new graph with the specified number of vertices that are
	 * connected with constant probability.
	 * 
	 * @param numVertices
	 *            the number of vertices.
	 * @param p
	 *            the edge probability
	 * @param randomSeed
	 *            a random seed
	 * @return a new random graph.
	 */
	public G generate(int numVertices, double p, long randomSeed) {
		G g = builder.createGraph();

		for (int i = 0; i < numVertices; i++)
			builder.addVertex(g);

		return generate(g, p, randomSeed);
	}

	/**
	 * Connects the vertices of an existing graph with constant probability.
	 * 
	 * @param graph
	 *            an existing graph
	 * @param p
	 *            the edge probability
	 * @param randomSeed
	 *            a random seed
	 * @return a graph with randomly inserted edges.
	 */
	@SuppressWarnings("unchecked")
	public G generate(G graph, double p, long randomSeed) {
		LinkedList<V> pending = new LinkedList<V>();
		pending.addAll((Collection<? extends V>) graph.getVertices());

		Random random = new Random(randomSeed);
		V v1;
		while ((v1 = pending.poll()) != null) {
			for (V v2 : pending) {
				if (random.nextDouble() <= p) {
					builder.addEdge(graph, v1, v2);
				}
			}
		}

		return graph;

	}

	/**
	 * Main-method to create random graphs.<br>
	 * Usage: ErdosRenyiGenerator graphMLFile numVertices probability [randomSeed]
	 * 
	 * @param args
	 *            command line arguments.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void main(String args[]) throws FileNotFoundException, IOException {
		int N = Integer.parseInt(args[1]);
		double p = Double.parseDouble(args[2]);
		long seed = (long) (Math.random() * 1000);
		if (args.length > 2)
			seed = Long.parseLong(args[3]);

		ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge>(
				new SparseGraphBuilder());
		Graph g = generator.generate(N, p, seed);

		GraphMLWriter writer = new GraphMLWriter();
		writer.write(g, args[0]);
	}
}
