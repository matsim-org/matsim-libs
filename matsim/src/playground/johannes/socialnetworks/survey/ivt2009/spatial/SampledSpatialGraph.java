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
package playground.johannes.socialnetworks.survey.ivt2009.spatial;

import java.util.Set;

import playground.johannes.socialnetworks.graph.SparseEdge;
import playground.johannes.socialnetworks.graph.SparseVertex;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;
import playground.johannes.socialnetworks.survey.ivt2009.SampledGraph;

/**
 * @author illenberger
 *
 */
public class SampledSpatialGraph extends SpatialGraph implements SampledGraph {

	@SuppressWarnings("unchecked")
	public Set<? extends SampledSpatialEdge> getEdges() {
		return (Set<? extends SampledSpatialEdge>) super.getEdges();
	}

	@SuppressWarnings("unchecked")
	public Set<? extends SampledSpatialVertex> getVertices() {
		return (Set<? extends SampledSpatialVertex>) super.getVertices();
	}

	@Override
	public SampledSpatialEdge getEdge(SparseVertex v1, SparseVertex v2) {
		return (SampledSpatialEdge) super.getEdge(v1, v2);
	}

	@Override
	protected boolean insertEdge(SparseEdge e, SparseVertex v1, SparseVertex v2) {
		return super.insertEdge(e, v1, v2);
	}

	@Override
	protected boolean insertVertex(SparseVertex v) {
		return super.insertVertex(v);
	}

}
