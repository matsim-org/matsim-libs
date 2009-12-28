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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

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

		if (hasCar) ((PlanImpl) plan).setType(PlanImpl.Type.CAR);
		else if (hasPt) ((PlanImpl) plan).setType(PlanImpl.Type.PT);
		else if (hasRide) ((PlanImpl) plan).setType(PlanImpl.Type.RIDE);
		else if (hasBike) ((PlanImpl) plan).setType(PlanImpl.Type.BIKE);
		else if (hasWalk) ((PlanImpl) plan).setType(PlanImpl.Type.WALK);
		else ((PlanImpl) plan).setType(null);
	}
}
