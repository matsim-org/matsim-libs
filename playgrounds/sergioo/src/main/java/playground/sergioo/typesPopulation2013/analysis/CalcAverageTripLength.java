/* *********************************************************************** *
 * project: org.matsim.*
 * CalcAverageTripLength.java
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

package playground.sergioo.typesPopulation2013.analysis;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Calculates the average trip length of all routes in a plan. The trip length
 * is the sum of the length of all links, including the route's end link, but
 * <em>not</em> including the route's start link.
 *
 * @author mrieser
 */
public class CalcAverageTripLength extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private double sumLength = 0.0;
	private int cntTrips = 0;
	private final Network network;

	public CalcAverageTripLength(final Network network) {
		this.network = network;
	}

	@Override
	public void run(final Person person) {
		this.run(person.getSelectedPlan());
	}

	@Override
	public void run(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				Route route = leg.getRoute();
				if (route != null) {
					double dist = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) route, this.network);
					if (route.getEndLinkId() != null && route.getStartLinkId() != route.getEndLinkId()) {
						dist += this.network.getLinks().get(route.getEndLinkId()).getLength();
					}
					this.sumLength += dist;
					this.cntTrips++;
				}
			}
		}
	}

	public double getAverageTripLength() {
		if (this.cntTrips == 0) {
			return 0;
		}
		return (this.sumLength / this.cntTrips);
	}
}
