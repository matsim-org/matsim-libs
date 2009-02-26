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

import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.PersonAlgorithm;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.utils.WorldUtils;

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

	private final Map<Id, Link> areaOfInterest;
	private Coord aoiCenter = null;
	private double aoiRadius = 0.0;

	public PersonIntersectAreaFilter(final PersonAlgorithm nextAlgorithm, final Map<Id, Link> areaOfInterest) {
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
	public boolean judge(final Person person) {
		List<Plan> plans = person.getPlans();
		for (Plan plan : plans) {
			for (int i = 1, n = plan.getActsLegs().size(); i < n; i+=2) {
				Leg leg = (Leg) plan.getActsLegs().get(i);
				if (leg.getRoute() == null) {
					if (judgeByBeeline((Act) plan.getActsLegs().get(i-1), (Act) plan.getActsLegs().get(i+1))) {
						return true;
					}
				} else {
					List<Link> links = ((CarRoute) leg.getRoute()).getLinks();
					if (links.size() == 0) {
						if (judgeByBeeline((Act) plan.getActsLegs().get(i-1), (Act) plan.getActsLegs().get(i+1))) {
							return true;
						}
					} else {
						for (Link link : links) {
							if (this.areaOfInterest.containsKey(link.getId())) return true;
						}
						// test departure link
						Link link = ((Act) plan.getActsLegs().get(i-1)).getLink();
						if (link != null) {
							if (this.areaOfInterest.containsKey(link.getId())) return true;
						}
						// test arrival link
						link = ((Act) plan.getActsLegs().get(i+1)).getLink();
						if (link != null) {
							if (this.areaOfInterest.containsKey(link.getId())) return true;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean judgeByBeeline(final Act fromAct, final Act toAct) {
		if (this.aoiCenter == null) {
			// we cannot use the bee-line decision if we don't know the alternative aoi-center
			return false;
		}
		Coord fromCoord = fromAct.getCoord();
		Coord toCoord = toAct.getCoord();

		if (fromCoord == null) {
			fromCoord = fromAct.getLink().getCenter();
		}
		if (toCoord == null) {
			toCoord = toAct.getLink().getCenter();
		}

		return (WorldUtils.distancePointLinesegment(fromCoord, toCoord, this.aoiCenter) <= this.aoiRadius);
	}

}
