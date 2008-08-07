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

import org.matsim.basic.v01.BasicActImpl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.utils.geometry.Coord;

/**
 * Moves the geographical location of act's a random amount to north/south and east/west,
 * but at most \a distance, so the original locations are no longer recognizable and the
 * plans can more legally be redistributed. If the act has a linkId assigned and possible
 * a route in its legs, those will be removed as well to force a new assignments to the
 * network based on the new coordinates.
 *
 * @author mrieser
 */
public class ActLocationFalsifier extends AbstractPersonAlgorithm implements PlanAlgorithmI {

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
		Iterator<BasicActImpl> actIter = plan.getIteratorAct();
		while (actIter.hasNext()) {
			Act act = (Act) actIter.next();
			Coord coord = act.getCoord();
			coord.setXY(coord.getX() + (MatsimRandom.random.nextDouble() - 0.5) *  this.totalDistance,
					coord.getY() + (MatsimRandom.random.nextDouble() - 0.5) * this.totalDistance);
			act.setLink(null);
		}
		Iterator<Leg> legIter = plan.getIteratorLeg();
		while (legIter.hasNext()) {
			Leg leg = legIter.next();
			leg.setRoute(null);
		}
	}

}
