/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityChoice2.java
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
package playground.johannes.socialnetworks.sim.locationChoice;

import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class ActivityChoiceRndAlterActivity implements PlanStrategyModule {
	
	private static final int RND_LINKS = 5;

	private final String type = "leisure";
	
	private final Random random;
	
	private final ActivityMover mover;
	
	private Map<Person, Double> desiredArrivalTimes;
	
	private Map<Person, Double> desiredDurations;
	
	private List<Id> linkIds;
	
	private final Map<Person, SocialVertex> vertexMapping;
	
	public ActivityChoiceRndAlterActivity(SocialGraph graph, Network network, ActivityMover mover, Random random, Map<Person, Double> desiredArrivalTimes, Map<Person, Double> desiredDurations) {
		this.random = random;
		this.mover = mover;
		this.desiredArrivalTimes = desiredArrivalTimes;
		this.desiredDurations = desiredDurations;
		
		linkIds = new ArrayList<Id>(network.getLinks().keySet());
		
		vertexMapping = new HashMap<Person, SocialVertex>(graph.getVertices().size());
		for(SocialVertex vertex : graph.getVertices()) {
			vertexMapping.put(vertex.getPerson().getPerson(), vertex);
		}
	}
	

	@Override
	public void handlePlan(Plan plan) {
		TIntArrayList indices = new TIntArrayList(plan.getPlanElements().size());
		/*
		 * retrieve all potential activity indices
		 */
		for(int i = 0; i < plan.getPlanElements().size(); i += 2) {
			Activity act = (Activity) plan.getPlanElements().get(i);
			if(type.equals(act.getType())) {
				indices.add(i);
			}
		}
		if (!indices.isEmpty()) {
			/*
			 * randomly select one index
			 */
			int idx = indices.get(random.nextInt(indices.size()));
			/*
			 * randomly draw new location
			 */
			List<Id> choiceSet = createChoicSet(plan.getPerson());
			Id link = choiceSet.get(random.nextInt(choiceSet.size()));
			/*
			 * move activity
			 */
			mover.moveActivity(plan, idx, link, desiredArrivalTimes.get(plan.getPerson()), desiredDurations.get(plan.getPerson()));
		}
	}

	private List<Id> createChoicSet(Person person) {
		SocialVertex ego = vertexMapping.get(person);
		
		List<Id> choiceSet = new ArrayList<Id>(ego.getNeighbours().size() + RND_LINKS);
		
		for(SocialVertex alter : ego.getNeighbours()) {
			Person p = alter.getPerson().getPerson();
			Plan plan = p.getSelectedPlan();
			Activity act = (Activity) plan.getPlanElements().get(2);
			choiceSet.add(act.getLinkId());
		}
		
		for(int i = 0; i < RND_LINKS; i++) {
			choiceSet.add(linkIds.get(random.nextInt(linkIds.size())));
		}
		
		return choiceSet;
	}

	@Override
	public void prepareReplanning() {
	}

	@Override
	public void finishReplanning() {
	}

}
