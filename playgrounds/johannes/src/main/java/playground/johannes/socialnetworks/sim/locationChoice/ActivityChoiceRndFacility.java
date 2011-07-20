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
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;

/**
 * @author illenberger
 *
 */
public class ActivityChoiceRndFacility implements PlanStrategyModule {
	
	private static final String type = "leisure";
	
	private final Random random;
	
	private final ActivityMover mover;
	
	private Map<Person, Double> desiredArrivalTimes;
	
	private Map<Person, Double> desiredDurations;
	
	final private List<Id> linkIds;
	
	public ActivityChoiceRndFacility(ActivityFacilities facilities, Network network, ActivityMover mover, Random random, Map<Person, Double> desiredArrivalTimes, Map<Person, Double> desiredDurations) {
		this.random = random;
		this.mover = mover;
		this.desiredArrivalTimes = desiredArrivalTimes;
		this.desiredDurations = desiredDurations;
		
		linkIds = new ArrayList<Id>(network.getLinks().keySet());
		
		for(ActivityFacility facility : facilities.getFacilities().values()) {
			boolean isLeisure = false;
			for(ActivityOption option : facility.getActivityOptions().values()) {
				if(option.getType().equalsIgnoreCase(type)) {
					isLeisure = true;
					break;
				}
			}
			
			if(isLeisure) {
				if(facility.getLinkId() != null)
					linkIds.add(facility.getLinkId());
			}
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
			Id link = linkIds.get(random.nextInt(linkIds.size()));
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
