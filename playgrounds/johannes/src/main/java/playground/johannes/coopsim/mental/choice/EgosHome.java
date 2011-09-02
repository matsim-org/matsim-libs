/* *********************************************************************** *
 * project: org.matsim.*
 * EgosHome.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.mental.choice;

import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class EgosHome implements FacilityChoiceSetGenerator {

	private final Random random;
	
	public EgosHome(Random random) {
		this.random = random;
	}
	
	@Override
	public ChoiceSet<Id> generate(Set<SocialVertex> egos) {
		ChoiceSet<Id> choiceSet = new ChoiceSet<Id>(random);
		
		for(SocialVertex ego : egos) {
			Activity home = (Activity) ego.getPerson().getPerson().getSelectedPlan().getPlanElements().get(0);
			choiceSet.addChoice(home.getFacilityId());
		}
		
		return choiceSet;
	}

}
