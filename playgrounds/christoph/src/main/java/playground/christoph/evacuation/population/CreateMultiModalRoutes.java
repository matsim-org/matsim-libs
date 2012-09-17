/* *********************************************************************** *
 * project: org.matsim.*
 * CreateMultiModalRoutes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.population;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.old.LegRouter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class CreateMultiModalRoutes extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private final Map<String, LegRouter> legRouters;
	private final Set<String> modesToReroute;
	
	public CreateMultiModalRoutes(Map<String, LegRouter> legRouters, Set<String> modesToReroute) {
		this.legRouters = legRouters;
		this.modesToReroute = modesToReroute;
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) this.run(plan);
	}

	@Override
	public void run(Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();
		for (int i = 0; i < planElements.size(); i++) {
			PlanElement planElement = planElements.get(i);
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (modesToReroute.contains(leg.getMode())) {
					LegRouter legRouter = legRouters.get(leg.getMode());
					legRouter.routeLeg(plan.getPerson(), leg, (Activity) planElements.get(i-1), (Activity) planElements.get(i+1), leg.getDepartureTime());					
				}
			}
		}
	}

}
