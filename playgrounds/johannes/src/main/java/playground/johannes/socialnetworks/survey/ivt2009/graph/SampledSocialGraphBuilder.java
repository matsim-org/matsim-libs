/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSocialNetFactory.java
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.graph.AbstractSparseGraphBuilder;
import org.matsim.core.population.PersonImpl;

import playground.johannes.socialnetworks.graph.social.SocialPerson;


/**
 * @author illenberger
 *
 */
public class SampledSocialGraphBuilder extends AbstractSparseGraphBuilder<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> {

	public SampledSocialGraphBuilder() {
		super(new SampledSocialGraphFactory());
	}

	@Override
	public SampledSocialVertex addVertex(SampledSocialGraph g) {
		throw new UnsupportedOperationException("Don't know what to with that...");
	}
	
	public SampledSocialVertex addVertex(SampledSocialGraph g, Person person, int iteration) {
		SampledSocialVertex ego = new SampledSocialVertex(new SocialPerson((PersonImpl) person), null);
		ego.detect(iteration);
		if(insertVertex(g, ego))
			return ego;
		else
			return null;
	}

}
