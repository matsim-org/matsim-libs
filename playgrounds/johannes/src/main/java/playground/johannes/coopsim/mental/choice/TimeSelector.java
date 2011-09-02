/* *********************************************************************** *
 * project: org.matsim.*
 * TimeSelector.java
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
import java.util.Set;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class TimeSelector implements ChoiceSelector {

	private Map<SocialVertex, Double> times;
	
	private final String key;
	
	private final Random random;
	
	public TimeSelector(Map<SocialVertex, Double> times, String key, Random random) {
		this.times = times;
		this.key = key;
		this.random = random;
	}
	
	@Override
	public Map<String, Object> select(Map<String, Object> choices) {
		@SuppressWarnings("unchecked")
		Set<SocialVertex> egos = (Set<SocialVertex>) choices.get(ActivityGroupSelector.KEY);
		
		List<Double> choiceSet = new ArrayList<Double>(egos.size());
		for(SocialVertex ego : egos)
			choiceSet.add(times.get(ego));
		
		Double time = choiceSet.get(random.nextInt(choiceSet.size()));
		choices.put(key, time);
		
		return choices;
	}

}
