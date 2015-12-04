/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphProjection.java
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
package org.matsim.contrib.socnetgen.sna.snowball;

import org.matsim.contrib.socnetgen.sna.graph.Edge;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.GraphProjection;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;

import java.util.Set;

/**
 * Representation of a snowball sampled graph that wraps the original graph and
 * uses it as delegate.
 * 
 * @author illenberger
 * 
 */
public class SampledGraphProjection<G extends Graph, V extends Vertex, E extends Edge> extends GraphProjection<G, V, E>
		implements SampledGraph {

	/**
	 * @see {@link GraphProjection#GraphProjection(Graph)}
	 */
	public SampledGraphProjection(G delegate) {
		super(delegate);
	}

	/**
	 * @see {@link GraphProjection#getEdges()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SampledEdgeDecorator<E>> getEdges() {
		return (Set<? extends SampledEdgeDecorator<E>>) super.getEdges();
	}

	/**
	 * @see {@link GraphProjection#getVertices()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SampledVertexDecorator<V>> getVertices() {
		return (Set<? extends SampledVertexDecorator<V>>) super.getVertices();
	}

	@Override
	public SampledEdgeDecorator<E> getEdge(E e) {
		return (SampledEdgeDecorator<E>) super.getEdge(e);
	}

	@Override
	public SampledVertexDecorator<V> getVertex(V v) {
		return (SampledVertexDecorator<V>) super.getVertex(v);
	}
}
