/* *********************************************************************** *
 * project: org.matsim.*
 * PersonRemoveLinkAndRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.plans.algorithms;

import java.util.ArrayList;

import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

public class PersonRemoveLinkAndRoute extends PersonAlgorithm implements PlanAlgorithmI {

	public PersonRemoveLinkAndRoute() {
		super();
	}

	@Override
	public void run(final Person person) {
		int nofPlans = person.getPlans().size();
		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			run(plan);
		}
	}

	public void run(final Plan plan) {
		ArrayList<?> actslegs = plan.getActsLegs();

		//		 loop over all <act>s, remove link-information
		for (int j = 0; j < actslegs.size(); j=j+2) {
			Act act = (Act)actslegs.get(j);
			act.setLink(null);
		}

		//		 loop over all <leg>s, remove route-information
		for (int j = 1; j < actslegs.size(); j=j+2) {
			Leg leg = (Leg)actslegs.get(j);
			leg.setRoute(null);
		}
	}
}
