/* *********************************************************************** *
 * project: org.matsim.*
 * EgosFacilities.java
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class EgosFacilities implements FacilityChoiceSetGenerator {

	private final Map<SocialVertex, List<Id>> choiceSets;
	
	private final Random random;
	
	public EgosFacilities(Map<SocialVertex, List<Id>> choiceSets, Random random) {
		this.choiceSets = choiceSets;
		this.random = random;
	}
	
	@Override
	public ChoiceSet<Id> generate(Collection<SocialVertex> egos) {
		ChoiceSet<Id> choiceSet = new ChoiceSet<Id>(random);
		
		for(SocialVertex ego : egos) {
			List<Id> facilityIds = choiceSets.get(ego);
			for(Id id : facilityIds)
				choiceSet.addChoice(id);
		}
		
		return choiceSet;
	}

}
