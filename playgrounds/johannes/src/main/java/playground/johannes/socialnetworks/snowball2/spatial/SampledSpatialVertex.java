/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialVertex2.java
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
package playground.johannes.socialnetworks.snowball2.spatial;

import java.util.List;

import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * Representation of a snowball sampled spatial vertex.
 * 
 * @author illenberger
 * 
 */
public interface SampledSpatialVertex extends SampledVertex, SpatialVertex {

	/**
	 * @see {@link SpatialVertex#getNeighbours()}
	 */
	public List<? extends SampledSpatialVertex> getNeighbours();

	/**
	 * @see {@link SpatialVertex#getEdges()}
	 */
	public List<? extends SampledSpatialEdge> getEdges();

}
