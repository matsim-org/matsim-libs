/* *********************************************************************** *
 * project: org.matsim.*
 * AltersHome.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

import java.util.Collection;
import java.util.Random;

/**
 * @author illenberger
 *
 */
public class AltersHome implements FacilityChoiceSetGenerator {

	private final Random random;
	
	public AltersHome(Random random) {
		this.random = random;
	}
	
	@Override
	public ChoiceSet<Id<ActivityFacility>> generate(Collection<SocialVertex> egos) {
		ChoiceSet<Id<ActivityFacility>> choiceSet = new ChoiceSet<>(random);
		
		for(SocialVertex ego : egos) {
			for(SocialVertex alter : ego.getNeighbours()) {
				Activity home = (Activity) alter.getPerson().getPerson().getSelectedPlan().getPlanElements().get(0);
				choiceSet.addChoice(home.getFacilityId());
			}
		}
		
		return choiceSet;
	}

}
