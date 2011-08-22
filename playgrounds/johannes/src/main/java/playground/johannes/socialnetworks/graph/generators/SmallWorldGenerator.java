/* *********************************************************************** *
 * project: org.matsim.*
 * SmallWorldGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.generators;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.sna.graph.io.GraphMLWriter;

import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.ExtendedTopologyAnalyzerTask;
import playground.johannes.socialnetworks.graph.analysis.TopologyAnalyzerTask;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author illenberger
 *
 */
public class SmallWorldGenerator<G extends Graph, V extends Vertex, E extends Edge> {

	private static final Logger logger = Logger.getLogger(SmallWorldGenerator.class);
	
	private final GraphBuilder<G, V, E> builder;
	
	private final RingLatticeGenerator<G, V, E> ringLatticeGenerator;
	
	private final Random random;
	
	public SmallWorldGenerator(GraphBuilder<G, V, E> builder) {
		this.builder = builder;
		this.random = new XORShiftRandom();
		this.ringLatticeGenerator = new RingLatticeGenerator(builder);
	}
	
	public SmallWorldGenerator(GraphBuilder<G, V, E> builder, Random random) {
		this.builder = builder;
		this.random = random;
		this.ringLatticeGenerator = new RingLatticeGenerator(builder);
	}
	
	public G generate(int N, int k, double p) {
		G graph = ringLatticeGenerator.generate(N, k);
		
		Set<Edge> edges = new HashSet<Edge>();
		for(Edge edge : graph.getEdges()) {
			if(random.nextDouble() < p) {
				edges.add(edge);
			}
		}
		
		List<V> vertices = new ArrayList<V>(graph.getVertices().size());
		vertices.addAll((Collection<? extends V>) graph.getVertices());
		
		int doubleEdges = 0;
		
		for(Edge edge : edges) {
			V source;
			
			if(random.nextBoolean()) {
				source = (V) edge.getVertices().getFirst();
			} else {
				source = (V) edge.getVertices().getSecond();
			}
			
			builder.removeEdge(graph, (E) edge);
			V target = null;
			while(target == null) {
				target = vertices.get(random.nextInt(vertices.size()));
				if(source == target) 
					target = null;
			}
			
			if(builder.addEdge(graph, source, target) == null)
					doubleEdges++;
			
		}
		
	
		logger.debug(String.format("Rejected %1$s double edges.", doubleEdges));
		
		return graph;
	}
	
	public static void main(String args[]) throws IOException {
		SmallWorldGenerator<SparseGraph, SparseVertex, SparseEdge> generator = new SmallWorldGenerator<SparseGraph, SparseVertex, SparseEdge>(new SparseGraphBuilder());
		SparseGraph graph = generator.generate(36000, 5, 0.2);
		
		GraphMLWriter writer = new GraphMLWriter();
		writer.write(graph, "/Users/jillenberger/Work/socialnets/data/graphs/smallworld/graph.graphml");
		
//		TopologyAnalyzerTask task = new TopologyAnalyzerTask();
//		ExtendedTopologyAnalyzerTask exTask = new ExtendedTopologyAnalyzerTask();
//		
//		AnalyzerTaskComposite composite = new AnalyzerTaskComposite();
//		composite.addTask(task);
//		composite.addTask(exTask);
//		
//		GraphAnalyzer.analyze(graph, composite, "/Users/jillenberger/Work/socialnets/data/graphs/smallworld/");
	}
}
