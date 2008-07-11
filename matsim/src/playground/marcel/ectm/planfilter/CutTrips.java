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

package playground.marcel.ectm.planfilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.plans.algorithms.PersonAlgorithmI;

public class CutTrips implements PersonAlgorithmI {

	private PersonAlgorithmI nextAlgorithm;
	private final Map<Id, Link> aoi;

	public CutTrips(final PersonAlgorithmI nextAlgorithm, final Map<Id, Link> aoi) {
		this.nextAlgorithm = nextAlgorithm;
		this.aoi = aoi;
	}

	public void run(final Person person) {
		List<Plan> plans = person.getPlans();
		if (plans.size() != 1) {
			throw new RuntimeException("only 1 plan per person supported!");
		}
		Plan plan = plans.get(0);

		/* First, we will go through all links of all legs and remember when is the
		 * first time we enter the AOI, and when we leave it again. After we leave
		 * it, we can break out of the loop. Additionally, we try to figure out the
		 * exact times we enter/leave the AOI.
		 */

		int firstInsideLeg = -1;
		Link firstInsideLink = null;
		int firstOutsideLeg = -1;
		Link firstOutsideLink = null;

		boolean startInside = this.aoi.containsKey(((Act) plan.getActsLegs().get(0)).getLink().getId());

		for (int legNr = 1, n = plan.getActsLegs().size(); legNr < n; legNr += 2) {
			Leg leg = (Leg) plan.getActsLegs().get(legNr);
			if (leg.getRoute() == null) {
				throw new RuntimeException("route is null. person=" + person.getId().toString());
			}

			Link depLink = ((Act) plan.getActsLegs().get(legNr - 1)).getLink();
			Link arrLink = ((Act) plan.getActsLegs().get(legNr + 1)).getLink();

			// test departure link
			if (this.aoi.containsKey(depLink.getId()) && firstInsideLink == null) {
				firstInsideLink = depLink;
				firstInsideLeg = legNr;
			}
			if (firstInsideLink != null && !this.aoi.containsKey(depLink.getId())) {
				firstOutsideLink = depLink;
				firstOutsideLeg = legNr;
				break;
			}

			// test links of route
			Link[] links = leg.getRoute().getLinkRoute();
			for (Link link : links) {
				if (this.aoi.containsKey(link.getId()) && firstInsideLink == null) {
					firstInsideLink = link;
					firstInsideLeg = legNr;
				}
				if (firstInsideLink != null && !this.aoi.containsKey(link.getId())) {
					firstOutsideLink = link;
					firstOutsideLeg = legNr;
					break;
				}
			}

			// test arrival link
			if (this.aoi.containsKey(arrLink.getId()) && firstInsideLink == null) {
				firstInsideLink = arrLink;
				firstInsideLeg = legNr;
			}
			if (firstInsideLink != null && !this.aoi.containsKey(arrLink.getId())) {
				firstOutsideLink = arrLink;
				firstOutsideLeg = legNr;
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
			// agent is never outside the area, nothing to do
			return;
		}

		if (!startInside) {
			// move all acts before firstInsideLeg to firstInsideLink
			for (int actNr = 0; actNr < firstInsideLeg; actNr += 2) {
				Act act = (Act) plan.getActsLegs().get(actNr);
				act.setLink(firstInsideLink);
			}
			// remove all routes from legs before firstInsideLeg, as they are now all at the same location
			for (int legNr = 1; legNr < firstInsideLeg; legNr += 2) {
				Leg leg = (Leg) plan.getActsLegs().get(legNr);
				leg.createRoute("0.0", "00:00:00");
			}
			// adapt route of the leg that leads into the AOI
			Leg leg = (Leg) plan.getActsLegs().get(firstInsideLeg);
			Route route = leg.getRoute();
			ArrayList<Node> nodes = route.getRoute();
			Iterator<Node> iter = nodes.iterator();
			while (iter.hasNext()) {
				Node node = iter.next();
				if (node.equals(firstInsideLink.getToNode())) {
					break;
				}
				iter.remove();
			}
		}

		if (firstOutsideLink != null) {
			// move all acts after firstOutsideLeg to firstOutsideLink
			for (int actNr = firstOutsideLeg+1; actNr < plan.getActsLegs().size(); actNr += 2) {
				Act act = (Act) plan.getActsLegs().get(actNr);
				act.setLink(firstOutsideLink);
			}

			// remove all routes from legs after firstOutsideLeg, as they are now all at the same location
			for (int legNr = firstOutsideLeg + 2; legNr < plan.getActsLegs().size(); legNr += 2) {
				Leg leg = (Leg) plan.getActsLegs().get(legNr);
				leg.createRoute("0.0", "00:00:00");
			}

			// adapt route of leg that leads out of the AOI
			boolean removing = false;
			Leg leg = (Leg) plan.getActsLegs().get(firstInsideLeg);
			Route route = leg.getRoute();
			ArrayList<Node> nodes = route.getRoute();
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
