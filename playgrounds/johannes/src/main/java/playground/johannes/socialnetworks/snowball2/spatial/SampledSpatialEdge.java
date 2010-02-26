/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialEdge2.java
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

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.snowball.SampledEdge;
import org.matsim.core.utils.collections.Tuple;

/**
 * Representation of a snowball sampled spatial edge.
 * 
 * @author illenberger
 * 
 */
public interface SampledSpatialEdge extends SampledEdge, SpatialEdge {

	/**
	 * @see {@link SpatialEdge#getVertices()}
	 */
	public Tuple<? extends SampledSpatialVertex, ? extends SampledSpatialVertex> getVertices();

	/**
	 * @see {@link SpatialEdge#getOpposite(Vertex)}
	 */
	public SampledSpatialVertex getOpposite(Vertex v);

}
