/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraphProjection.java
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

import java.util.Set;

import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialGraphProjection;
import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialEdge;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialVertex;

/**
 * Extension of {@link SpatialGraphProjection} that implements
 * {@link SampledGraph}.
 * 
 * @see {@link SpatialGraphProjection}
 * @see {@link SampledGraph}
 * @author illenberger
 * 
 */
public class SampledSpatialGraphProjection <G extends SampledSpatialGraph, V extends SampledSpatialVertex, E extends SampledSpatialEdge> extends
		SpatialGraphProjection<G, V, E> implements SampledSpatialGraph {

	/**
	 * Creates an empty spatial sampled graph projection.
	 * 
	 * @param delegate the original graph.
	 */
	public SampledSpatialGraphProjection(G delegate) {
		super(delegate);
	}
	
	/**
	 * @see {@link SpatialGraphProjection#getEdges()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SampledSpatialEdgeDecorator<E>> getEdges() {
		return (Set<? extends SampledSpatialEdgeDecorator<E>>) super.getEdges();
	}

	/**
	 * @see {@link SpatialGraphProjection#getVertices()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SampledSpatialVertexDecorator<V>> getVertices() {
		return (Set<? extends SampledSpatialVertexDecorator<V>>) super.getVertices();
	}

	/**
	 * @see {@link SpatialGraphProjection#getEdge(SparseVertex, SparseVertex)}
	 */
	@Override
	public SampledSpatialEdgeDecorator<E> getEdge(SparseVertex v_i, SparseVertex v_j) {
		return (SampledSpatialEdgeDecorator<E>) super.getEdge(v_i, v_j);
	}

	/**
	 * @see {@link SpatialGraphProjection#getVertex(org.matsim.contrib.sna.graph.spatial.SpatialVertex)}
	 */
	@Override
	public SampledSpatialVertexDecorator<V> getVertex(V v) {
		return (SampledSpatialVertexDecorator<V>) super.getVertex(v);
	}

}
