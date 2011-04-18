/* *********************************************************************** *
 * project: org.matsim.*
 * PersonDeleteNonKtiCompatibleRoutes.java
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

package herbie.running.population.algorithms;

import herbie.running.router.KtiPtRoute;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;


/**
 * Deletes all routes from a plans file which are not compatible with the routes used in the kti runs.
 *
 * @author meisterk
 *
 */
public class PersonDeleteNonKtiCompatibleRoutes extends AbstractPersonAlgorithm {

	@Override
	public void run(Person person) {

		/*
		 * Modify routes as follows:
		 * - Delete all "pt" routes independent of whether they are valid kti pt routes or not.
		 * -- Delete all non-kti pt routes because they are invalid for this scenario (e.g. they might survive the evolutionary optimization
		 * due to lower costs)
		 * -- Delete all kti pt routes because reading them in is still much too slow. Initial rerouting is ok because it is not time-dependent.
		 * (TODO) consider integration of PlansCalcRouteKti(Info) into org.matsim scenario
		 * - Set new distance of car routes because the value is probably not correct (see KtiNodeNetworkRouteImpl).
		 */
		for (Plan plan : person.getPlans()) {
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (leg.getMode().equals(TransportMode.pt)) {
						if (!(leg.getRoute() instanceof KtiPtRoute)) {
							leg.setRoute(null);
						}
//						leg.setRoute(null);
					} else if (leg.getMode().equals(TransportMode.car)) {
						if (leg.getRoute() != null) {
							leg.getRoute().setDistance(Double.NaN);
							leg.getRoute().setDistance(leg.getRoute().getDistance());
						}
					} else {
						// do nothing
					}
				}
			}
		}

	}

}
