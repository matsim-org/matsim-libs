/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.population.algorithms;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;

@Disabled
public class TestsUtil {

	static Plan createPlanFromFacilities(ActivityFacilitiesImpl layer, Person person, String mode, String facString) {
		Plan plan = PopulationUtils.createPlan(person);
		String[] locationIdSequence = facString.split(" ");
		for (int aa=0; aa < locationIdSequence.length; aa++) {
			ActivityFacility location = layer.getFacilities().get(Id.create(locationIdSequence[aa], ActivityFacility.class));
			Activity act;
			act = PopulationUtils.createAndAddActivity(plan, "actAtFacility" + locationIdSequence[aa]);
			act.setFacilityId(location.getId());
			act.setEndTime(10*3600);
			if (aa != (locationIdSequence.length - 1)) {
				PopulationUtils.createAndAddLeg( plan, mode );
			}
		}
		return plan;
	}

	static Plan createPlanFromLinks(Network layer, Person person, String mode, String linkString) {
		Plan plan = PopulationUtils.createPlan(person);
		String[] locationIdSequence = linkString.split(" ");
		for (int aa=0; aa < locationIdSequence.length; aa++) {
			Link location = layer.getLinks().get(Id.create(locationIdSequence[aa], Link.class));
			Activity act;
			act = PopulationUtils.createAndAddActivityFromLinkId(plan, "actOnLink" + locationIdSequence[aa], (Id<Link>) location.getId());
			act.setEndTime(10*3600);
			if (aa != (locationIdSequence.length - 1)) {
				PopulationUtils.createAndAddLeg( plan, mode );
			}
		}
		return plan;
	}

	/* Warning: This is NOT claimed to be correct. (It isn't.)
	 *
	 */
	static boolean equals(PlanElement o1, PlanElement o2) {
		if (o1 instanceof Leg) {
			if (o2 instanceof Leg) {
				Leg leg1 = (Leg) o1;
				Leg leg2 = (Leg) o2;
				if (!leg1.getDepartureTime().equals(leg2.getDepartureTime())) {
					return false;
				}
				if (!leg1.getMode().equals(leg2.getMode())) {
					return false;
				}
				if (!leg1.getTravelTime().equals(leg2.getTravelTime())) {
					return false;
				}
			} else {
				return false;
			}
		} else if (o1 instanceof Activity) {
			if (o2 instanceof Activity) {
				Activity activity1 = (Activity) o1;
				Activity activity2 = (Activity) o2;
				if (activity1.getEndTime().isUndefined() ^ activity2.getEndTime().isUndefined()) {
					return false;
				}
				if (activity1.getEndTime().isDefined() && activity1.getEndTime().seconds()
						!= activity2.getEndTime().seconds()) {
					return false;
				}
				if (activity1.getStartTime().isUndefined() ^ activity2.getStartTime().isUndefined()) {
					return false;
				}
				if (activity1.getStartTime().isDefined() && activity1.getStartTime().seconds()
						!= activity2.getStartTime().seconds()) {
					return false;
				}
			} else {
				return false;
			}
		} else {
			throw new RuntimeException ("Unexpected PlanElement");
		}
		return true;
	}

	public static boolean equals(List<PlanElement> planElements,
			List<PlanElement> planElements2) {
		int nElements = planElements.size();
		if (nElements != planElements2.size()) {
			return false;
		} else {
			for (int i = 0; i < nElements; i++) {
				if (!equals(planElements.get(i), planElements2.get(i))) {
					return false;
				}
			}
		}
		return true;
	}

}
