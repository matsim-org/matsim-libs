/* *********************************************************************** *
 * project: org.matsim.*
 * MarkovChain.java
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
package playground.johannes.socialnetworks.sim.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;

import playground.johannes.socialnetworks.sim.locationChoice.ActivityMover;

/**
 * @author illenberger
 *
 */
public class MarkovChain {

	private final Random random;
	
	private final ActivityMover mover;
	
	private final Map<Person, Double> desiredArrivalTimes;
	
	private final Map<Person, Double> desiredDurations;
		
	public MarkovChain(ActivityMover mover, Map<Person, Double> desiredArrivalTimes, Map<Person, Double> desiredDurations, Random random) {
		this.mover = mover;
		this.desiredArrivalTimes = desiredArrivalTimes;
		this.desiredDurations = desiredDurations;
		this.random = random;
	}
	
	public boolean nextState(Set<Person> egos) {
		/*
		 * create choice set
		 */
		List<Id> links = generateChoiceSet(egos);
		if(links == null)
			return false;
		/*
		 * draw link
		 */
		Id link = links.get(random.nextInt(links.size()));
		/*
		 * draw arrival time and duration
		 */
		List<Person> egoList = new ArrayList<Person>(egos);
		double arrivalTime = desiredArrivalTimes.get(egoList.get(random.nextInt(egos.size())));
		double duration = desiredDurations.get(egoList.get(random.nextInt(egos.size())));
		/*
		 * copy plans
		 */
		List<Plan> plans = new ArrayList<Plan>(egos.size());
		for(Person ego : egos) {
			Plan copy = ((PersonImpl)ego).copySelectedPlan();
			plans.add(copy);
		}
		/*
		 * move activities
		 */
		for(Plan plan : plans)
			mover.moveActivity(plan, 2, link, arrivalTime, duration);
//			mover.moveActivity(plan, 2, link, desiredArrivalTimes.get(plan.getPerson()), desiredDurations.get(plan.getPerson()));
		
		return true;
		
	}
	
	protected List<Id> generateChoiceSet(Set<Person> egos) {
		List<Id> links = new ArrayList<Id>(egos.size());
		
		for(Person ego : egos) {
			Id homeLink = ((Activity) ego.getSelectedPlan().getPlanElements().get(0)).getLinkId();
			links.add(homeLink);
		}
		
		for(int i = 0; i < links.size(); i++) {
			for(int j = i+1; j < links.size(); j++) {
				if(links.get(i).equals(links.get(j))) {
//					System.err.println("Same links in choice set!");
					return null;
				}
			}
		}
		return links;
	}
	
}
