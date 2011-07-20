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
public class ActivityChoiceRndAlterHome implements PlanStrategyModule {

	private final String type = "leisure";
	
	private final Random random;
	
	private final Map<Person, SocialVertex> vertexMapping;
	
	private final ActivityMover mover;
	
	private Map<Person, Double> desiredArrivalTimes;
	
	private Map<Person, Double> desiredDurations;
	
	public ActivityChoiceRndAlterHome(SocialGraph graph, ActivityMover mover, Random random, Map<Person, Double> desiredArrivalTimes, Map<Person, Double> desiredDurations) {
		vertexMapping = new HashMap<Person, SocialVertex>(graph.getVertices().size());
		for(SocialVertex vertex : graph.getVertices()) {
			vertexMapping.put(vertex.getPerson().getPerson(), vertex);
		}
		
		this.random = random;
		this.mover = mover;
		this.desiredArrivalTimes = desiredArrivalTimes;
		this.desiredDurations = desiredDurations;
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
			SocialVertex v_i = vertexMapping.get(plan.getPerson());
			/*
			 * make choice set including home location
			 */
			List<SocialVertex> choiceSet = new ArrayList<SocialVertex>(v_i.getNeighbours().size() + 1);
			for(SocialVertex v_j : v_i.getNeighbours()) {
				choiceSet.add(v_j);
			}
			choiceSet.add(v_i);
			/*
			 * draw
			 */
			SocialVertex v_j = choiceSet.get(random.nextInt(choiceSet.size()));
			Id link = ((Activity) v_j.getPerson().getPerson().getPlans().get(0).getPlanElements().get(0)).getLinkId();
			/*
			 * move activity
			 */
			mover.moveActivity(plan, idx, link, desiredArrivalTimes.get(plan.getPerson()), desiredDurations.get(plan.getPerson()));
		}
	}


	@Override
	public void prepareReplanning() {
	}


	@Override
	public void finishReplanning() {
	}

}
