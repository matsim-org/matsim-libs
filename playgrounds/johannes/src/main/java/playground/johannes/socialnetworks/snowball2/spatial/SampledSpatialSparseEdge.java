/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialEdge.java
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
import org.matsim.contrib.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.core.utils.collections.Tuple;


/**
 * Implementation of {@link SampledSpatialEdge} with a {@link SpatialSparseEdge}.
 * 
 * @author illenberger
 *
 */
public class SampledSpatialSparseEdge extends SpatialSparseEdge implements SampledSpatialEdge {

	/**
	 * @see {@link SpatialSparseEdge#getVertices()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Tuple<? extends SampledSpatialSparseVertex, ? extends SampledSpatialSparseVertex> getVertices() {
		return (Tuple<? extends SampledSpatialSparseVertex, ? extends SampledSpatialSparseVertex>) super.getVertices();
	}

	/**
	 * @see {@link SpatialSparseEdge#getOpposite(Vertex)}
	 */
	@Override
	public SampledSpatialSparseVertex getOpposite(Vertex v) {
		return (SampledSpatialSparseVertex)super.getOpposite(v);
	}
}
