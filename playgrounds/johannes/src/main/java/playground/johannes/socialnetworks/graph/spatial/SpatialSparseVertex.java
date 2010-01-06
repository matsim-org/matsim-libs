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
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


/**
 * Implementation of {@link SpatialVertex} following the definitions of {@link SparseVertex}.
 * 
 * @author illenberger
 *
 */
public class SpatialSparseVertex extends SparseVertex implements SpatialVertex {

	private final static GeometryFactory geometryFactory = new GeometryFactory();
	
	/**
	 * @deprecated will be replaced by point.
	 */
	private Coord coord;
	
	private Point point;
	
	protected SpatialSparseVertex(Coord coord) {
		this.coord = coord;
		point = geometryFactory.createPoint(new Coordinate(coord.getX(), coord.getY()));
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
		return coord;
	}

	/**
	 * @see {@link SpatialVertex#getPoint()}
	 */
	@Override
	public Point getPoint() {
		return point;
	}
}
