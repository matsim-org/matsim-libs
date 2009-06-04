/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialEdge.java
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

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.SparseEdge;
import playground.johannes.socialnetworks.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class SpatialEdge extends SparseEdge {

	public SpatialEdge(SpatialVertex v1, SpatialVertex v2) {
		super(v1, v2);
	}

	public double length() {
		Coord c1 = getVertices().getFirst().getCoordinate();
		Coord c2 = getVertices().getSecond().getCoordinate();
		return CoordUtils.calcDistance(c1, c2);
	}
	
	@Override
	public SpatialVertex getOpposite(Vertex v) {
		return (SpatialVertex) super.getOpposite(v);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Tuple<? extends SpatialVertex, ? extends SpatialVertex> getVertices() {
		return (Tuple<? extends SpatialVertex, ? extends SpatialVertex>) super.getVertices();
	}

}
