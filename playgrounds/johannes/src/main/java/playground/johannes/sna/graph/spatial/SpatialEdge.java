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
package playground.johannes.sna.graph.spatial;

import org.matsim.core.utils.collections.Tuple;

import playground.johannes.sna.graph.Edge;
import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.graph.spatial.SpatialVertex;


/**
 * Representation of a spatially embedded edge.
 * 
 * @author illenberger
 *
 */
public interface SpatialEdge extends Edge {
	
	/**
	 * @see {@link Edge#getVertices()}
	 */
	public Tuple<? extends SpatialVertex, ? extends SpatialVertex> getVertices();
	
	/**
	 * @see {@link Edge#getOpposite(Vertex)}
	 */
	public SpatialVertex getOpposite(Vertex v);
	
	/**
	 * Returns the orthodromic distance (great-circle distance) between both vertices.
	 * 
	 * @return the orthodromic distance (great-circle distance) distance between both vertices.
	 */
	public double length();

}
