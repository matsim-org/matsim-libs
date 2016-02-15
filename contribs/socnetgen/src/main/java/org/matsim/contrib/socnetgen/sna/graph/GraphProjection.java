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
package org.matsim.contrib.socnetgen.sna.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class allows to extract subgraphs from existing graphs by projecting
 * specific vertices and edges onto a new graph. A GraphProjection works similar
 * to a decorator, however, the connectivity of vertices and edges can differ
 * from the original graph.
 * 
 * @author illenberger
 * 
 */
public class GraphProjection<G extends Graph, V extends Vertex, E extends Edge>
		extends SparseGraph {

	private G delegate;

	private Map<V, VertexDecorator<V>> vMapping = new HashMap<V, VertexDecorator<V>>();

	private Map<E, EdgeDecorator<E>> eMapping = new HashMap<E, EdgeDecorator<E>>();

	/**
	 * Creates a new empty projection from graph <tt>delegate</tt>.
	 * 
	 * @param delegate
	 *            the original graph.
	 */
	public GraphProjection(G delegate) {
		this.delegate = delegate;
	}

	/**
	 * Returns the original graph.
	 * 
	 * @return the original graph.
	 */
	public G getDelegate() {
		return delegate;
	}

	/**
	 * Returns the decorator representing the projection of vertex <tt>v</tt>.
	 * 
	 * @param v
	 *            the original vertex.
	 * @return the decorater of vertex <tt>v</tt>, or <tt>null</tt> if there is
	 *         no projection for <tt>v</tt>.
	 */
	public VertexDecorator<V> getVertex(V v) {
		return vMapping.get(v);
	}


	/**
	 * Returns the decorator representing the projection of edge <tt>e</tt>.
	 * 
	 * @param v
	 *            the original edge.
	 * @return the decorater of edge <tt>e</tt>, or <tt>null</tt> if there is
	 *         no projection for <tt>e</tt>.
	 */
	public EdgeDecorator<E> getEdge(E e) {
		return eMapping.get(e);
	}

	/**
	 * Associates vertex <tt>delegate</tt> with its decorator <tt>decorator</tt>
	 * .
	 * 
	 * @param delegate
	 *            the original vertex.
	 * @param decorator
	 *            the decorator of the original vertex.
	 */
	void setMapping(V delegate, VertexDecorator<V> decorator) {
		vMapping.put(delegate, decorator);
	}

	/**
	 * Removes the mapping for <tt>delegate</tt>.
	 * 
	 * @param delegate
	 *            the original vertex.
	 */
	void removeMapping(V delegate) {
		vMapping.remove(delegate);
	}

	/**
	 * Associates edge <tt>delegate</tt> with its decorator <tt>decorator</tt>.
	 * 
	 * @param delegate
	 *            the original edge.
	 * @param decorator
	 *            the decorator of the original edge.
	 */
	void setMapping(E delegate, EdgeDecorator<E> decorator) {
		eMapping.put(delegate, decorator);
	}

	/**
	 * Removes the mapping for <tt>delegate</tt>.
	 * 
	 * @param delegate
	 *            the original edge.
	 */
	void removeMapping(E delegate) {
		eMapping.remove(delegate);
	}

	/**
	 * @see {@link Graph#getEdges()}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends EdgeDecorator<E>> getEdges() {
		return (Set<? extends EdgeDecorator<E>>) super.getEdges();
	}

	/**
	 * @see {@link Graph#getVertices()}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends VertexDecorator<V>> getVertices() {
		return (Set<? extends VertexDecorator<V>>) super.getVertices();
	}

	/**
	 * @see SparseGraph#getEdge(SparseVertex,
	 *      SparseVertex)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public EdgeDecorator<E> getEdge(SparseVertex v_i, SparseVertex v_j) {
		return (EdgeDecorator<E>) super.getEdge(v_i, v_j);
	}
}
