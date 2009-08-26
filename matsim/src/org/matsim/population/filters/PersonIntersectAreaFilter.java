/* *********************************************************************** *
 * project: org.matsim.*
 * PersonIntersectAreaFilter.java
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

import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.PersonAlgorithm;
import org.matsim.world.WorldUtils;

/**
 * Filters all persons out whose routes do not lead at least once through
 * one of the links within the <code>areaOfInterest</code>, passing only
 * persons to the next algorithm that travel at least once on a link of
 * the <code>areaOfInterest</code>. For the case a leg is missing route
 * information, and thus missing the information which links are used,
 * an {@linkplain #setAlternativeAOI(Coord, double) alternative area of
 * interest can be specified}, given by its center and a radius. In that
 * case it is tested whether the bee-line connecting the start and end
 * point of the leg leads through the alternative aoi-circle.
 *
 * @author laemmel
 * @author mrieser
 */
public class PersonIntersectAreaFilter extends AbstractPersonFilter {

	private final Map<Id, LinkImpl> areaOfInterest;
	private Coord aoiCenter = null;
	private double aoiRadius = 0.0;

	public PersonIntersectAreaFilter(final PersonAlgorithm nextAlgorithm, final Map<Id, LinkImpl> areaOfInterest) {
		this.nextAlgorithm = nextAlgorithm;
		this.areaOfInterest = areaOfInterest;
	}

	/**
	 * Sets an alternative area of interest, given by its center and a radius.
	 * This alternative aoi is used when a route is missing in a leg and thus no
	 * links are available to decide. In that case it is checked if the bee-line
	 * between from and to activity cuts the circle with radius <code>aoiRadius</code>
	 * around <code>aoiCenter</code>.
	 *
	 * @param aoiCenter
	 * @param aoiRadius
	 */
	public void setAlternativeAOI(final Coord aoiCenter, final double aoiRadius) {
		this.aoiCenter = aoiCenter;
		this.aoiRadius = aoiRadius;
	}


	@Override
	public boolean judge(final PersonImpl person) {
		List<PlanImpl> plans = person.getPlans();
		for (PlanImpl plan : plans) {
			for (int i = 1, n = plan.getPlanElements().size(); i < n; i+=2) {
				LegImpl leg = (LegImpl) plan.getPlanElements().get(i);
				if (leg.getRoute() == null) {
					if (judgeByBeeline((ActivityImpl) plan.getPlanElements().get(i-1), (ActivityImpl) plan.getPlanElements().get(i+1))) {
						return true;
					}
				}
				else if (leg.getRoute() instanceof NetworkRouteWRefs) {
					List<Link> links = ((NetworkRouteWRefs) leg.getRoute()).getLinks();
					if (links.size() == 0) {
						if (judgeByBeeline((ActivityImpl) plan.getPlanElements().get(i-1), (ActivityImpl) plan.getPlanElements().get(i+1))) {
							return true;
						}
					}
					else {
						for (Link link : links) {
							if (this.areaOfInterest.containsKey(link.getId())) return true;
						}
						// test departure link
						Link link = ((ActivityImpl) plan.getPlanElements().get(i-1)).getLink();
						if (link != null) {
							if (this.areaOfInterest.containsKey(link.getId())) return true;
						}
						// test arrival link
						link = ((ActivityImpl) plan.getPlanElements().get(i+1)).getLink();
						if (link != null) {
							if (this.areaOfInterest.containsKey(link.getId())) return true;
						}
					}
				}
				else { // leg.getRoute() instanceof GenericRoute
					if (judgeByBeeline((ActivityImpl) plan.getPlanElements().get(i-1), (ActivityImpl) plan.getPlanElements().get(i+1))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean judgeByBeeline(final ActivityImpl fromAct, final ActivityImpl toAct) {
		if (this.aoiCenter == null) {
			// we cannot use the bee-line decision if we don't know the alternative aoi-center
			return false;
		}
		Coord fromCoord = fromAct.getCoord();
		Coord toCoord = toAct.getCoord();

		if (fromCoord == null) {
			fromCoord = fromAct.getLink().getCoord();
		}
		if (toCoord == null) {
			toCoord = toAct.getLink().getCoord();
		}

		return (WorldUtils.distancePointLinesegment(fromCoord, toCoord, this.aoiCenter) <= this.aoiRadius);
	}

}
