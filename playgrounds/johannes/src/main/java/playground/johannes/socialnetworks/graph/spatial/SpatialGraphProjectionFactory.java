/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraphProjectionFactory.java
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
package playground.johannes.socialnetworks.graph.spatial;

import org.matsim.contrib.sna.graph.GraphProjectionFactory;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

/**
 * Implementation of GraphProjectionFactory to create instances of SpatialGraphProjection,
 * SpatialVertexDecorator and SpatialEdgeDecorator.
 * 
 * @author illenberger
 *
 */
public class SpatialGraphProjectionFactory<G extends SpatialGraph, V extends SpatialVertex, E extends SpatialEdge> implements
		GraphProjectionFactory<G, V, E, SpatialGraphProjection<G, V, E>, SpatialVertexDecorator<V>, SpatialEdgeDecorator<E>> {

	/**
	 * Creates and returns a spatial edge decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original edge.
	 * 
	 * @return a spatial edge decorator.
	 */
	@Override
	public SpatialEdgeDecorator<E> createEdge(E delegate) {
		return new SpatialEdgeDecorator<E>(delegate);
	}

	/**
	 * Creates and returns an empty spatial graph projection on <tt>delegate</tt>.
	 * 
	 * @param delegate the original graph.
	 * 
	 * @return an empty spatial graph projection.
	 */
	@Override
	public SpatialGraphProjection<G, V, E> createGraph(G delegate) {
		return new SpatialGraphProjection<G, V, E>(delegate);
	}

	/**
	 * Creates and returns a spatial vertex decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original vertex.
	 * 
	 * @return a new spatial vertex decorator.
	 */
	@Override
	public SpatialVertexDecorator<V> createVertex(V delegate) {
		return new SpatialVertexDecorator<V>(delegate);
	}

}
