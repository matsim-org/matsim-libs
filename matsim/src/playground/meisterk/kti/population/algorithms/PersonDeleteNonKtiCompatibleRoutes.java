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

package playground.meisterk.kti.population.algorithms;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * Deletes all routes from a plans file which are not compatible with the routes used in the kti runs.
 * 
 * @author meisterk
 *
 */
public class PersonDeleteNonKtiCompatibleRoutes extends AbstractPersonAlgorithm {

	@Override
	public void run(PersonImpl person) {

		/*
		 * Modify routes as follows:
		 * - Delete all "pt" routes independent of whether they are valid kti pt routes or not.
		 * -- Delete all non-kti pt routes because they are invalid for this scenario (e.g. they might survive the evolutionary optimization
		 * due to lower costs)
		 * -- Delete all kti pt routes because reading them in is still much too slow. Initial rerouting is ok because it is not time-dependent.
		 * - Set new distance of car routes because the value is probably not correct (see KtiNodeNetworkRouteImpl).
		 */
		for (PlanImpl plan : person.getPlans()) {
			for (BasicPlanElement pe : plan.getPlanElements()) {
				if (pe instanceof LegImpl) {
					LegImpl leg = (LegImpl) pe;
					if (leg.getMode().equals(TransportMode.pt)) {
						leg.setRoute(null);
					} else if (leg.getMode().equals(TransportMode.car)) {
						// invalidate distance information
						leg.getRoute().setDistance(Double.NaN);
						// set new distance information
						leg.getRoute().setDistance(leg.getRoute().getDistance());
					} else {
						// do nothing
					}
				}
			}
		}
		
	}

}
