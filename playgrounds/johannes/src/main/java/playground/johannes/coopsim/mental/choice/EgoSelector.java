/* *********************************************************************** *
 * project: org.matsim.*
 * EgoSelector.java
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

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class EgoSelector implements ChoiceSelector {
	
	public static final String KEY = "ego";

	private final List<SocialVertex> egoList;
	
	private final Random random;
	
	public EgoSelector(SocialGraph graph, Random random) {
		this.random = random;
		
		this.egoList = new ArrayList<SocialVertex>(graph.getVertices().size());
		for(SocialVertex v : graph.getVertices())
			egoList.add(v);
	}
	
	@Override
	public Map<String, Object> select(Map<String, Object> choices) {
		/*
		 * draw random ego
		 */
		SocialVertex ego = egoList.get(random.nextInt(egoList.size()));
		choices.put(KEY, ego);
		return choices;
	}

}
