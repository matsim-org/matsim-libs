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

import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonFacility2Link extends AbstractPersonAlgorithm implements PlanAlgorithm {

	public PersonFacility2Link() {
		super();
	}

	@Override
	public void run(final PersonImpl person) {
		for (PlanImpl plan : person.getPlans()) {
			run(plan);
		}
	}

	public void run(final PlanImpl plan) {
		ActivityImpl act = plan.getFirstActivity();
		while (act != plan.getLastActivity()) {
			act.setLink(act.getFacility().getLink());
			act = plan.getNextActivity(plan.getNextLeg(act));
		}
		act.setLink(act.getFacility().getLink());
	}
}
