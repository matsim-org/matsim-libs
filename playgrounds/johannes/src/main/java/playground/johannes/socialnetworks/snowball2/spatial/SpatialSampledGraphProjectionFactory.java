/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraphProjectionFactory.java
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

import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.snowball.SampledGraphProjectionFactory;



/**
 * Implementation of GraphProjectionFactory to create instances of SpatialSampledGraphProjection,
 * SpatialSampledVertexDecorator and SampledSampledSpatialEdgeDecorator.
 * @author illenberger
 *
 */
public class SpatialSampledGraphProjectionFactory <G extends SpatialGraph, V extends SpatialVertex, E extends SpatialEdge>
							extends SampledGraphProjectionFactory<G, V, E> {

	/**
	 * Creates and returns a spatial sampled edge decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original edge.
	 * 
	 * @return a sampled spatial edge decorator.
	 */
	@Override
	public SpatialSampledEdgeDecorator<E> createEdge(E delegate) {
		return new SpatialSampledEdgeDecorator<E>(delegate);
	}

	/**
	 * Creates and returns an empty spatial sampled graph projection on <tt>delegate</tt>.
	 * 
	 * @param delegate the original graph.
	 * 
	 * @return an empty sampled spatial graph projection.
	 */
	@Override
	public SpatialSampledGraphProjection<G, V, E> createGraph(G delegate) {
		return new SpatialSampledGraphProjection<G, V, E>(delegate);
	}

	/**
	 * Creates and returns a spatial sampled vertex decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original vertex.
	 * 
	 * @return a new sampled spatial vertex decorator.
	 */
	@Override
	public SpatialSampledVertexDecorator<V> createVertex(V delegate) {
		return new SpatialSampledVertexDecorator<V>(delegate);
	}

}
