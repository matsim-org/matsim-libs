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

/**
 * 
 */
package playground.johannes.socialnetworks.ivtsurveys;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPlanElement;

import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;

/**
 * @author illenberger
 *
 */
public class SampledSocialNet<P extends BasicPerson<? extends BasicPlan<? extends BasicPlanElement>>> extends SocialNetwork<P> {

	private Set<Ego<P>> sampledVertices = new HashSet<Ego<P>>();
	
	@Override
	public Ego<P> addEgo(P person) {
		Ego<P> ego = super.addEgo(person);
		sampledVertices.add(ego);
		return ego;
	}

	public Ego<P> addUnsampledEgo(P person) {
		return addEgo(person);
	}
	
	@Override
	public Set<? extends Ego<P>> getVertices() {
		return sampledVertices;
	}

}
