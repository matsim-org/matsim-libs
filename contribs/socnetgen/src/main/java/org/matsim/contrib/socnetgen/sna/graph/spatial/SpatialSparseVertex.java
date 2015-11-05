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
package org.matsim.contrib.socnetgen.sna.graph.spatial;

import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.socnetgen.sna.graph.SparseVertex;

import java.util.List;


/**
 * Implementation of {@link SpatialVertex} following the definitions of {@link SparseVertex}.
 * 
 * @author illenberger
 *
 */
public class SpatialSparseVertex extends SparseVertex implements SpatialVertex {

	private Point point;

	/**
	 * Creates an isolated vertex located at point <tt>point</tt>.
	 * 
	 * @param point the point in space at which this vertex is located.
	 */
	protected SpatialSparseVertex(Point point) {
		this.point = point;
	}
	
	/**
	 * @see {@link SparseVertex#getNeighbours()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SpatialSparseVertex> getNeighbours() {
		return (List<? extends SpatialSparseVertex>) super.getNeighbours();
	}

	/**
	 * @see {@link SparseVertex#getEdges()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SpatialSparseEdge> getEdges() {
		return (List<? extends SpatialSparseEdge>) super.getEdges();
	}

	/**
	 * @deprecated will be replaced by {@link #getPoint()}.
	 */
	public Coord getCoordinate() {
		return new Coord(point.getCoordinate().x, point.getCoordinate().y);
	}

	/**
	 * @see {@link SpatialVertex#getPoint()}
	 */
	@Override
	public Point getPoint() {
		return point;
	}
}
