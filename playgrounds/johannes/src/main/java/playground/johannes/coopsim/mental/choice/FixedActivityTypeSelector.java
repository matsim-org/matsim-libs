/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTypeSelector.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class FixedActivityTypeSelector implements ChoiceSelector {

	public static final String KEY = "acttype";
	
	private final Map<SocialVertex, String> types;
	
	private final Random random;
	
	public FixedActivityTypeSelector(Map<SocialVertex, String> types, Random random) {
		this.types = types;
		this.random = random;
	}
	
	@Override
	public Map<String, Object> select(Map<String, Object> choices) {
//		@SuppressWarnings("unchecked")
//		List<SocialVertex> egos = (List<SocialVertex>) choices.get(ActivityGroupSelector.KEY);
//		
//		List<String> choiceSet = new ArrayList<String>(egos.size());
//		for(SocialVertex ego : egos)
//			choiceSet.add(types.get(ego));
//		
//		String time = choiceSet.get(random.nextInt(choiceSet.size()));
		
		choices.put(KEY, types.get(choices.get(EgoSelector.KEY)));
	
		return choices;
	}

}
