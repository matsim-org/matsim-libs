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

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;

public class PersonRemoveLinkAndRoute extends AbstractPersonAlgorithm implements PlanAlgorithm {

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
		//		 loop over all <act>s, remove link-information
		ActIterator actIter = plan.getIteratorAct();
		while (actIter.hasNext()) {
			((Act)actIter.next()).setLink(null);
		}

		//		 loop over all <leg>s, remove route-information
		LegIterator legIter = plan.getIteratorLeg();
		while (legIter.hasNext()) {
			legIter.next().setRoute(null);
		}
	}
}
