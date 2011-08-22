/* *********************************************************************** *
 * project: org.matsim.*
 * JointActivityCoice.java
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
public class JointActivityCoice implements PlanStrategyModule {

	private final Map<Person, SocialVertex> vertexMapping;
	
	private final Random random;
	
	private final ActivityMover mover;
	
	private final Map<Person, Double> desiredArrivalTimes;
	
	private final Map<Person, Double> desiredDurations;
	
	public JointActivityCoice(SocialGraph graph, ActivityMover mover, Random random, Map<Person, Double> desiredArrivalTimes, Map<Person, Double> desiredDurations) {
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
		/*
		 * select activity to change
		 */
		int index = selectPlanIndex(plan);
		/*
		 * select alter to coordinate with
		 */
		SocialVertex ego = vertexMapping.get(plan.getPerson());
		SocialVertex alter = ego.getNeighbours().get(random.nextInt(ego.getNeighbours().size()));
		/*
		 * generate choice set
		 */
		List<Id> links = generateChoiceSet(plan.getPerson(), alter.getPerson().getPerson());
		if(links == null)
			return;
		/*
		 * select location from choice set
		 */
		Id link = links.get(random.nextInt(links.size()));
		/*
		 * move activity
		 */
		mover.moveActivity(plan, index, link, desiredArrivalTimes.get(plan.getPerson()), desiredDurations.get(plan.getPerson()));
	}

	private int selectPlanIndex(Plan plan) {
		return 2;
	}
	
	private List<Id> generateChoiceSet(Person ego, Person alter) {
		List<Id> links = new ArrayList<Id>(2);
		
		Id egoLink = ((Activity) ego.getSelectedPlan().getPlanElements().get(0)).getLinkId();
		Id alterLink = ((Activity) alter.getSelectedPlan().getPlanElements().get(0)).getLinkId();
		
		if(egoLink.equals(alterLink))
			return null;
		
		links.add(egoLink);
		links.add(alterLink);
		
		return links;
	}
	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.replanning.PlanStrategyModule#prepareReplanning()
	 */
	@Override
	public void prepareReplanning() {
		// TODO Auto-generated method stub
		
	}
	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.replanning.PlanStrategyModule#finishReplanning()
	 */
	@Override
	public void finishReplanning() {
		// TODO Auto-generated method stub
		
	}
}
