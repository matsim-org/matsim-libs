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
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Use a SpatialGraphProjection to get a spatial clipping of a spatial graph.
 * The clipping is a {@link GraphProjection} on the original graph and
 * implements {@link SpatialGraph}.
 * 
 * @author illenberger
 * 
 */
public class SpatialGraphProjection<G extends SpatialGraph, V extends SpatialVertex, E extends SpatialEdge>
		extends GraphProjection<G, V, E> implements SpatialGraph {

	/**
	 * Creates an empty spatial graph projection.
	 * 
	 * @param delegate
	 *            the original graph.
	 */
	public SpatialGraphProjection(G delegate) {
		super(delegate);
	}

	/**
	 * @see {@link GraphProjection#getEdges()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialEdgeDecorator<E>> getEdges() {
		return (Set<? extends SpatialEdgeDecorator<E>>) super.getEdges();
	}

	/**
	 * @see {@link GraphProjection#getVertices()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialVertexDecorator<V>> getVertices() {
		return (Set<? extends SpatialVertexDecorator<V>>) super.getVertices();
	}

	/**
	 * @see {@link GraphProjection#getEdge(SparseVertex, SparseVertex)}
	 */
	@Override
	public SpatialEdgeDecorator<E> getEdge(SparseVertex v_i, SparseVertex v_j) {
		return (SpatialEdgeDecorator<E>) super.getEdge(v_i, v_j);
	}

	/**
	 * @see {@link GraphProjection#getVertex(org.matsim.contrib.sna.graph.Vertex)}
	 */
	@Override
	public SpatialVertexDecorator<V> getVertex(V v) {
		return (SpatialVertexDecorator<V>) super.getVertex(v);
	}

	/**
	 * @see {@link SpatialGraph#getCoordinateReferenceSysten()}
	 */
	@Override
	public CoordinateReferenceSystem getCoordinateReferenceSysten() {
		return getDelegate().getCoordinateReferenceSysten();
	}
}
