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

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.population.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.locationchoice.utils.QuadTreeRing;

/*
 * @author anhorni
 */
public class RandomLocationMutator extends LocationMutator {

	private static final Logger log = Logger.getLogger(RandomLocationMutator.class);
	public RandomLocationMutator(final NetworkLayer network, Controler controler) {
		super(network, controler);
	}
	
	public RandomLocationMutator(final NetworkLayer network, Controler controler,
			TreeMap<String, QuadTreeRing<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacility []> facilities_of_type) {
		super(network, controler, quad_trees, facilities_of_type);
	}

	/*
	 * For all secondary activities of the plan chose randomly a new facility which provides 
	 * the possibility to perform the same activity. 
	 * plan == selected plan
	 */
	@Override
	public void handlePlan(final Plan plan){	
		if (super.locationChoiceBasedOnKnowledge) {
			//log.info("LC based on knowledge");
			this.handlePlanBasedOnKnowldge(plan);	
		}
		else {
			//log.info("LC based on defined types");
			//log.info(this.defineFlexibleActivities.getFlexibleTypes());
			this.handlePlanForPreDefinedFlexibleTypes(plan);	
		}
		super.resetRoutes(plan);		
	}
		
	private void handlePlanBasedOnKnowldge(final Plan plan) {
		
		List<Activity> movablePrimaryActivities = defineMovablePrimaryActivities(plan);
		
		final List<?> actslegs = plan.getPlanElements();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Activity act = (Activity)actslegs.get(j);
				
			boolean	isPrimary = plan.getPerson().getKnowledge().isPrimary(act.getType(), act.getFacilityId());
			boolean	movable = movablePrimaryActivities.contains(act);
	
			// if home is accidentally not defined as primary
			if ((!isPrimary || movable) && !(act.getType().startsWith("h") || act.getType().startsWith("tta"))) {
				int length = this.facilitiesOfType.get(act.getType()).length;
				// only one facility: do not need to do location choice
				if (length > 1) {
					this.setNewLocationForAct(act, length);
				}		
			}	
		}
	}
	
	private void handlePlanForPreDefinedFlexibleTypes(final Plan plan) {
		final List<?> actslegs = plan.getPlanElements();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Activity act = (Activity)actslegs.get(j);
				
			// if home is accidentally not defined as primary
			if (super.defineFlexibleActivities.getFlexibleTypes().contains(act.getType())) {
				int length = this.facilitiesOfType.get(act.getType()).length;
				// only one facility: do not need to do location choice
				if (length > 1) {
					this.setNewLocationForAct(act, length);
				}		
			}	
		}
	}
	
	private void setNewLocationForAct(Activity act, int length) {
		ActivityFacility facility = this.facilitiesOfType.get(act.getType())[MatsimRandom.getRandom().nextInt(length)];				
		act.setFacility(facility);
		act.setLink(this.network.getNearestLink(facility.getCoord()));
		act.setCoord(facility.getCoord());	
	}
}
