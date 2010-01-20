/* *********************************************************************** *
 * project: org.matsim.*
 * ActLocationFalsifier.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

/**
 * Moves the geographical location of act's a random amount to north/south and east/west,
 * but at most a configurable distance, so the original locations are no longer recognizable and the
 * plans can more legally be redistributed. If the act has a linkId assigned and possible
 * a route in its legs, those will be removed as well to force a new assignments to the
 * network based on the new coordinates.
 *
 * @author mrieser
 */
public class ActLocationFalsifier extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private final double totalDistance;

	public ActLocationFalsifier(double distance) {
		this.totalDistance = 2.0 *  distance;
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

	public void run(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				Coord coord = act.getCoord();
				coord.setXY(coord.getX() + (MatsimRandom.getRandom().nextDouble() - 0.5) *  this.totalDistance,
						coord.getY() + (MatsimRandom.getRandom().nextDouble() - 0.5) * this.totalDistance);
				act.setLinkId(null);
			} else if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				leg.setRoute(null);
			}
		}
	}

}
