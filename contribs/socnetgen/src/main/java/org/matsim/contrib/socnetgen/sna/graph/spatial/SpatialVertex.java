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
import org.matsim.contrib.socnetgen.sna.graph.Vertex;

import java.util.List;


/**
 * Representation of a vertex embedded in space.
 * 
 * @author illenberger
 *
 */
public interface SpatialVertex extends Vertex {
	
	/**
	 * @see {@link Vertex#getEdges()}
	 */
	public List<? extends SpatialEdge> getEdges();
	
	/**
	 * @see {@link Vertex#getNeighbours()}
	 */
	public List<? extends SpatialVertex> getNeighbours();

	/**
	 * Returns the 2-dimensional coordinate of this vertex.
	 * 
	 * @deprecated will be replaced by {@link #getPoint()}.
	 * @return the 2-dimensional coordinate of this vertex.
	 */
	public Coord getCoordinate();
	
	/**
	 * Returns the point in space this vertex is located at.
	 * 
	 * @return the point in space this vertex is located at.
	 */
	public Point getPoint();
	
}
