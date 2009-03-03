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
import java.util.List;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.NetworkLayer;


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
		
		List<Act> movablePrimaryActivities = null; 
		if (Gbl.getConfig().locationchoice().getFixByActType().equals("false")) {
			movablePrimaryActivities = defineMovablePrimaryActivities(plan);
		}
		
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			
			boolean isPrimary = false;
			boolean movable = false;
			if (Gbl.getConfig().locationchoice().getFixByActType().equals("false")) {	
				isPrimary = plan.getPerson().getKnowledge().isPrimary(act.getType(), act.getFacilityId());
				movable = movablePrimaryActivities.contains(act);
			}
			else {
				isPrimary = plan.getPerson().getKnowledge().isSomewherePrimary(act.getType());
			}
					
			if (!isPrimary || movable) {
				int length = this.facilities_of_type.get(act.getType()).length;
				// only one facility: do not need to do location choice
				if (length > 1) {
				//	Facility facility = (Facility)this.quad_trees.get(act.getType()).values().toArray()[
				//	                       MatsimRandom.random.nextInt(size)];
					Facility facility = this.facilities_of_type.get(act.getType())[MatsimRandom.random.nextInt(length)];
					
					act.setFacility(facility);
					act.setLink(this.network.getNearestLink(facility.getCenter()));
					act.setCoord(facility.getCenter());
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
