/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeFilter.java
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.SparseGraph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.io.GraphMLWriter;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class DegreeFilter implements GraphFilter<Graph> {

	private GraphBuilder<Graph, Vertex, Edge> builder;
	
	private int threshold;
	
	private boolean lessthan;

	public DegreeFilter(GraphBuilder<? extends Graph, ? extends Vertex, ? extends Edge> builder, int threshold) {
		this.builder = (GraphBuilder<Graph, Vertex, Edge>) builder;
		this.threshold = threshold;
	}
	
	public void setLessthan(boolean flag) {
		this.lessthan = flag;
	}
	
	@Override
	public Graph apply(Graph graph) {
		Set<Vertex> remove = new HashSet<Vertex>();
		
		for(Vertex vertex : graph.getVertices()) {
			if(lessthan) {
				if(vertex.getNeighbours().size() < threshold)
					remove.add(vertex);
			} else {
				if (vertex.getNeighbours().size() > threshold)
					remove.add(vertex);
			}
		}
		
		for(Vertex vertex : remove) {
			Set<Edge> edges = new HashSet<Edge>(vertex.getEdges());
			for(Edge edge : edges)
				builder.removeEdge(graph, edge);
			
			builder.removeVertex(graph, vertex);
		}
		
		return graph;
	}

	public static void main(String args[]) throws IOException {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		SparseGraph graph = reader.readGraph(args[0]);
		
		DegreeFilter filter = new DegreeFilter(new SparseGraphBuilder(), 3);
		filter.setLessthan(true);
		filter.apply(graph);
		
		GraphMLWriter writer = new GraphMLWriter();
		writer.write(graph, args[1]);
	}
}
