/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialVertexDecorator.java
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

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.snowball.SampledVertexDecorator;


import com.vividsolutions.jts.geom.Point;


/**
 * An extension of {@link SampledVertexDecorator} that implements {@link SpatialVertex}.
 * 
 * @author illenberger
 *
 */
public class SpatialSampledVertexDecorator<V extends SpatialVertex> extends SampledVertexDecorator<V> implements SpatialVertex {

	/**
	 * Creates an isolated spatial sampled vertex decorator that decorates <tt>delegate</tt>. 
	 * @param delegate the original vertex.
	 */
	protected SpatialSampledVertexDecorator(V delegate) {
		super(delegate);
	}

	/**
	 * @see {@link SampledVertexDecorator#getEdges()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SpatialSampledEdgeDecorator<?>> getEdges() {
		return (List<? extends SpatialSampledEdgeDecorator<?>>) super.getEdges();
	}

	/**
	 * @see {@link SampledVertexDecorator#getNeighbours()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SpatialSampledVertexDecorator<V>> getNeighbours() {
		return (List<? extends SpatialSampledVertexDecorator<V>>) super.getNeighbours();
	}

	/**
	 * @see {@link SpatialVertex#getCoordinate()}
	 * @deprecated
	 */
	@Override
	public Coord getCoordinate() {
		return getDelegate().getCoordinate();
	}

	/**
	 * @see {@link SpatialVertex#getPoint()}
	 */
	@Override
	public Point getPoint() {
		return getDelegate().getPoint();
	}

}
