/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialEdgeDecorator.java
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
package playground.johannes.socialnetworks.snowball2.spatial;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.snowball2.SampledEdgeDecorator;


/**
 * An extension to {@link SampledEdgeDecorator} that implements {@link SpatialEdge}.
 * 
 * @author illenberger
 *
 */
public class SpatialSampledEdgeDecorator<E extends SpatialEdge> extends SampledEdgeDecorator<E>
		implements SpatialEdge {

	/**
	 * Creates an orphaned edge decorator which decorates <tt>delegate</tt>.
	 * 
	 * @param delegate
	 */
	protected SpatialSampledEdgeDecorator(E delegate) {
		super(delegate);
	}

	/**
	 * @see {@link SampledEdgeDecorator#getOpposite(Vertex)}
	 */
	@Override
	public SpatialSampledVertexDecorator<?> getOpposite(Vertex v) {
		return (SpatialSampledVertexDecorator<?>) super.getOpposite(v);
	}

	/**
	 * @see {@link SampledEdgeDecorator#getVertices()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Tuple<? extends SpatialSampledVertexDecorator<?>, ? extends SpatialSampledVertexDecorator<?>> getVertices() {
		return (Tuple<? extends SpatialSampledVertexDecorator<?>, ? extends SpatialSampledVertexDecorator<?>>) super.getVertices();
	}

	/**
	 * @see {@link SpatialEdge#length()}
	 */
	@Override
	public double length() {
		// TODO Auto-generated method stub
		return getDelegate().length();
	}

}
