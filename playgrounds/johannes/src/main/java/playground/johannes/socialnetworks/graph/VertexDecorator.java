/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractVertexDecorator.java
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
package playground.johannes.socialnetworks.graph;

import java.util.List;

import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.Vertex;

/**
 * Decorator class for any type of {@link Vertex}.
 * 
 * @author illenberger
 * 
 */
public class VertexDecorator<V extends Vertex> extends SparseVertex {

	private V delegate;

	/**
	 * Creates a new decorator for <tt>delegate</tt>.
	 * 
	 * @param delegate
	 *            the vertex to be decorated.
	 */
	protected VertexDecorator(V delegate) {
		super();
		this.delegate = delegate;
	}

	/**
	 * Returns the decorated vertex.
	 * 
	 * @return the decorated vertex.
	 */
	public V getDelegate() {
		return delegate;
	}

	/**
	 * @see {@link SparseVertex#getEdges()}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends EdgeDecorator<?>> getEdges() {
		return (List<? extends EdgeDecorator<?>>) super.getEdges();
	}

	/**
	 * @see {@link SparseVertex#getNeighbours()}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends VertexDecorator<V>> getNeighbours() {
		return (List<? extends VertexDecorator<V>>) super.getNeighbours();
	}
}
