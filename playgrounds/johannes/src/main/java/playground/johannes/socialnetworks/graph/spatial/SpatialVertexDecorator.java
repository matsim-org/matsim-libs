/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialVertexDecorator.java
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

import java.util.List;

import org.matsim.api.core.v01.Coord;

import playground.johannes.socialnetworks.graph.VertexDecorator;

/**
 * @author illenberger
 *
 */
public class SpatialVertexDecorator<V extends SpatialVertex> extends VertexDecorator<V> implements
		SpatialVertex {

	protected SpatialVertexDecorator(V delegate) {
		super(delegate);
	}

	@Override
	public List<? extends SpatialEdgeDecorator<?>> getEdges() {
		return (List<? extends SpatialEdgeDecorator<?>>) super.getEdges();
	}

	@Override
	public List<? extends SpatialVertexDecorator<V>> getNeighbours() {
		return (List<? extends SpatialVertexDecorator<V>>) super.getNeighbours();
	}

	public Coord getCoordinate() {
		return getDelegate().getCoordinate();
	}

}
