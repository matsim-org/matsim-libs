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

package org.matsim.locationchoice.random;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.locationchoice.utils.QuadTreeRing;

/**
 * @author anhorni
 */
public class RandomLocationMutator extends LocationMutator {

	public RandomLocationMutator(final Network network, Controler controler, Random random) {
		super(network, controler, random);
	}

	public RandomLocationMutator(final Network network, Controler controler, 
			TreeMap<String, QuadTreeRing<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type, Random random) {
		super(network, controler, quad_trees, facilities_of_type, random);
	}


	/*
	 * For all secondary activities of the plan chose randomly a new facility which provides
	 * the possibility to perform the same activity.
	 * plan == selected plan
	 */
	@Override
	public void handlePlan(final Plan plan){
		this.handlePlanForPreDefinedFlexibleTypes(plan);
		super.resetRoutes(plan);
	}


	private void handlePlanForPreDefinedFlexibleTypes(final Plan plan) {
		final List<?> actslegs = plan.getPlanElements();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final ActivityImpl act = (ActivityImpl)actslegs.get(j);

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

	private void setNewLocationForAct(ActivityImpl act, int length) {
		ActivityFacilityImpl facility = this.facilitiesOfType.get(act.getType())[super.random.nextInt(length)];
		act.setFacilityId(facility.getId());
		act.setLinkId(((NetworkImpl) this.network).getNearestLink(facility.getCoord()).getId());
		act.setCoord(facility.getCoord());
	}
}
