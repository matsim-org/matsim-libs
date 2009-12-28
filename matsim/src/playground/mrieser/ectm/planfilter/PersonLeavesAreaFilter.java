/* *********************************************************************** *
 * project: org.matsim.*
 * PersonLEavesArea.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mrieser.ectm.planfilter;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.filters.AbstractPersonFilter;

/**
 * @author mrieser
 */
public class PersonLeavesAreaFilter extends AbstractPersonFilter {

	private final Map<Id, Link> areaOfInterest;

	public PersonLeavesAreaFilter(final PersonAlgorithm nextAlgorithm, final Map<Id, Link> areaOfInterest) {
		this.nextAlgorithm = nextAlgorithm;
		this.areaOfInterest = areaOfInterest;
	}

	@Override
	public boolean judge(final Person person) {
		for (Plan plan : person.getPlans()) {
			for (int i = 1, n = plan.getPlanElements().size(); i < n; i+=2) {
				Leg leg = (Leg) plan.getPlanElements().get(i);
				if (leg.getRoute() == null) {
					return false;
				}
				for (Link link : ((NetworkRouteWRefs) leg.getRoute()).getLinks()) {
					if (!this.areaOfInterest.containsKey(link.getId())) return true;
				}
				// test departure link
				Link link = ((ActivityImpl) plan.getPlanElements().get(i-1)).getLink();
				if (link != null) {
					if (!this.areaOfInterest.containsKey(link.getId())) return true;
				}
				// test arrival link
				link = ((ActivityImpl) plan.getPlanElements().get(i+1)).getLink();
				if (link != null) {
					if (!this.areaOfInterest.containsKey(link.getId())) return true;
				}
			}
		}
		return false;
	}

}
