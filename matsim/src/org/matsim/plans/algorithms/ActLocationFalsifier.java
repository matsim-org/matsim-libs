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

package org.matsim.plans.algorithms;

import java.util.Iterator;

import org.matsim.basic.v01.BasicAct;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.utils.geometry.CoordI;

/**
 * Moves the geographical location of act's a random amount to north/south and east/west,
 * but at most \a distance, so the original locations are no longer recognizable and the
 * plans can more legally be redistributed. If the act has a linkId assigned and possible
 * a route in its legs, those will be removed as well to force a new assignments to the
 * network based on the new coordinates.
 *
 * @author mrieser
 */
public class ActLocationFalsifier extends PersonAlgorithm implements PlanAlgorithmI {

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
		Iterator<BasicAct> actIter = plan.getIteratorAct();
		while (actIter.hasNext()) {
			Act act = (Act) actIter.next();
			CoordI coord = act.getCoord();
			coord.setXY(coord.getX() + (Gbl.random.nextDouble() - 0.5) *  this.totalDistance,
					coord.getY() + (Gbl.random.nextDouble() - 0.5) * this.totalDistance);
			act.setLink(null);
		}
		Iterator<Leg> legIter = plan.getIteratorLeg();
		while (legIter.hasNext()) {
			Leg leg = legIter.next();
			leg.setRoute(null);
		}
	}

}
