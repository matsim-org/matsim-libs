/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractSparseGrahBuilder.java
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
package org.matsim.contrib.socnetgen.sna.graph;

import org.matsim.core.utils.collections.Tuple;

import java.util.HashMap;
import java.util.Map;

/**
 * An abstract GraphBuilder implementation to build graphs that extend from
 * SparseGraph. A SparseGraph is an undirected and unweighted graph that does
 * not allow multiple edges between vertices and does not allow self-loops.
 * 
 * @author illenberger
 * 
 */
public abstract class AbstractSparseGraphBuilder <G extends SparseGraph, V extends SparseVertex, E extends SparseEdge> implements GraphBuilder<G, V, E> {

	private GraphFactory<G, V, E> factory;
	
	/**
	 * Creates a new AbstractSparseGraphBuilder.
	 * 
	 * @param factory a GraphFactory for instantiating new graphs, vertices and edges.
	 */
	public AbstractSparseGraphBuilder(GraphFactory<G, V, E> factory) {
		this.factory = factory;
	}
	
	/**
	 * Returns the GraphFactory used for this GraphBuilder.
	 * @return the GraphFactory used for this GraphBuilder.
	 */
	protected GraphFactory<G, V, E> getFactory() {
		return factory;
	}
	
	/**
	 * Creates and returns an empty graph.
	 * 
	 * @return an empty graph. 
	 */
	public G createGraph() {
		return factory.createGraph();
	}
	
	/**
	 * @see {@link GraphBuilder#addVertex(Graph)}
	 */
	public V addVertex(G graph) {
		V vertex = factory.createVertex();
		if(insertVertex(graph, vertex))
			return vertex;
		else
			return null;
	}

	/**
	 * Inserts a new edge between two vertices.
	 * 
	 * @param graph
	 *            the graph a new edge should be inserted in.
	 * @param v_i
	 *            ,v_j the vertices between the edge should be inserted in.
	 * 
	 * @return the newly inserted edge, or </tt>null
	 *         <tt> if there exists already an edge between <tt>v_i</tt> and
	 *         <tt>v_j</tt> or if <tt>v_i == v_j</tt>.
	 */
	public E addEdge(G graph, V v_i, V v_j) {
		E edge = factory.createEdge();
		if (insertEdge(graph, v_i, v_j, edge))
			return edge;
		else
			return null;
	}

	/**
	 * Inserts <tt>vertex</tt> into <tt>graph</tt>.
	 * 
	 * @param graph
	 *            the graph <tt>vertex</tt> should be inserted in.
	 * @param vertex
	 *            the vertex to insert into <tt>graph</tt>.
	 * @return <tt>true</tt> if the vertex has been inserted into the graph, or
	 *         <tt>false</tt> if the vertex is already part of the graph.
	 */
	protected boolean insertVertex(G graph, V vertex) {
		return graph.insertVertex(vertex);
	}

	/**
	 * Inserts <tt>edge</tt> into <tt>graph</tt> between <tt>v_i</tt> and
	 * <tt>v_j</tt> and assures the correct connectivity between vertices and
	 * edges.
	 * 
	 * @param graph
	 *            the graph where the edge should be inserted in.
	 * @param v_i
	 *            ,v_j the vertices between which the edge should be inserted
	 *            in.
	 * @param edge
	 *            the edge to be inserted into the graph.
	 * @return <tt>true</tt> if the edge has bees inserted into the graph, or
	 *         <tt>false</tt> if there exists already an edge between
	 *         <tt>v_i</tt> and <tt>v_j</tt> or if <tt>v_i == v_j</tt>.
	 */
	protected boolean insertEdge(G graph, V v_i, V v_j, E edge) {
		if (v_i != v_j) {
			if (!v_i.getNeighbours().contains(v_j)) {
				edge.setVertices(new Tuple<SparseVertex, SparseVertex>(v_i,	v_j));
				v_i.addEdge(edge);
				v_j.addEdge(edge);
				return graph.insertEdge(edge);
			} else
				return false;
		} else
			return false;
	}

	/**
	 * @see {@link GraphBuilder#removeVertex(Graph, Vertex)}.
	 */
	public boolean removeVertex(G graph, V vertex) {
		if(vertex.getEdges().isEmpty()) {
			return graph.removeVertex(vertex);
		} else {
			return false;
		}
	}

	/**
	 * Removes an edge from a graph and assures the correct release of
	 * connections between vertices and edges.
	 * 
	 * @param graph
	 *            the graph where the edge should be removed from.
	 * @param edge
	 *            the edge to be removed.
	 * @return <tt>true</tt> if the edge has been removed from the graph, or
	 *         <tt>false</tt> if the edge is not part of the graph or any other
	 *         error occurred.
	 * @throws RuntimeException
	 *             if the removal failed for any reason and the correct graph
	 *             connectivity can not be guaranteed.
	 */
	@SuppressWarnings("unchecked")
	public boolean removeEdge(G graph, E edge) {
		Tuple<SparseVertex, SparseVertex> vertices = (Tuple<SparseVertex, SparseVertex>) edge.getVertices();
		if (vertices == null) {
			return false;
		} else {
			SparseVertex v1 = vertices.getFirst();
			SparseVertex v2 = vertices.getSecond();
			boolean removedv1 = v1.removeEdge(edge);
			boolean removedv2 = v2.removeEdge(edge);
			edge.setVertices(null);
			boolean removed = graph.removeEdge(edge);
			if (removedv1 && removedv2 && removed)
				return true;
			else
				throw new RuntimeException(
						"Failed to remove edge from graph. Consitency no more assured!");
		}
	}

	/*
	 * UNTESTED!
	 */
	@SuppressWarnings("unchecked")
	public G copyGraph(G graph) {
		G newGraph = factory.copyGraph(graph);
		
		Map<SparseVertex, V> vertexMapping = new HashMap<SparseVertex, V>();
		
		for(SparseVertex vertex : graph.getVertices()) {
			V newVertex = factory.copyVertex((V) vertex);
			vertexMapping.put(vertex, newVertex);
			insertVertex(newGraph, newVertex);
		}
		
		for(SparseEdge edge : graph.getEdges()) {
			V vertex1 = vertexMapping.get(edge.getVertices().getFirst());
			V vertex2 = vertexMapping.get(edge.getVertices().getSecond());
			E newEdge = factory.copyEdge((E) edge);
			insertEdge(newGraph, vertex1, vertex2, newEdge);
		}
		
		return newGraph;
	}
}
