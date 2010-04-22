/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSocialNet.java
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
package playground.johannes.socialnetworks.survey.ivt2009.graph;

import java.util.Set;

import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.johannes.socialnetworks.graph.social.SocialGraph;

/**
 * @author illenberger
 *
 */
public class SocialSparseGraph extends SpatialSparseGraph implements SocialGraph {

	public SocialSparseGraph(CoordinateReferenceSystem crs) {
		super(crs);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<? extends SocialSparseEdge> getEdges() {
		return (Set<? extends SocialSparseEdge>) super.getEdges();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<? extends SocialSparseVertex> getVertices() {
		return (Set<? extends SocialSparseVertex>) super.getVertices();
	}

	@Override
	public SocialSparseEdge getEdge(SparseVertex v1, SparseVertex v2) {
		return (SocialSparseEdge) super.getEdge(v1, v2);
	}

	
}
