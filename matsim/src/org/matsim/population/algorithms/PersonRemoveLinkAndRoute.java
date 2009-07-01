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

package org.matsim.population.algorithms;

import org.matsim.core.api.experimental.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

public class PersonRemoveLinkAndRoute extends AbstractPersonAlgorithm implements PlanAlgorithm {

	@Override
	public void run(final PersonImpl person) {
		for (PlanImpl plan : person.getPlans()) {
			run(plan);
		}
	}

	public void run(final PlanImpl plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				((ActivityImpl) pe).setLink(null);
			} else if (pe instanceof LegImpl) {
				((LegImpl) pe).setRoute(null);
			}
		}
	}
}
