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


}
