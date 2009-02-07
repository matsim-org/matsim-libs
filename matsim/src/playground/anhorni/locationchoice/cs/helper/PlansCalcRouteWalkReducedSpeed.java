/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.cs.helper;

import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.util.LeastCostPathCalculator;

public class PlansCalcRouteWalkReducedSpeed extends PlansCalcRoute {

	public PlansCalcRouteWalkReducedSpeed(LeastCostPathCalculator router,
			LeastCostPathCalculator routerFreeflow) {
		super(router, routerFreeflow);
	}

	public double handleWalkLeg(final Leg leg, final Act fromAct, final Act toAct, final double depTime) {
		// make simple assumption about distance and walking speed
		double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
		double speed = 4.0 / 3.6; // 4.0 km/h --> m/s
		// create an empty route, but with realistic travel time
		CarRoute route = new NodeCarRoute(fromAct.getLink(), toAct.getLink());
		int travTime = (int)(dist / speed);
		route.setTravelTime(travTime);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}
}
