/* *********************************************************************** *
 * project: org.matsim.*
 * PlanCalcType.java
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

import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

public class PlanCalcType extends PersonAlgorithm implements PlanAlgorithmI {

	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

	public void run(Plan plan) {
		boolean hasCar = false;
		boolean hasPt = false;
		boolean hasRide = false;
		boolean hasWalk = false;
		boolean hasBike = false;
		
		for (int i = 1, max = plan.getActsLegs().size(); i < max; i += 2) {
			Leg leg = (Leg)plan.getActsLegs().get(i);
			if (leg.getMode().equals("car")) hasCar = true;
			else if (leg.getMode().equals("pt")) hasPt = true;
			else if (leg.getMode().equals("ride")) hasRide = true;
			else if (leg.getMode().equals("bike")) hasBike = true;
			else if (leg.getMode().equals("walk")) hasWalk = true;
		}
		
		if (hasCar) plan.setType("car");
		else if (hasPt) plan.setType("pt");
		else if (hasRide) plan.setType("ride");
		else if (hasBike) plan.setType("bike");
		else if (hasWalk) plan.setType("walk");
		else plan.setType(null);
	}
}
