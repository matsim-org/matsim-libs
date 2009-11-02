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
package playground.johannes.socialnetworks.survey.ivt2009;

import org.matsim.api.basic.v01.Coord;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import playground.johannes.socialnetworks.graph.Edge;
import playground.johannes.socialnetworks.graph.GraphProjection;
import playground.johannes.socialnetworks.graph.Vertex;
import playground.johannes.socialnetworks.graph.VertexDecorator;
import playground.johannes.socialnetworks.graph.spatial.SpatialEdge;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.spatial.Zone;

/**
 * @author illenberger
 *
 */
public class SpatialGraphProjection <G extends SpatialGraph, V extends SpatialVertex, E extends SpatialEdge> extends GraphProjection<G, V, E> {

	private Zone zone;
	
	public SpatialGraphProjection(G delegate, Zone zone) {
		super(delegate);
		
	}

	@Override
	public void decorate() {
		GeometryFactory factory = new GeometryFactory();
		for(Vertex v : getDelegate().getVertices()) {
			Coord c = ((V)v).getCoordinate();
			if(zone.getBorder().contains(factory.createPoint(new Coordinate(c.getX(), c.getY()))))
				addVertex((V) v);
		}
		
		for(Edge e : getDelegate().getEdges()) {
			VertexDecorator<V> v1 = getVertex((V) e.getVertices().getFirst());
			VertexDecorator<V> v2 = getVertex((V) e.getVertices().getSecond());
			if(v1 != null && v2 != null)
				addEdge(v1, v2, (E) e);
		}
	}

}
