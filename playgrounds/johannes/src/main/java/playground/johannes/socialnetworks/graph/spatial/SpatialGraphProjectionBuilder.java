/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraphProjectionBuilder.java
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

import org.matsim.contrib.sna.graph.GraphProjectionBuilder;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * An GraphBuilder implementation to build spatial graph projections.
 * 
 * @author illenberger
 * 
 */
public class SpatialGraphProjectionBuilder<G extends SpatialGraph, V extends SpatialVertex, E extends SpatialEdge>
		extends
		GraphProjectionBuilder<G, V, E, SpatialGraphProjection<G, V, E>, SpatialVertexDecorator<V>, SpatialEdgeDecorator<E>> {

	/**
	 * Creates a new spatial graph projection builder.
	 * 
	 * @param factory
	 *            the factory for creating SpatialGraphProjection,
	 *            SpatialVertexDecorator and SpatialEdgeDecorator.
	 */
	public SpatialGraphProjectionBuilder(
			SpatialGraphProjectionFactory<G, V, E> factory) {
		super(factory);
	}

	/**
	 * Creates a new spatial graph projection builder with a
	 * {@link SpatialGraphProjectionFactory}.
	 */
	public SpatialGraphProjectionBuilder() {
		super(new SpatialGraphProjectionFactory<G, V, E>());
	}

	/**
	 * Creates a SpatialGraphProjection on a spatial clipping of a graph. The
	 * projection includes all vertices and edges the are located within the
	 * bounds of <tt>geometry</tt>. Edges that have one vertex within the bounds
	 * and one vertex outside of the bounds are not included.
	 * 
	 * @param delegate
	 *            the original graph.
	 * @param geometry
	 *            the geometry the defines the bounds of the clipping.
	 * @return a SpatialGraphProjection on a clipping of <tt>delegate</tt>.
	 */
	@SuppressWarnings("unchecked")
	public SpatialGraphProjection<G, V, E> decorate(G delegate,
			Geometry geometry) {
		SpatialGraphProjection<G, V, E> projection = createGraph(delegate);

		for (SpatialVertex v : delegate.getVertices()) {
			if(v.getPoint().getSRID() != geometry.getSRID())
				throw new RuntimeException(String.format(
						"Graph and geometry do not have the same coordinate reference system. (%1$s, %2$s)",
						v.getPoint().getSRID(), geometry.getSRID()));
			
			if (geometry.contains(v.getPoint())) {
				addVertex(projection, (V) v);
			}
		}

		for (SpatialEdge e : delegate.getEdges()) {
			SpatialVertexDecorator<V> v_i = projection.getVertex((V) e
					.getVertices().getFirst());
			SpatialVertexDecorator<V> v_j = projection.getVertex((V) e
					.getVertices().getSecond());
			if (v_i != null && v_j != null) {
				addEdge(projection, v_i, v_j, (E) e);
			}
		}

		return projection;
	}

}
