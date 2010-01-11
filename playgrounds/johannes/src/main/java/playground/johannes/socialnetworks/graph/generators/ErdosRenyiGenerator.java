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
package playground.johannes.socialnetworks.graph.generators;

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

import playground.johannes.socialnetworks.graph.Partitions;

/**
 * @author illenberger
 *
 */
public class ErdosRenyiGenerator<G extends Graph, V extends Vertex, E extends Edge> {

	private GraphBuilder<G, V, E> builder;
	
	public ErdosRenyiGenerator(GraphBuilder<G, V, E> factory) {
		this.builder = factory;
	}

	public G generate(int numVertices, double p, long randomSeed) {
		G g = builder.createGraph();
//		LinkedList<V> pending = new LinkedList<V>();
		for (int i = 0; i < numVertices; i++)
			builder.addVertex(g);

		return generate(g, p, randomSeed);
//		Random random = new Random(randomSeed);
//		V v1;
//		while ((v1 = pending.poll()) != null) {
//			for (V v2 : pending) {
//				if (random.nextDouble() <= p) {
//					factory.addEdge(g, v1, v2);
//				}
//			}
//		}
//
//		return g;
	}
	
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
	
	public static void main(String args[]) throws FileNotFoundException, IOException {
		int N = Integer.parseInt(args[1]);
		double p = Double.parseDouble(args[2]);
		long seed = (long)(Math.random() * 1000);
		if(args.length > 3)
			seed = Long.parseLong(args[3]);
		
		ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge>(new SparseGraphBuilder());
		Graph g = generator.generate(N, p, seed);
		
		for(String arg : args) {
			if(arg.equalsIgnoreCase("-e")) {
				g = Partitions.subGraphs(g).first();
				break;
			}
		}
		
		GraphMLWriter writer = new GraphMLWriter();
		writer.write(g, args[0]);
	}
}
