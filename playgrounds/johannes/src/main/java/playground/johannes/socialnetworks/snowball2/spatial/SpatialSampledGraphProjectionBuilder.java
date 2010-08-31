/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraphProjectionBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.snowball.SampledGraphProjectionBuilder;


/**
 * @author illenberger
 * 
 */
public class SpatialSampledGraphProjectionBuilder<G extends SpatialGraph, V extends SpatialVertex, E extends SpatialEdge>
		extends SampledGraphProjectionBuilder<G, V, E> {

	/**
	 * Creates a new spatial sampled graph projection builder with a
	 * {@link SpatialSampledGraphFactory}.
	 */
	public SpatialSampledGraphProjectionBuilder() {
		super(new SpatialSampledGraphProjectionFactory<G, V, E>());
	}
}
