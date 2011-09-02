/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityFacilitySelector.java
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class ActivityFacilitySelector implements ChoiceSelector {

	public static final String KEY = "facility";
	
	private final Map<String, FacilityChoiceSetGenerator> generators;
	
	public ActivityFacilitySelector() {
		generators = new HashMap<String, FacilityChoiceSetGenerator>();
	}
	
	public void addGenerator(String type, FacilityChoiceSetGenerator generator) {
		generators.put(type, generator);
	}
	
	@Override
	public Map<String, Object> select(Map<String, Object> choices) {
		String type = (String)choices.get(ActivityTypeSelector.KEY);
		FacilityChoiceSetGenerator generator = generators.get(type);
		
		@SuppressWarnings("unchecked")
		Set<SocialVertex> egos = (Set<SocialVertex>) choices.get(ActivityGroupSelector.KEY);
		
		ChoiceSet<Id> choiceSet = generator.generate(egos);
		Id facility = choiceSet.randomChoice();
		
		choices.put(KEY, facility);
		
		return choices;
	}

}
