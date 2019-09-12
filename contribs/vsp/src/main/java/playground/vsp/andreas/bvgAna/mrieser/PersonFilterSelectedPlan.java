/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFilterSelectedPlan.java
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

package playground.vsp.andreas.bvgAna.mrieser;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;

/**
 * Removes all non-selected plans from a person. If a person has no
 * plan selected, the person will be left with zero plans.
 *
 * @author mrieser
 */
public class PersonFilterSelectedPlan extends AbstractPersonAlgorithm {

	public PersonFilterSelectedPlan() {
		super();
	}

	@Override
	public void run(final Person person) {
		int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = person.getPlans().get(planId);
			if (!PersonUtils.isSelected(plan)) {
				person.getPlans().remove(planId);
				planId--;
				nofPlans--;
			}
		}
	}
}
