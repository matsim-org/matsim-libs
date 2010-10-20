/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSocialTie.java
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

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.graph.social.SocialEdge;

/**
 * @author illenberger
 *
 */
public class SocialSparseEdge extends SpatialSparseEdge implements SocialEdge {//, SampledEdge {

	private double frequency;
	
	private String type;
	
	@Override
	public SocialSparseVertex getOpposite(Vertex v) {
		return (SocialSparseVertex) super.getOpposite(v);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Tuple<? extends SocialSparseVertex, ? extends SocialSparseVertex> getVertices() {
		return (Tuple<? extends SocialSparseVertex, ? extends SocialSparseVertex>) super.getVertices();
	}

	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}
	
	public double getFrequency() {
		return frequency;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
}
