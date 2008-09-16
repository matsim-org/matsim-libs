/* *********************************************************************** *
 * project: org.matsim.*
 * SparseGraph.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes.snowball2;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * @author illenberger
 *
 */
public class SparseGraphDecorator {

	private Map<SparseVertex, Vertex> vertexMappings = new LinkedHashMap<SparseVertex, Vertex>();
	
	private Map<Vertex, SparseVertex> vertexMappingsReverse = new LinkedHashMap<Vertex, SparseVertex>();
	
	private SparseGraph sparseGraph;
	
	public SparseGraphDecorator(Graph g) {
		sparseGraph = newGraph(g.numVertices(), g.numEdges());
		Set<Vertex> vertices = g.getVertices();
		for(Vertex v: vertices) {
			SparseVertex sv = sparseGraph.newVertex();
			vertexMappings.put(sv, v);
			vertexMappingsReverse.put(v, sv);
			sparseGraph.addVertex(sv);
		}
		
		Set<Edge> edges = g.getEdges();
		for(Edge e : edges) {
			Pair p = e.getEndpoints();
			SparseVertex v1 = vertexMappingsReverse.get(p.getFirst());
			SparseVertex v2 = vertexMappingsReverse.get(p.getSecond());
			sparseGraph.addEdge(v1, v2);
		}
	}
	
	public SparseGraph getSparseGraph() {
		return sparseGraph;
	}
	
	protected SparseGraph newGraph(int numVertex, int numEdges) {
		return new SparseGraph(numVertex, numEdges);
	}
	
	public Set<SparseVertex> getVertices() {
		return vertexMappings.keySet();
	}
	
	public SparseVertex getVertex(Vertex v) {
		return vertexMappingsReverse.get(v);
	}
	
	public Vertex getVertex(SparseVertex v) {
		return vertexMappings.get(v);
	}
}
