/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraphProjection.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.Set;

import org.matsim.contrib.sna.graph.GraphProjection;

/**
 * @author illenberger
 *
 */
public class SpatialGraphProjection<G extends SpatialGraph, V extends SpatialVertex, E extends SpatialEdge> extends GraphProjection<G, V, E> implements
		SpatialGraph {

	public SpatialGraphProjection(G delegate) {
		super(delegate);
	}

	@Override
	public Set<? extends SpatialEdgeDecorator<E>> getEdges() {
		return (Set<? extends SpatialEdgeDecorator<E>>) super.getEdges();
	}

	@Override
	public Set<? extends SpatialVertexDecorator<V>> getVertices() {
		return (Set<? extends SpatialVertexDecorator<V>>) super.getVertices();
	}

}
