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
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.snowball.SampledGraphProjection;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Extension of {@link SampledGraphProjection} that implements
 * {@link SpatialGraph}.
 * 
 * @see {@link SampledGraphProjection}
 * @see {@link SpatialGraph}
 * @author illenberger
 * 
 */
public class SpatialSampledGraphProjection <G extends SpatialGraph, V extends SpatialVertex, E extends SpatialEdge> extends
		SampledGraphProjection<G, V, E> implements SpatialGraph {

	/**
	 * Creates an empty spatial sampled graph projection.
	 * 
	 * @param delegate the original graph.
	 */
	public SpatialSampledGraphProjection(G delegate) {
		super(delegate);
	}
	
	/**
	 * @see {@link SampledGraphProjection#getEdges()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialSampledEdgeDecorator<E>> getEdges() {
		return (Set<? extends SpatialSampledEdgeDecorator<E>>) super.getEdges();
	}

	/**
	 * @see {@link SampledGraphProjection#getVertices()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<? extends SpatialSampledVertexDecorator<V>> getVertices() {
		return (Set<? extends SpatialSampledVertexDecorator<V>>) super.getVertices();
	}

	/**
	 * @see {@link SampledGraphProjection#getEdge(SparseVertex, SparseVertex)}
	 */
	@Override
	public SpatialSampledEdgeDecorator<E> getEdge(SparseVertex v_i, SparseVertex v_j) {
		return (SpatialSampledEdgeDecorator<E>) super.getEdge(v_i, v_j);
	}

	/**
	 * @see {@link SampledGraphProjection#getVertex(org.matsim.contrib.sna.graph.spatial.SpatialVertex)}
	 */
	@Override
	public SpatialSampledVertexDecorator<V> getVertex(V v) {
		return (SpatialSampledVertexDecorator<V>) super.getVertex(v);
	}

	/**
	 * @see {@link SpatialGraph#getCoordinateReferenceSysten()}
	 */
	@Override
	public CoordinateReferenceSystem getCoordinateReferenceSysten() {
		return getDelegate().getCoordinateReferenceSysten();
	}

}
