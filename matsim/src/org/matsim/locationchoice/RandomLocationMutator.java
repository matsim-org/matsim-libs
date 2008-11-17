/* *********************************************************************** *
 * project: org.matsim.*
 * RandomLocationMutator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.locationchoice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.controler.Controler;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
//import org.apache.log4j.Logger;


public class RandomLocationMutator extends LocationMutator {

	//private static final Logger log = Logger.getLogger(RandomLocationMutator.class);
	public RandomLocationMutator(final NetworkLayer network, Controler controler) {
		super(network, controler);
	}

	/*
	 * For all secondary activities of the plan chose randomly a new facility which provides 
	 * the possibility to perform the same activity. 
	 * plan == selected plan
	 */
	@Override
	public void handlePlan(final Plan plan){
		
		if (!this.personPrimaryActs.containsKey(plan.getPerson().getId())) {
			this.personPrimaryActs.put(plan.getPerson().getId(), new TreeSet<String>());
		}
		
		ArrayList<Activity> secondaryActivities = plan.getPerson().getKnowledge().getActivities(false);
		
		Iterator<Activity> iter_activities = secondaryActivities.iterator();
		while (iter_activities.hasNext()){
			Activity activity = iter_activities.next();
			String type = activity.getType();
			exchangeFacilities(type ,this.quad_trees.get(type).values().toArray(), plan);
		}
	}

	public void exchangeFacilities(final String type, Object [] exchange_facilities, final Plan plan) {
		
		if (exchange_facilities.length == 0) return;
		
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			boolean isPrimary = plan.getPerson().getKnowledge().isPrimary(act.getType(), act.getFacilityId());
			boolean inPersonPrimaryActs = this.personPrimaryActs.get(plan.getPerson().getId()).contains(act.getType());
			
			
			if (act.getType().equals(type)  && (!isPrimary || inPersonPrimaryActs)) {
				
				
				final Facility facility=(Facility)exchange_facilities[
				           MatsimRandom.random.nextInt(exchange_facilities.length-1)];
				
				act.setFacility(facility);
				act.setLink(this.network.getNearestLink(facility.getCenter()));
				act.setCoord(facility.getCenter());	
			}
			
			if (act.getType().equals(type) && plan.getPerson().getKnowledge().isPrimary(act.getType(), act.getFacilityId()) &&
					!this.personPrimaryActs.get(plan.getPerson().getId()).contains(act.getType())) {
				
				this.personPrimaryActs.get(plan.getPerson().getId()).add(act.getType());
			}
			
			
		}

		// loop over all <leg>s, remove route-information
		// routing is done after location choice
		for (int j = 1; j < actslegs.size(); j=j+2) {
			final Leg leg = (Leg)actslegs.get(j);
			leg.setRoute(null);
		}
	}
}
