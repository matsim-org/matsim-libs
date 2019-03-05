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

package org.matsim.contrib.locationchoice.random;

import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.LocationMutator;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

/**
 * @author anhorni
 */
public class RandomLocationMutator extends LocationMutator {

	public RandomLocationMutator(final Scenario scenario, Random random) {
		super(scenario, random);
	}

	public RandomLocationMutator(final Scenario scenario, TreeMap<String, ? extends QuadTree<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type, Random random) {
		super(scenario, quad_trees, facilities_of_type, random);
	}


	/*
	 * For all secondary activities of the plan chose randomly a new facility which provides
	 * the possibility to perform the same activity.
	 * plan == selected plan
	 */
	@Override
	public void run(final Plan plan) {
		this.handlePlanForPreDefinedFlexibleTypes(plan);
		PlanUtils.resetRoutes(plan);
	}


	private void handlePlanForPreDefinedFlexibleTypes(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				final Activity act = (Activity) pe;
	
				// if home is accidentally not defined as primary
				if ( super.getDefineFlexibleActivities().getFlexibleTypes().contains(act.getType() )) {
					int length = this.getFacilitiesOfType().get(act.getType() ).length;
					// only one facility: do not need to do location choice
					if (length > 1) {
						this.setNewLocationForAct((Activity) act, length);
					}
				}
			}
		}
	}

	private void setNewLocationForAct(Activity act, int length) {
		ActivityFacilityImpl facility = this.getFacilitiesOfType().get(act.getType() )[super.getRandom().nextInt(length )];
		act.setFacilityId(facility.getId());
		act.setLinkId(NetworkUtils.getNearestLink(((Network) this.getScenario().getNetwork()), facility.getCoord() ).getId() );
		act.setCoord(facility.getCoord());
	}
}
