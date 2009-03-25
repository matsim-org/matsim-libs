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

import java.util.Iterator;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.basic.v01.BasicActivityImpl;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Moves the geographical location of act's a random amount to north/south and east/west,
 * but at most \a distance, so the original locations are no longer recognizable and the
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

	@SuppressWarnings("unchecked")
	public void run(Plan plan) {
		Iterator<BasicActivityImpl> actIter = plan.getIteratorAct();
		while (actIter.hasNext()) {
			Activity act = (Activity) actIter.next();
			Coord coord = act.getCoord();
			coord.setXY(coord.getX() + (MatsimRandom.getRandom().nextDouble() - 0.5) *  this.totalDistance,
					coord.getY() + (MatsimRandom.getRandom().nextDouble() - 0.5) * this.totalDistance);
			act.setLink(null);
		}
		Iterator<Leg> legIter = plan.getIteratorLeg();
		while (legIter.hasNext()) {
			Leg leg = legIter.next();
			leg.setRoute(null);
		}
	}

}
