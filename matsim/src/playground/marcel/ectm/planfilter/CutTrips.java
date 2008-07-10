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

import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
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

		for (int i = 1, n = plan.getActsLegs().size(); i < n; i+=2) {
			Leg leg = (Leg) plan.getActsLegs().get(i);
			if (leg.getRoute() == null) {
				throw new RuntimeException("route is null. person=" + person.getId().toString());
			}

			Link depLink = ((Act) plan.getActsLegs().get(i-1)).getLink();
			Link arrLink = ((Act) plan.getActsLegs().get(i+1)).getLink();

			// start at departure link
			if (this.aoi.containsKey(depLink.getId())) {
				boolean leftAOI = false;
				// start inside AOI
				Link lastInsideLink = depLink;
				Link[] links = leg.getRoute().getLinkRoute();
				for (Link link : links) {
					if (this.aoi.containsKey(link.getId())) {
						// still inside, remember link
						lastInsideLink = link;
					} else {
						// we're leaving the aoi, shorten this route and delete all legs afterwards
						leftAOI = true;
						break; // step out of this for-loop
					}
				}
				if (leftAOI || !this.aoi.containsKey(arrLink)) {
					// shorten route such that lastInsideLink is no longer part of it
					// TODO

					// move act-location to lastInsideLink
					// TODO

					// remove the route from all legs after that one, and move all following activities to the lastInsideLink
					// TODO

					break; // step out of for-loop for legs
				}

			} else {
				boolean enteredAOI = false;
				boolean leftAOIagain = false;
				// start outside AOI
				Link lastOutsideLink = depLink;
				Link firstInsideLink = null;
				Link[] links = leg.getRoute().getLinkRoute();
				for (Link link : links) {
					if (!this.aoi.containsKey(link.getId())) {
						if (enteredAOI) {
							// we're out again
							leftAOIagain = true;
							break; // step out of this for-loop
						}
						// still inside, remember link
						lastOutsideLink = link;
					} else {
						if (!enteredAOI) {
							// we're entering the aoi
							enteredAOI = true;
							firstInsideLink = link;
						}
					}
				}
				if (!this.aoi.containsKey(arrLink)) {
					if (enteredAOI) {
						leftAOIagain = true;
					}
				}
				if (enteredAOI) {
					// move from-act to firstInsideLink
					// TODO

					// shorten route such that all links up to firstInsideLink are no longer part of it
					// TODO

					if (leftAOIagain) {
						// shorten route such that lastInsideLink is no longer part of it
						// TODO

						// move to-act-location to lastInsideLink
						// TODO

						// remove all legs after that one
						// TODO

						break; // step out of for-loop for legs
					}
				} else {
					// route never entered the aoi
					// remove this leg completely
					// or remove route and move from-act to the firstInsideLink of a following route
					// TODO
				}
			}
		}

		if (plan.getActsLegs().size() > 2) {
			// there is still at least one leg in that plan
			this.nextAlgorithm.run(person);
		}
	}
}
