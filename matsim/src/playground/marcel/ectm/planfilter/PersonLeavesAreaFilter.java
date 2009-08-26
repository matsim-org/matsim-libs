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

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.PersonAlgorithm;
import org.matsim.population.filters.AbstractPersonFilter;

/**
 * @author mrieser
 */
public class PersonLeavesAreaFilter extends AbstractPersonFilter {

	private final Map<Id, LinkImpl> areaOfInterest;

	public PersonLeavesAreaFilter(final PersonAlgorithm nextAlgorithm, final Map<Id, LinkImpl> areaOfInterest) {
		this.nextAlgorithm = nextAlgorithm;
		this.areaOfInterest = areaOfInterest;
	}

	@Override
	public boolean judge(final PersonImpl person) {
		List<PlanImpl> plans = person.getPlans();
		for (PlanImpl plan : plans) {
			for (int i = 1, n = plan.getPlanElements().size(); i < n; i+=2) {
				LegImpl leg = (LegImpl) plan.getPlanElements().get(i);
				if (leg.getRoute() == null) {
					return false;
				}
				for (Link link : ((NetworkRouteWRefs) leg.getRoute()).getLinks()) {
					if (!this.areaOfInterest.containsKey(link.getId())) return true;
				}
				// test departure link
				LinkImpl link = ((ActivityImpl) plan.getPlanElements().get(i-1)).getLink();
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
