/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityGroupSelector.java
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

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class ActivityGroupSelector implements ChoiceSelector {

	public static final String KEY = "egos";
	
	private final Map<String, ActivityGroupGenerator> generators;
	
	public ActivityGroupSelector() {
		generators = new HashMap<String, ActivityGroupGenerator>();
	}
	
	public void addGenerator(String type, ActivityGroupGenerator generator) {
		generators.put(type, generator);
	}
	
	@Override
	public Map<String, Object> select(Map<String, Object> choices) {
		String type = (String)choices.get(ActivityTypeSelector.KEY);
		
		ActivityGroupGenerator generator = generators.get(type);
		
		SocialVertex ego = (SocialVertex) choices.get(EgoSelector.KEY);
		Set<SocialVertex> group = generator.generate(ego);
		choices.put(KEY, group);
		
		return choices;
	}

}
