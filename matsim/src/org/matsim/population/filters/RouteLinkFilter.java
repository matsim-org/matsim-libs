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

package org.matsim.population.filters;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.population.algorithms.PlanAlgorithm;

public class RouteLinkFilter extends AbstractPlanFilter {

	private final Set<Id> linkIds;

	public RouteLinkFilter(final PlanAlgorithm nextAlgo) {
		this.nextAlgorithm = nextAlgo;
		this.linkIds = new HashSet<Id>();
	}

	public void addLink(final Id linkId) {
		this.linkIds.add(linkId);
	}

	@Override
	public boolean judge(final PlanImpl plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				for (Link link : ((NetworkRoute) leg.getRoute()).getLinks()) {
					if (this.linkIds.contains(link.getId())) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
