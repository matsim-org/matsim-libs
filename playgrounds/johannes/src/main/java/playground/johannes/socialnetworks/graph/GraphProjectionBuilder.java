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
package playground.johannes.socialnetworks.graph;

import org.matsim.contrib.sna.graph.AbstractSparseGraphBuilder;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;

/**
 * An GraphBuilder implementation to build graph projections. 
 * 
 * @author illenberger
 */
public class GraphProjectionBuilder<G2 extends Graph, V2 extends Vertex, E2 extends Edge,
									G extends GraphProjection<G2, V2, E2>, V extends VertexDecorator<V2>, E extends EdgeDecorator<E2>>
									extends AbstractSparseGraphBuilder<G, V, E> {

	private GraphProjectionFactory<G2, V2, E2, G, V, E> factory;

	/**
	 * Creates a new graph projection builder.
	 * 
	 * @param factory the factory for creating GraphProjection, VertexDecorator and EdgeDecorator. 
	 */
	public GraphProjectionBuilder(GraphProjectionFactory<G2, V2, E2, G, V, E> factory) {
		super(null);
		this.factory = factory;
	}

	/**
	 * @throws {@link UnsupportedOperationException}. Use {@link #addEdge(GraphProjection, VertexDecorator, VertexDecorator, Edge) instead.
	 */
	@Override
	public E addEdge(G graph, V vI, V vJ) {
		throw new UnsupportedOperationException("Cannot create an EdgeDecorator without a delegate.");
	}
	
	/**
	 * Creates a new edge decorator that decorates <tt>delegate</tt> and inserts it between <tt>v_i</tt> and <tt>v_j</tt>.
	 * 
	 * @param graph the graph a new edge decorator should be inserted in.
	 * @param v_i the vertex decorators between the edge should be inserted in.
	 * @param v_j the vertex decorators between the edge should be inserted in.
	 * @param delegate the original edge.
	 * @return the newly inserted edge decorator, or </tt>null
	 *         <tt> if there exists already an edge between <tt>v_i</tt> and
	 *         <tt>v_j</tt> or if <tt>v_i == v_j</tt>.
	 */
	public E addEdge(G graph, V v_i, V v_j, E2 delegate) {
		E edge = factory.createEdge(delegate);
		if(insertEdge(graph, v_i, v_j, edge))
			return edge;
		else
			return null;
	}

	/**
	 * @throws {@link UnsupportedOperationException}. Use {@link #addVertex(GraphProjection, Vertex) instead.
	 */
	@Override
	public V addVertex(G graph) {
		throw new UnsupportedOperationException("Cannot create a vertex decorator without a delegate.");
	}

	/**
	 * Creates a new vertex decorator that decorates <tt>delegate</tt> and adds it to a graph.
	 * 
	 * @param graph the graph the new vertex decorator should be inserted in.
	 * @param delegate the original vertex.
	 * @return the newly inserted vertex decorator, or <tt>null</tt> is the insertion of a
	 *         vertex would violate the definition of the underlying graph.
	 */
	public V addVertex(G graph, V2 delegate) {
		V vertex = factory.createVertex(delegate);
		if(insertVertex(graph, vertex))
			return vertex;
		else
			return null;
	}
	
	/**
	 * @throws {@link UnsupportedOperationException}. Use {@link #createGraph(Graph)} instead.
	 */
	@Override
	public G createGraph() {
		throw new UnsupportedOperationException("Cannot create a graph projection without a delegate.");
	}
	
	/**
	 * Creates a new and empty graph projection on <tt>delegate</tt>.
	 * 
	 * @param delegate the original graph.
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
		
		for(Vertex v : delegate.getVertices()) {
			V v2 = this.addVertex(projection, (V2) v);
			projection.setMapping((V2)v, v2);
		}
		
		for(Edge e : delegate.getEdges()) {
			V v_i = (V) projection.getVertex((V2) e.getVertices().getFirst());
			V v_j = (V) projection.getVertex((V2) e.getVertices().getSecond());
			this.addEdge(projection, v_i, v_j, (E2) e);
		}
		
		return projection;
	}
}
