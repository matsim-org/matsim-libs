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

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.graph.SparseEdge;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;


/**
 * @author illenberger
 *
 */
public class SpatialSparseEdge extends SparseEdge implements SpatialEdge {

	public double length() {
		Coord c1 = getVertices().getFirst().getCoordinate();
		Coord c2 = getVertices().getSecond().getCoordinate();
		return CoordUtils.calcDistance(c1, c2);
	}
	
	@Override
	public SpatialSparseVertex getOpposite(Vertex v) {
		return (SpatialSparseVertex) super.getOpposite(v);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Tuple<? extends SpatialSparseVertex, ? extends SpatialSparseVertex> getVertices() {
		return (Tuple<? extends SpatialSparseVertex, ? extends SpatialSparseVertex>) super.getVertices();
	}

}
