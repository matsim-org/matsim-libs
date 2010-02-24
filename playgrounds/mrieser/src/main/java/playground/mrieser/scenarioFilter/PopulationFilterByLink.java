/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.scenarioFilter;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.population.algorithms.PersonAlgorithm;

public class PopulationFilterByLink implements PersonAlgorithm {

	private final Set<Id> linkIds;

	public PopulationFilterByLink(final Set<Id> linkIds) {
		this.linkIds = linkIds;
	}

	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		boolean containsLink = false;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (leg.getRoute() instanceof NetworkRoute) {
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					for (Id linkId : route.getLinkIds()) {
						if (linkIds.contains(linkId)) {
							containsLink = true;
							break;
						}
					}
					if (linkIds.contains(route.getStartLinkId()) || (linkIds.contains(route.getEndLinkId()))) {
						containsLink = true;
						break;
					}
				}
			}
		}
		if (!containsLink) {
			((PersonImpl) person).removePlan(plan);
		}
	}

}
