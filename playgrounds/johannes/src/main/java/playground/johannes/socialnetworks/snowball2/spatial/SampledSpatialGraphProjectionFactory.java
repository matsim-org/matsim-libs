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

import org.matsim.contrib.sna.graph.spatial.SpatialGraphProjectionFactory;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialEdge;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialVertex;


/**
 * Implementation of GraphProjectionFactory to create instances of SampledSpatialGraphProjection,
 * SampledSpatialVertexDecorator and SampledSpatialEdgeDecorator.
 * @author illenberger
 *
 */
public class SampledSpatialGraphProjectionFactory <G extends SampledSpatialGraph, V extends SampledSpatialVertex, E extends SampledSpatialEdge>
							extends SpatialGraphProjectionFactory<G, V, E> {

	/**
	 * Creates and returns a sampled spatial edge decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original edge.
	 * 
	 * @return a sampled spatial edge decorator.
	 */
	@Override
	public SampledSpatialEdgeDecorator<E> createEdge(E delegate) {
		return new SampledSpatialEdgeDecorator<E>(delegate);
	}

	/**
	 * Creates and returns an empty sampled spatial graph projection on <tt>delegate</tt>.
	 * 
	 * @param delegate the original graph.
	 * 
	 * @return an empty sampled spatial graph projection.
	 */
	@Override
	public SampledSpatialGraphProjection<G, V, E> createGraph(G delegate) {
		return new SampledSpatialGraphProjection<G, V, E>(delegate);
	}

	/**
	 * Creates and returns a sampled spatial vertex decorator that decorates <tt>delegate</tt>.
	 * 
	 * @param delegate the original vertex.
	 * 
	 * @return a new sampled spatial vertex decorator.
	 */
	@Override
	public SampledSpatialVertexDecorator<V> createVertex(V delegate) {
		return new SampledSpatialVertexDecorator<V>(delegate);
	}

}
