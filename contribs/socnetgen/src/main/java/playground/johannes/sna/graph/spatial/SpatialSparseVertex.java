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
package playground.johannes.sna.graph.spatial;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.johannes.sna.graph.SparseVertex;

import com.vividsolutions.jts.geom.Point;


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
		return new CoordImpl(point.getCoordinate().x, point.getCoordinate().y);
	}

	/**
	 * @see {@link SpatialVertex#getPoint()}
	 */
	@Override
	public Point getPoint() {
		return point;
	}
}
