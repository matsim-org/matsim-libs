/* *********************************************************************** *
 * project: org.matsim.*
 * PersonRemoveNotSelectedPlans.java
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

import java.util.Iterator;

import org.matsim.plans.Person;
import org.matsim.plans.Plan;

/**
 * @author marcel
 * Removes all plans except the selected one from the person. If no plan is
 * selected, a random one will be selected and kept.
 */
public class PersonRemoveNotSelectedPlans extends PersonAlgorithm implements PersonAlgorithmI {

	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		if (plan == null) {
			person.selectRandomPlan();
		}
		for (Iterator<Plan> iter = person.getPlans().iterator(); iter.hasNext(); ) {
			Plan plan2 = iter.next();
			if (!plan2.isSelected()) {
				iter.remove();
			}
		}
	}
}
