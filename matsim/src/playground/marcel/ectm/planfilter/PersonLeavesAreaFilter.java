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

package playground.marcel.ectm.planfilter;

import java.util.List;
import java.util.Map;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
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
		List<Plan> plans = person.getPlans();
		for (Plan plan : plans) {
			for (int i = 1, n = plan.getActsLegs().size(); i < n; i+=2) {
				Leg leg = (Leg) plan.getActsLegs().get(i);
				if (leg.getRoute() == null) {
					return false;
				}
				for (Link link : ((CarRoute) leg.getRoute()).getLinks()) {
					if (!this.areaOfInterest.containsKey(link.getId())) return true;
				}
				// test departure link
				Link link = ((Act) plan.getActsLegs().get(i-1)).getLink();
				if (link != null) {
					if (!this.areaOfInterest.containsKey(link.getId())) return true;
				}
				// test arrival link
				link = ((Act) plan.getActsLegs().get(i+1)).getLink();
				if (link != null) {
					if (!this.areaOfInterest.containsKey(link.getId())) return true;
				}
			}
		}
		return false;
	}

}
