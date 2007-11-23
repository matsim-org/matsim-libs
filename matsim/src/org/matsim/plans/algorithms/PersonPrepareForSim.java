/* *********************************************************************** *
 * project: org.matsim.*
 * PersonPrepareForSim.java
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

import org.matsim.gbl.Gbl;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

public class PersonPrepareForSim extends PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final PlanAlgorithmI router;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonPrepareForSim(final PlanAlgorithmI router) {
		super();
		this.router = router;
	}


	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		// first make sure we have a selected plan
		Plan selectedPlan = person.getSelectedPlan();
		if (selectedPlan == null) {
			// no selected plan was found, select the first one as a default
			if (person.getPlans().size() > 0) {
				selectedPlan = person.getPlans().get(0);
				person.setSelectedPlan(selectedPlan);
			} else {
				Gbl.warningMsg(this.getClass(), "run()", "Person " + person.getId() + " has no plans!");
			}
		}

		// next make sure all the plans have valid routes
		for (Plan plan : person.getPlans()) {
			boolean hasRoute = true;
			ArrayList<Object> actslegs = plan.getActsLegs();
			for (int i = 1; i < actslegs.size(); i = i+2) {
				Leg leg = (Leg)actslegs.get(i);
				if (leg.getRoute() == null) {
					hasRoute = false;
				}
			}
			if (!hasRoute) {
				this.router.run(plan);
			}
		}

	}

}
