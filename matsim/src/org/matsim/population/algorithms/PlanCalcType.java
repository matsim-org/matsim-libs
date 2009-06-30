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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.population.LegImpl;

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

		for (int i = 1, max = plan.getPlanElements().size(); i < max; i += 2) {
			LegImpl leg = (LegImpl)plan.getPlanElements().get(i);
			if (leg.getMode().equals(TransportMode.car)) hasCar = true;
			else if (leg.getMode().equals(TransportMode.pt)) hasPt = true;
			else if (leg.getMode().equals(TransportMode.ride)) hasRide = true;
			else if (leg.getMode().equals(TransportMode.bike)) hasBike = true;
			else if (leg.getMode().equals(TransportMode.walk)) hasWalk = true;
		}

		if (hasCar) plan.setType(Plan.Type.CAR);
		else if (hasPt) plan.setType(Plan.Type.PT);
		else if (hasRide) plan.setType(Plan.Type.RIDE);
		else if (hasBike) plan.setType(Plan.Type.BIKE);
		else if (hasWalk) plan.setType(Plan.Type.WALK);
		else plan.setType(null);
	}
}
