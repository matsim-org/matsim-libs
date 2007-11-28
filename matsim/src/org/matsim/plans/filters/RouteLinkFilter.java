/* *********************************************************************** *
 * project: org.matsim.*
 * RouteLinkFilter.java.java
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

package org.matsim.plans.filters;

import java.util.HashSet;
import java.util.Set;

import org.matsim.basic.v01.BasicPlan.LegIterator;
import org.matsim.network.Link;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.identifiers.IdI;

public class RouteLinkFilter extends AbstractPlanFilter {

	private final Set<IdI> linkIds;

	public RouteLinkFilter(final PlanAlgorithmI nextAlgo) {
		this.nextAlgorithm = nextAlgo;
		this.linkIds = new HashSet<IdI>();
	}

	public void addLink(final IdI linkId) {
		this.linkIds.add(linkId);
	}

	@Override
	public boolean judge(final Plan plan) {
		LegIterator iter = plan.getIteratorLeg();
		while (iter.hasNext()) {
			Leg leg = (Leg)iter.next();
			Link[] links = leg.getRoute().getLinkRoute();
			for (Link link : links) {
				if (this.linkIds.contains(link.getId())) {
					return true;
				}
			}
		}
		return false;
	}

}
