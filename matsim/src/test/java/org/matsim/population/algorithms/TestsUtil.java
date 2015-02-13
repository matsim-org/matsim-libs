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

import org.junit.Ignore;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.facilities.ActivityFacilitiesImpl;

@Ignore
public class TestsUtil {

	static PlanImpl createPlanFromFacilities(ActivityFacilitiesImpl layer, PersonImpl person, String mode, String facString) {
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
		String[] locationIdSequence = facString.split(" ");
		for (int aa=0; aa < locationIdSequence.length; aa++) {
			BasicLocation location = layer.getFacilities().get(Id.create(locationIdSequence[aa], ActivityFacility.class));
			ActivityImpl act;
			act = plan.createAndAddActivity("actAtFacility" + locationIdSequence[aa]);
			act.setFacilityId(location.getId());
			act.setEndTime(10*3600);
			if (aa != (locationIdSequence.length - 1)) {
				plan.createAndAddLeg(mode);
			}
		}
		return plan;
	}

	static PlanImpl createPlanFromLinks(NetworkImpl layer, PersonImpl person, String mode, String linkString) {
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
		String[] locationIdSequence = linkString.split(" ");
		for (int aa=0; aa < locationIdSequence.length; aa++) {
			BasicLocation location = layer.getLinks().get(Id.create(locationIdSequence[aa], Link.class));
			ActivityImpl act;
			act = plan.createAndAddActivity("actOnLink" + locationIdSequence[aa], location.getId());
			act.setEndTime(10*3600);
			if (aa != (locationIdSequence.length - 1)) {
				plan.createAndAddLeg(mode);
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
				if (leg1.getDepartureTime() != leg2.getDepartureTime()) {
					return false;
				}
				if (!leg1.getMode().equals(leg2.getMode())) {
					return false;
				}
				if (leg1.getTravelTime() != leg2.getTravelTime()) {
					return false;
				}
			} else {
				return false;
			}
		} else if (o1 instanceof Activity) {
			if (o2 instanceof Activity) {
				Activity activity1 = (Activity) o1;
				Activity activity2 = (Activity) o2;
				if (activity1.getEndTime() != activity2.getEndTime()) {
					return false;
				}
				if (activity1.getStartTime() != activity2.getStartTime()) {
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
