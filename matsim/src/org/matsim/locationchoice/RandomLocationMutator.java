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

import java.util.List;
import java.util.TreeMap;

import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree;


public class RandomLocationMutator extends LocationMutator {

	//private static final Logger log = Logger.getLogger(RandomLocationMutator.class);
	public RandomLocationMutator(final NetworkLayer network, Controler controler) {
		super(network, controler);
	}
	
	public RandomLocationMutator(final NetworkLayer network, Controler controler,
			TreeMap<String, QuadTree<Facility>> quad_trees,
			TreeMap<String, Facility []> facilities_of_type) {
		super(network, controler, quad_trees, facilities_of_type);
	}

	/*
	 * For all secondary activities of the plan chose randomly a new facility which provides 
	 * the possibility to perform the same activity. 
	 * plan == selected plan
	 */
	@Override
	public void handlePlan(final Plan plan){
		
		List<Activity> movablePrimaryActivities = null; 
		if (this.config.getFixByActType().equals("false")) {
			movablePrimaryActivities = defineMovablePrimaryActivities(plan);
		}
		
		final List<?> actslegs = plan.getPlanElements();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Activity act = (Activity)actslegs.get(j);
			
			boolean isPrimary = false;
			boolean movable = false;
			if (this.config.getFixByActType().equals("false")) {	
				isPrimary = plan.getPerson().getKnowledge().isPrimary(act.getType(), act.getFacilityId());
				// "if" makes things actually slower!
				//if (isPrimary && !act.getType().startsWith("h")) {
					movable = movablePrimaryActivities.contains(act);
				//}
			}
			else {
				isPrimary = plan.getPerson().getKnowledge().isSomewherePrimary(act.getType());
			}
					
			// if home is accidentally not defined as primary
			if ((!isPrimary || movable) && !act.getType().startsWith("h")) {
				int length = this.facilities_of_type.get(act.getType()).length;
				// only one facility: do not need to do location choice
				if (length > 1) {
				//	Facility facility = (Facility)this.quad_trees.get(act.getType()).values().toArray()[
				//	                       MatsimRandom.random.nextInt(size)];
					Facility facility = this.facilities_of_type.get(act.getType())[MatsimRandom.getRandom().nextInt(length)];
					
					act.setFacility(facility);
					act.setLink(this.network.getNearestLink(facility.getCoord()));
					act.setCoord(facility.getCoord());
				}		
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
