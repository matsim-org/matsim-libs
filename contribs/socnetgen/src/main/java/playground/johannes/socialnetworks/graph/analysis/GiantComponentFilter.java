/* *********************************************************************** *
 * project: org.matsim.*
 * GiantComponentFilter.java
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
package playground.johannes.socialnetworks.graph.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import playground.johannes.sna.graph.Edge;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.SparseGraph;
import playground.johannes.sna.graph.SparseGraphBuilder;
import playground.johannes.sna.graph.SparseVertex;
import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.graph.analysis.Components;
import playground.johannes.sna.graph.io.GraphMLWriter;
import playground.johannes.sna.graph.io.SparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class GiantComponentFilter implements GraphFilter<Graph> {

	@Override
	public Graph apply(Graph graph) {
		Components components = Components.getInstance();
		List<Set<Vertex>> comps = components.components(graph);
		
		Set<Vertex> giant = null;
		int size = 0;
		for(Set<Vertex> comp : comps) {
			if(comp.size() > size) {
				size = comp.size();
				giant = comp;
			}
		}
		
		SparseGraphBuilder builder = new SparseGraphBuilder();
		SparseGraph newgraph = builder.createGraph();
		
		Map<Vertex, SparseVertex> mapping = new HashMap<Vertex, SparseVertex>();
		for(Vertex v : giant) {
			SparseVertex newvertex = builder.addVertex(newgraph);
			mapping.put(v, newvertex);
		}
		
		for(Vertex v : giant) {
			SparseVertex v1 = mapping.get(v);
			for(Edge edge : v.getEdges()) {
				SparseVertex v2 = mapping.get(edge.getOpposite(v));
				builder.addEdge(newgraph, v1, v2);
			}
		}
		
		return newgraph;
	}

	public static void main(String args[]) throws IOException {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		SparseGraph graph = reader.readGraph(args[0]);
		
		GiantComponentFilter filter = new GiantComponentFilter();
		Graph newgraph = filter.apply(graph);
		
		GraphMLWriter writer = new GraphMLWriter();
		writer.write(newgraph, args[1]);
	}
}
