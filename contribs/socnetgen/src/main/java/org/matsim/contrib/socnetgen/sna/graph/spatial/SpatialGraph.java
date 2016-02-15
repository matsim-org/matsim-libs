/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialGraph.java
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

import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.Set;


/**
 * Representation of a spatially embedded graph, i.e., each vertex has a coordinate.
 * 
 * @author illenberger
 *
 */
public interface SpatialGraph extends Graph {

	/**
	 * @see {@link Graph#getVertices()}
	 */
	public Set<? extends SpatialVertex> getVertices();
	
	/**
	 * @see {@link Graph#getEdges()}
	 */
	public Set<? extends SpatialEdge> getEdges();
	
	/**
	 * Returns the coordinate reference system of this graph.
	 * 
	 * @return the coordinate reference system of this graph.
	 */
	public CoordinateReferenceSystem getCoordinateReferenceSysten();
	
}
