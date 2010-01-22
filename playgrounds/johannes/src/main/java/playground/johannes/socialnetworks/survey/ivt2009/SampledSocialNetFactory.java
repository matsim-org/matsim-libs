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
package playground.johannes.socialnetworks.survey.ivt2009;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.graph.GraphFactory;
import org.matsim.core.population.PersonImpl;

import playground.johannes.socialnetworks.graph.social.SocialPerson;


/**
 * @author illenberger
 *
 */
public class SampledSocialNetFactory implements GraphFactory<SampledSocialNet, SampledEgo, SampledSocialTie> {

	public SampledSocialTie createEdge() {
		return new SampledSocialTie(0);
	}

	public SampledSocialNet createGraph() {
		return new SampledSocialNet();
	}

	public SampledEgo createVertex() {
		throw new UnsupportedOperationException();
	}
	
	public SampledEgo createVertex(Person person) {
		return new SampledEgo(new SocialPerson((PersonImpl) person));
	}

}
