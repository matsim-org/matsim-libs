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
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 *
 */
public class SpatialGraphProjectionBuilder<G extends SpatialGraph, V extends SpatialVertex, E extends SpatialEdge> extends
		GraphProjectionBuilder<G, V, E, SpatialGraphProjection<G, V, E>, SpatialVertexDecorator<V>, SpatialEdgeDecorator<E>> {

	public SpatialGraphProjectionBuilder(SpatialGraphProjectionFactory<G, V, E> factory) {
		super(factory);
	}
	/**
	 * @param factory
	 */
	public SpatialGraphProjectionBuilder() {
		super(new SpatialGraphProjectionFactory<G, V, E>());
	}
	
	public SpatialGraphProjection<G, V, E> decorate(G delegate, Geometry geometry) {
		SpatialGraphProjection<G, V, E> projection = createGraph(delegate);
		
		for(SpatialVertex v : delegate.getVertices()) {
			if(geometry.contains(v.getPoint())) {
				addVertex(projection, (V) v);
			}
		}
		
		for(SpatialEdge e : delegate.getEdges()) {
			SpatialVertexDecorator<V> v_i = projection.getVertex((V) e.getVertices().getFirst());
			SpatialVertexDecorator<V> v_j = projection.getVertex((V) e.getVertices().getSecond());
			if(v_i != null && v_j != null) {
				addEdge(projection, v_i, v_j, (E) e);
			}
		}
		
		return projection;
	}

}
