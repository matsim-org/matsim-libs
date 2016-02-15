/* *********************************************************************** *
 * project: org.matsim.*
 * GraphProjectionBuilder.java
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

import java.util.HashSet;
import java.util.Set;

/**
 * An GraphBuilder implementation to build graph projections.
 * 
 * @author illenberger
 */
public class GraphProjectionBuilder<G2 extends Graph, V2 extends Vertex, E2 extends Edge, G extends GraphProjection<G2, V2, E2>, V extends VertexDecorator<V2>, E extends EdgeDecorator<E2>>
		extends AbstractSparseGraphBuilder<G, V, E> {

	private GraphProjectionFactory<G2, V2, E2, G, V, E> factory;

	/**
	 * Creates a new graph projection builder.
	 * 
	 * @param factory
	 *            the factory for creating GraphProjection, VertexDecorator and
	 *            EdgeDecorator.
	 */
	public GraphProjectionBuilder(
			GraphProjectionFactory<G2, V2, E2, G, V, E> factory) {
		super(null);
		this.factory = factory;
	}

	@Override
	protected GraphFactory<G, V, E> getFactory() {
		throw new UnsupportedOperationException("Use getProjectionFactory() instead!");
	}

	protected GraphProjectionFactory<G2, V2, E2, G, V, E> getProjectionFactory() {
		return factory;
	}
	
	/**
	 * @throws {@link UnsupportedOperationException}. Use
	 *         {@link #addEdge(GraphProjection, VertexDecorator, VertexDecorator, Edge)
	 *         instead.
	 */
	@Override
	public E addEdge(G graph, V vI, V vJ) {
		throw new UnsupportedOperationException(
				"Cannot create an EdgeDecorator without a delegate.");
	}

	/**
	 * Creates a new edge decorator that decorates <tt>delegate</tt> and inserts
	 * it between <tt>v_i</tt> and <tt>v_j</tt>.
	 * 
	 * @param graph
	 *            the graph a new edge decorator should be inserted in.
	 * @param v_i
	 *            the vertex decorators between the edge should be inserted in.
	 * @param v_j
	 *            the vertex decorators between the edge should be inserted in.
	 * @param delegate
	 *            the original edge.
	 * @return the newly inserted edge decorator, or </tt>null
	 *         <tt> if there exists already an edge between <tt>v_i</tt> and
	 *         <tt>v_j</tt> or if <tt>v_i == v_j</tt>.
	 */
	public E addEdge(G graph, V v_i, V v_j, E2 delegate) {
		E edge = factory.createEdge(delegate);
		if (insertEdge(graph, v_i, v_j, edge)) {
			graph.setMapping(delegate, edge);
			return edge;
		} else
			return null;
	}

	/**
	 * @throws {@link UnsupportedOperationException}. Use
	 *         {@link #addVertex(GraphProjection, Vertex) instead.
	 */
	@Override
	public V addVertex(G graph) {
		throw new UnsupportedOperationException(
				"Cannot create a vertex decorator without a delegate.");
	}

	/**
	 * Creates a new vertex decorator that decorates <tt>delegate</tt> and adds
	 * it to a graph.
	 * 
	 * @param graph
	 *            the graph the new vertex decorator should be inserted in.
	 * @param delegate
	 *            the original vertex.
	 * @return the newly inserted vertex decorator, or <tt>null</tt> is the
	 *         insertion of a vertex would violate the definition of the
	 *         underlying graph.
	 */
	public V addVertex(G graph, V2 delegate) {
		V vertex = factory.createVertex(delegate);
		if (insertVertex(graph, vertex)) {
			graph.setMapping(delegate, vertex);
			return vertex;
		} else
			return null;
	}

	/**
	 * @see {@link AbstractSparseGraphBuilder#removeEdge(SparseGraph, SparseEdge)}
	 */
	@Override
	public boolean removeEdge(G graph, E edge) {
		if (super.removeEdge(graph, edge)) {
			graph.removeMapping(edge.getDelegate());
			return true;
		} else
			return false;
	}

	/**
	 * @see {@link AbstractSparseGraphBuilder#removeVertex(SparseGraph, SparseVertex)}
	 */
	@Override
	public boolean removeVertex(G graph, V vertex) {
		if (super.removeVertex(graph, vertex)) {
			graph.removeMapping(vertex.getDelegate());
			return true;
		} else
			return false;
	}

	/**
	 * @throws {@link UnsupportedOperationException}. Use
	 *         {@link #createGraph(Graph)} instead.
	 */
	@Override
	public G createGraph() {
		throw new UnsupportedOperationException(
				"Cannot create a graph projection without a delegate.");
	}

	/**
	 * Creates a new and empty graph projection on <tt>delegate</tt>.
	 * 
	 * @param delegate
	 *            the original graph.
	 * @return an empty graph projection.
	 */
	public G createGraph(G2 delegate) {
		return factory.createGraph(delegate);
	}

	/**
	 * Creates a new graph projection that completely decorates
	 * <tt>delegate</tt>, i.e. projects all vertices and edges including
	 * connectivity.
	 * 
	 * @param delegate
	 *            the original graph.
	 * @return the new graph projection.
	 */
	@SuppressWarnings("unchecked")
	public G decorateGraph(G2 delegate) {
		G projection = createGraph(delegate);

		for (Vertex v : delegate.getVertices()) {
			addVertex(projection, (V2) v);
		}

		for (Edge e : delegate.getEdges()) {
			V v_i = (V) projection.getVertex((V2) e.getVertices().getFirst());
			V v_j = (V) projection.getVertex((V2) e.getVertices().getSecond());
			this.addEdge(projection, v_i, v_j, (E2) e);
		}

		return projection;
	}

	/**
	 * Synchronizes the projection with its underlying delegate, in that
	 * vertices and edges that have been removed from the delegate will also be
	 * removed from the projection.
	 * 
	 * @param graph a graph projection.
	 */
	@SuppressWarnings("unchecked")
	public void synchronize(G graph) {
		Set<E> edges = new HashSet<E>();
		for (EdgeDecorator<E2> edge : graph.getEdges()) {
			if (!graph.getDelegate().getEdges().contains(edge.getDelegate())) {
				edges.add((E) edge);
			}
		}

		Set<V> vertices = new HashSet<V>();
		for (VertexDecorator<V2> vertex : graph.getVertices()) {
			if (!graph.getDelegate().getVertices().contains(
					vertex.getDelegate())) {
				vertices.add((V) vertex);
			}
		}

		for (E edge : edges) {
			removeEdge(graph, edge);
		}

		for (V vertex : vertices) {
			removeVertex(graph, vertex);
		}
	}
}
