/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphProjectionFactory.java
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
import org.matsim.contrib.socnetgen.sna.graph.GraphProjectionFactory;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;

/**
 * A sampled graph projection factory is responsible for instantiating new
 * sampled graph projections, vertex decorators and edge decorators. It does not
 * handle the connectivity of vertices and edges.
 * 
 * @author illenberger
 * 
 */
public class SampledGraphProjectionFactory<G extends Graph, V extends Vertex, E extends Edge>
		implements
		GraphProjectionFactory<G, V, E, SampledGraphProjection<G, V, E>, SampledVertexDecorator<V>, SampledEdgeDecorator<E>> {

	/**
	 * Creates and returns an orphaned sampled edge decorator that decorates
	 * <tt>delegate</tt>.
	 * 
	 * @param delegate
	 *            the original edge.
	 * 
	 * @return an orphaned sampled edge decorator.
	 */

	@Override
	public SampledEdgeDecorator<E> createEdge(E delegate) {
		return new SampledEdgeDecorator<E>(delegate);
	}

	/**
	 * Creates and returns an empty sampled graph projection on
	 * <tt>delegate</tt>.
	 * 
	 * @param delegate
	 *            the original graph.
	 * 
	 * @return an empty sampled graph projection.
	 */
	@Override
	public SampledGraphProjection<G, V, E> createGraph(G delegate) {
		return new SampledGraphProjection<G, V, E>(delegate);
	}

	/**
	 * Creates and returns an isolated sampled vertex decorator that decorates
	 * <tt>delegate</tt>.
	 * 
	 * @param delegate
	 *            the original vertex.
	 * 
	 * @return an isolated sampled vertex decorator.
	 */
	@Override
	public SampledVertexDecorator<V> createVertex(V delegate) {
		return new SampledVertexDecorator<V>(delegate);
	}

	@Override
	public SampledGraphProjection<G, V, E> copyGraph(SampledGraphProjection<G, V, E> graph) {
		throw new UnsupportedOperationException("There is currently no need to implement this mehtod.");
	}

	@Override
	public SampledVertexDecorator<V> copyVertex(SampledVertexDecorator<V> vertex) {
		throw new UnsupportedOperationException("There is currently no need to implement this mehtod.");
	}

	@Override
	public SampledEdgeDecorator<E> copyEdge(SampledEdgeDecorator<E> edge) {
		throw new UnsupportedOperationException("There is currently no need to implement this mehtod.");
	}

}
