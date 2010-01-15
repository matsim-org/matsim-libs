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

import org.matsim.contrib.sna.snowball.spatial.SampledSpatialEdge;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialVertex;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraphProjectionFactory;

/**
 * @author illenberger
 *
 */
public class SampledSpatialGraphProjectionFactory <G extends SampledSpatialGraph, V extends SampledSpatialVertex, E extends SampledSpatialEdge>
							extends SpatialGraphProjectionFactory<G, V, E> {

	@Override
	public SampledSpatialEdgeDecorator<E> createEdge(E delegate) {
		return new SampledSpatialEdgeDecorator<E>(delegate);
	}

	@Override
	public SampledSpatialGraphProjection<G, V, E> createGraph(G delegate) {
		return new SampledSpatialGraphProjection<G, V, E>(delegate);
	}

	@Override
	public SampledSpatialVertexDecorator<V> createVertex(V delegate) {
		return new SampledSpatialVertexDecorator<V>(delegate);
	}

}
