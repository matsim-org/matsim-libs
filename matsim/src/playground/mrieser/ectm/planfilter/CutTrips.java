/* *********************************************************************** *
 * project: org.matsim.*
 * CutTrips.java
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonAlgorithm;

public class CutTrips implements PersonAlgorithm {

	private final PersonAlgorithm nextAlgorithm;
	private final Map<Id, Link> aoi;

	public CutTrips(final PersonAlgorithm nextAlgorithm, final Map<Id, Link> aoi) {
		this.nextAlgorithm = nextAlgorithm;
		this.aoi = aoi;
	}

	public void run(final Person person) {
		final List<? extends Plan> plans = person.getPlans();
		if (plans.size() != 1) {
			throw new RuntimeException("only 1 plan per person supported!");
		}
		final Plan plan = plans.get(0);

		/* First, we will go through all links of all legs and remember when is the
		 * first time we enter the AOI, and when we leave it again. After we leave
		 * it, we can break out of the loop. Additionally, we try to figure out the
		 * exact times we enter/leave the AOI.
		 */

		int firstInsideLeg = -1;
		Link firstInsideLink = null;
		Link lastInsideLink = null;
		int firstOutsideLeg = -1;
		Link firstOutsideLink = null;

		boolean startInside = this.aoi.containsKey(((ActivityImpl) plan.getPlanElements().get(0)).getLink().getId());

		for (int legNr = 1, n = plan.getPlanElements().size(); legNr < n; legNr += 2) {
			LegImpl leg = (LegImpl) plan.getPlanElements().get(legNr);
			if (leg.getRoute() == null) {
				throw new RuntimeException("route is null. person=" + person.getId().toString());
			}

			Link depLink = ((ActivityImpl) plan.getPlanElements().get(legNr - 1)).getLink();
			Link arrLink = ((ActivityImpl) plan.getPlanElements().get(legNr + 1)).getLink();

			// test departure link
			if (this.aoi.containsKey(depLink.getId())) {
				if (firstInsideLink == null) {
					firstInsideLink = depLink;
					firstInsideLeg = legNr;
				}
				lastInsideLink = depLink;
			}
			if (firstInsideLink != null && !this.aoi.containsKey(depLink.getId())) {
				firstOutsideLink = depLink;
				firstOutsideLeg = legNr;
				break;
			}

			// test links of route
			for (Link link : ((NetworkRouteWRefs) leg.getRoute()).getLinks()) {
				if (this.aoi.containsKey(link.getId())) {
					if (firstInsideLink == null) {
						firstInsideLink = link;
						firstInsideLeg = legNr;
					}
					if (firstOutsideLink == null) {
						lastInsideLink = link;
					}
				}
				if (firstInsideLink != null && !this.aoi.containsKey(link.getId())) {
					firstOutsideLink = link;
					firstOutsideLeg = legNr;
					break;
				}
			}

			// test arrival link
			if (this.aoi.containsKey(arrLink.getId())) {
				if (firstInsideLink == null) {
					firstInsideLink = arrLink;
					firstInsideLeg = legNr;
				}
				if (firstOutsideLink == null) {
					lastInsideLink = arrLink;
				}
			}
			if (firstInsideLink != null && firstOutsideLink == null && !this.aoi.containsKey(arrLink.getId())) {
				firstOutsideLink = arrLink;
				firstOutsideLeg = legNr;
				break;
			}

			if (firstOutsideLink != null) {
				break;
			}
		}

		/* In the second step, we move all activities before firstInsideLeg to the
		 * firstInsideLink, and move all activities after firstOutsideLeg to the
		 * firstOutsideLink. Additionally, we have to adapt the routes of
		 * firstInsideLeg and firstOutsideLeg as their from or to activity might
		 * have changed its location.
		 */

		if (startInside && (firstOutsideLink == null)) {
			// agent is never outside the area, so call the next algo and that's it
			this.nextAlgorithm.run(person);
			return;
		}

		if (!startInside) {
			// move all acts before firstInsideLeg to firstInsideLink
			for (int actNr = 0; actNr < firstInsideLeg; actNr += 2) {
				ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(actNr);
				act.setLink(firstInsideLink);
			}
			// remove all routes from legs before firstInsideLeg, as they are now all at the same location
			for (int legNr = 1; legNr < firstInsideLeg; legNr += 2) {
				LegImpl leg = (LegImpl) plan.getPlanElements().get(legNr);
				leg.setRoute(null);
			}
			// find the time the agent is entering the AOI, and use that time as from-act endtime
			LegImpl leg = (LegImpl) plan.getPlanElements().get(firstInsideLeg);
			NetworkRouteWRefs route = (NetworkRouteWRefs) leg.getRoute();
			double traveltime = 0.0;
			for (Link link : route.getLinks()) {
				traveltime += link.getLength()/link.getFreespeed(Time.UNDEFINED_TIME);
				if (link.equals(firstInsideLink)) {
					break;
				}
			}
			ActivityImpl fromAct = (ActivityImpl) plan.getPlanElements().get(firstInsideLeg - 1);
			if (fromAct.getDuration() != Time.UNDEFINED_TIME) {
				fromAct.setDuration(fromAct.getDuration() + traveltime);
			}
			if (fromAct.getEndTime() != Time.UNDEFINED_TIME) {
				fromAct.setEndTime(fromAct.getEndTime() + traveltime);
			}
			if (leg.getDepartureTime() != Time.UNDEFINED_TIME) {
				leg.setDepartureTime(leg.getDepartureTime() + traveltime);
			}

			// adapt route of the leg that leads into the AOI
			List<Node> nodes = route.getNodes();
			Iterator<Node> iter = nodes.iterator();
			while (iter.hasNext()) {
				Node node = iter.next();
				if (node.equals(firstInsideLink.getToNode())) {
					break;
				}
				iter.remove();
			}
		}

		// move all acts after firstOutsideLeg to lastInsideLink
		for (int actNr = firstOutsideLeg+1; actNr < plan.getPlanElements().size(); actNr += 2) {
			ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(actNr);
			act.setLink(lastInsideLink);
		}

		if (firstOutsideLink != null) {
			// remove all routes from legs after firstOutsideLeg, as they are now all at the same location
			for (int legNr = firstOutsideLeg + 2; legNr < plan.getPlanElements().size(); legNr += 2) {
				LegImpl leg = (LegImpl) plan.getPlanElements().get(legNr);
				leg.setRoute(null);
			}

			// adapt route of leg that leads out of the AOI
			boolean removing = false;
			LegImpl leg = (LegImpl) plan.getPlanElements().get(firstOutsideLeg);
			NetworkRouteWRefs route = (NetworkRouteWRefs) leg.getRoute();
			List<Node> nodes = route.getNodes();
			Iterator<Node> iter = nodes.iterator();
			while (iter.hasNext()) {
				Node node = iter.next();
				if (node.equals(firstOutsideLink.getFromNode())) {
					removing = true;
				}
				if (removing) {
					iter.remove();
				}
			}
		}

		this.nextAlgorithm.run(person);
	}

}
