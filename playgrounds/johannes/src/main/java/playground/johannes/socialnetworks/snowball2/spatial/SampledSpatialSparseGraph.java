/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSpatialGraph.java
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

import java.util.Set;

import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.johannes.socialnetworks.snowball2.SampledGraph;

/**
 * @author illenberger
 *
 */
public class SampledSpatialSparseGraph extends SpatialSparseGraph implements SampledGraph {

	public SampledSpatialSparseGraph(CoordinateReferenceSystem crs) {
		super(crs);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<? extends SampledSpatialSparseEdge> getEdges() {
		return (Set<? extends SampledSpatialSparseEdge>) super.getEdges();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<? extends SampledSpatialSparseVertex> getVertices() {
		return (Set<? extends SampledSpatialSparseVertex>) super.getVertices();
	}

	@Override
	public SampledSpatialSparseEdge getEdge(SparseVertex v1, SparseVertex v2) {
		return (SampledSpatialSparseEdge) super.getEdge(v1, v2);
	}

}
