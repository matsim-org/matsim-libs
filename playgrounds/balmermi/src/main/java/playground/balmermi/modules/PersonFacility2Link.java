/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFacility2Link
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.balmermi.modules;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonFacility2Link extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private final ActivityFacilities facilities;
	
	public PersonFacility2Link(final ActivityFacilities facilities) {
		this.facilities = facilities;
	}

	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

	public void run(final Plan plan) {
		ActivityImpl act = ((PlanImpl) plan).getFirstActivity();
		while (act != ((PlanImpl) plan).getLastActivity()) {
			act.setLinkId(((ActivityFacilityImpl) this.facilities.getFacilities().get(act.getFacilityId())).getLinkId());
			act = ((PlanImpl) plan).getNextActivity(((PlanImpl) plan).getNextLeg(act));
		}
		act.setLinkId(((ActivityFacilityImpl) this.facilities.getFacilities().get(act.getFacilityId())).getLinkId());
	}
}
