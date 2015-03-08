/* *********************************************************************** *
 * project: org.matsim.*
 * RouteLinkFilter.java
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

package org.matsim.contrib.analysis.filters.population;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.population.algorithms.PlanAlgorithm;

public class RouteLinkFilter extends AbstractPlanFilter {

	private final Set<Id<Link>> linkIds;

	public RouteLinkFilter(final PlanAlgorithm nextAlgo) {
		this.nextAlgorithm = nextAlgo;
		this.linkIds = new HashSet<Id<Link>>();
	}

	public void addLink(final Id<Link> linkId) {
		this.linkIds.add(linkId);
	}

	@Override
	public boolean judge(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				for (Id<Link> linkId : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
					if (this.linkIds.contains(linkId)) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
