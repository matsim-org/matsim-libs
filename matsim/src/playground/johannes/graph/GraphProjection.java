/* *********************************************************************** *
 * project: org.matsim.*
 * GraphProjection.java
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
package playground.johannes.graph;

import java.util.Set;

/**
 * @author illenberger
 *
 */
public class GraphProjection<G extends Graph, V extends Vertex, E extends Edge> extends SparseGraph {

	private G delegate;
	
	public GraphProjection(G delegate) {
		this.delegate = delegate;
	}
	
	public G getDelegate() {
		return delegate;
	}
	
	@Override
	public EdgeDecorator<E> addEdge(SparseVertex v1, SparseVertex v2) {
		return (EdgeDecorator<E>) super.addEdge(v1, v2);
	}

	public EdgeDecorator<E> addEdge(VertexDecorator<V> v1, VertexDecorator<V> v2, E delegate) {
		EdgeDecorator<E> edge = addEdge(v1, v2);
		edge.setDelegate(delegate);
		return edge;
	}
	
	@Override
	public VertexDecorator<V> addVertex() {
		return (VertexDecorator<V>) super.addVertex();
	}

	public VertexDecorator<V> addVertex(V delegate) {
		VertexDecorator<V> v = addVertex();
		v.setDelegate(delegate);
		return v;
	}
	
	@Override
	public Set<? extends EdgeDecorator<E>> getEdges() {
		return (Set<? extends EdgeDecorator<E>>) super.getEdges();
	}

	@Override
	public Set<? extends VertexDecorator<V>> getVertices() {
		return (Set<? extends VertexDecorator<V>>) super.getVertices();
	}

	@Override
	protected EdgeDecorator<E> newEdge(SparseVertex v1, SparseVertex v2) {
		return new EdgeDecorator<E>((VertexDecorator<V>)v1, (VertexDecorator<V>)v2);
	}

	@Override
	protected VertexDecorator<V> newVertex() {
		return new VertexDecorator<V>();
	}

}
