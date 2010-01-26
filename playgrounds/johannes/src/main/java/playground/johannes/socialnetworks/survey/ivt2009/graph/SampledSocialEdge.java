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
import org.matsim.contrib.sna.snowball.SampledEdge;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.graph.social.SocialEdge;

/**
 * @author illenberger
 *
 */
public class SampledSocialEdge extends SpatialSparseEdge implements SocialEdge, SampledEdge {

	@Override
	public SampledSocialVertex getOpposite(Vertex v) {
		return (SampledSocialVertex) super.getOpposite(v);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Tuple<? extends SampledSocialVertex, ? extends SampledSocialVertex> getVertices() {
		return (Tuple<? extends SampledSocialVertex, ? extends SampledSocialVertex>) super.getVertices();
	}

	
}
