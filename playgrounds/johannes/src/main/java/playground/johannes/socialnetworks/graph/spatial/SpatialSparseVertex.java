/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialVertex.java
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
import org.matsim.contrib.sna.graph.SparseVertex;


/**
 * @author illenberger
 *
 */
public class SpatialSparseVertex extends SparseVertex implements SpatialVertex {

	private Coord coord;
	
	protected SpatialSparseVertex(Coord coord) {
		this.coord = coord;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SpatialSparseVertex> getNeighbours() {
		return (List<? extends SpatialSparseVertex>) super.getNeighbours();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SpatialSparseEdge> getEdges() {
		return (List<? extends SpatialSparseEdge>) super.getEdges();
	}

	public Coord getCoordinate() {
		return coord;
	}
}
