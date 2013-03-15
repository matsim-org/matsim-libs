/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveRoutes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.population;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class RemoveRoutes extends AbstractPersonAlgorithm {

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					leg.setRoute(null);
				}
			}
		}
	}

}
