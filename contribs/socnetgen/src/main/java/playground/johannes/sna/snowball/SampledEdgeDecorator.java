/* *********************************************************************** *
 * project: org.matsim.*
 * SampledEdgeDecorator.java
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
package playground.johannes.sna.snowball;

import org.matsim.core.utils.collections.Tuple;

import playground.johannes.sna.graph.Edge;
import playground.johannes.sna.graph.EdgeDecorator;
import playground.johannes.sna.graph.Vertex;

/**
 * A decorated class that implements SampledEdge.
 * 
 * @author illenberger
 * 
 */
public class SampledEdgeDecorator<E extends Edge> extends EdgeDecorator<E> implements SampledEdge {

	/**
	 * Creates a new orphaned sampled edge decorator.
	 * 
	 * @param delegate
	 *            the original edge.
	 */
	protected SampledEdgeDecorator(E delegate) {
		super(delegate);
	}

	/**
	 * @see {@link EdgeDecorator#getOpposite(Vertex)}
	 */
	@Override
	public SampledVertexDecorator<?> getOpposite(Vertex v) {
		return (SampledVertexDecorator<?>) super.getOpposite(v);
	}

	/**
	 * @see {@link EdgeDecorator#getVertices()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Tuple<? extends SampledVertexDecorator<?>, ? extends SampledVertexDecorator<?>> getVertices() {
		return (Tuple<? extends SampledVertexDecorator<?>, ? extends SampledVertexDecorator<?>>) super.getVertices();
	}

}
