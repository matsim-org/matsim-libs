/* *********************************************************************** *
 * project: org.matsim.*
 * PersonRemoveLinkAndRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.population.algorithms;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

public final class PersonRemoveLinkAndRoute extends AbstractPersonAlgorithm implements PlanAlgorithm {

	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

	@Override
	public void run(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				((Activity) pe).setLinkId(null);
			} else if (pe instanceof Leg) {
				((Leg) pe).setRoute(null);
			}
		}
	}
}
