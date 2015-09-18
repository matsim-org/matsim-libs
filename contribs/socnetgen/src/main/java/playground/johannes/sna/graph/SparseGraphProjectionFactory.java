/* *********************************************************************** *
 * project: org.matsim.*
 * GraphProjectionFactory.java
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
package playground.johannes.sna.graph;

/**
 * Implementation of GraphProjectionFactory to create instances of GraphProjection,
 * VertexDecorator and EdgeDecorator.
 * 
 * @author illenberger
 *
 */
public class SparseGraphProjectionFactory<G extends Graph, V extends Vertex, E extends Edge> implements GraphProjectionFactory<G, V, E, GraphProjection<G,V,E>, VertexDecorator<V>, EdgeDecorator<E>>{

	/**
	 * Creates and returns an edge decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original edge.
	 * 
	 * @return an edge decorator.
	 */
	@Override
	public EdgeDecorator<E> createEdge(E delegate) {
		return new EdgeDecorator<E>(delegate);
	}

	/**
	 * Creates and returns an empty graph projection on <tt>delegate</tt>.
	 * 
	 * @param delegate the original graph.
	 * 
	 * @return an empty graph projection.
	 */
	@Override
	public GraphProjection<G, V, E> createGraph(G delegate) {
		return new GraphProjection<G, V, E>(delegate);
	}

	/**
	 * Creates and returns a vertex decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original vertex.
	 * 
	 * @return a new vertex decorator.
	 */
	@Override
	public VertexDecorator<V> createVertex(V delegate) {
		return new VertexDecorator<V>(delegate);
	}

	@Override
	public GraphProjection<G, V, E> copyGraph(GraphProjection<G, V, E> graph) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

	@Override
	public VertexDecorator<V> copyVertex(VertexDecorator<V> vertex) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

	@Override
	public EdgeDecorator<E> copyEdge(EdgeDecorator<E> edge) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

}
