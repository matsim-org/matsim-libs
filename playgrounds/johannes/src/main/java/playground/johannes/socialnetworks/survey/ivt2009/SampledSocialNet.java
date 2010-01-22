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
package playground.johannes.socialnetworks.survey.ivt2009;

import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.graph.SparseVertex;
import org.matsim.contrib.sna.snowball.SampledGraph;

import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.social.SocialPerson;

/**
 * @author illenberger
 *
 */
public class SampledSocialNet extends SocialNetwork implements SampledGraph {

	@Override
	@SuppressWarnings("unchecked")
	public Set<? extends SampledSocialTie> getEdges() {
		return (Set<? extends SampledSocialTie>) super.getEdges();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<? extends SampledEgo> getVertices() {
		return (Set<? extends SampledEgo>) super.getVertices();
	}

	@Override
	public SampledSocialTie getEdge(SparseVertex v1, SparseVertex v2) {
		return (SampledSocialTie) super.getEdge(v1, v2);
	}

	@Override
	public SampledEgo getEgo(SocialPerson p) {
		return (SampledEgo) super.getEgo(p);
	}
}
