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

package org.matsim.population.algorithms;

import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;

public class PlanCalcType extends AbstractPersonAlgorithm implements PlanAlgorithm {

	@Override
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
			if (leg.getMode().equals(BasicLeg.Mode.car)) hasCar = true;
			else if (leg.getMode().equals(BasicLeg.Mode.pt)) hasPt = true;
			else if (leg.getMode().equals(BasicLeg.Mode.ride)) hasRide = true;
			else if (leg.getMode().equals(BasicLeg.Mode.bike)) hasBike = true;
			else if (leg.getMode().equals(BasicLeg.Mode.walk)) hasWalk = true;
		}

		if (hasCar) plan.setType(Plan.Type.CAR);
		else if (hasPt) plan.setType(Plan.Type.PT);
		else if (hasRide) plan.setType(Plan.Type.RIDE);
		else if (hasBike) plan.setType(Plan.Type.BIKE);
		else if (hasWalk) plan.setType(Plan.Type.WALK);
		else plan.setType(null);
	}
}
