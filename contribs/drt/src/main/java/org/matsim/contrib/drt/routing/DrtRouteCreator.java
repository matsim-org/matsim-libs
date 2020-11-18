/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.DefaultMainLegRouter;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.name.Named;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 * @author Kai Nagel
 */
public class DrtRouteCreator implements DefaultMainLegRouter.RouteCreator {
	private final DrtConfigGroup drtCfg;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;

	public DrtRouteCreator(DrtConfigGroup drtCfg, Network modalNetwork,
			LeastCostPathCalculatorFactory leastCostPathCalculatorFactory,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			TravelDisutilityFactory travelDisutilityFactory) {
		this.drtCfg = drtCfg;
		this.travelTime = travelTime;
		router = leastCostPathCalculatorFactory.createPathCalculator(modalNetwork,
				travelDisutilityFactory.createTravelDisutility(travelTime), travelTime);
	}

	/**
	 * Calculates the maximum travel time defined as: drtCfg.getMaxTravelTimeAlpha() * unsharedRideTime + drtCfg.getMaxTravelTimeBeta()
	 *
	 * @param drtCfg
	 * @param unsharedRideTime ride time of the direct (shortest-time) route
	 * @return maximum travel time
	 */
	static double getMaxTravelTime(DrtConfigGroup drtCfg, double unsharedRideTime) {
		return drtCfg.getMaxTravelTimeAlpha() * unsharedRideTime + drtCfg.getMaxTravelTimeBeta();
	}

	public Route createRoute(double departureTime, Link accessActLink, Link egressActLink,
			RouteFactories routeFactories) {
		VrpPathWithTravelData unsharedPath = VrpPaths.calcAndCreatePath(accessActLink, egressActLink, departureTime,
				router, travelTime);
		double unsharedRideTime = unsharedPath.getTravelTime();//includes first & last link
		double maxTravelTime = getMaxTravelTime(drtCfg, unsharedRideTime);
		double unsharedDistance = VrpPaths.calcDistance(unsharedPath);//includes last link

		DrtRoute route = routeFactories.createRoute(DrtRoute.class, accessActLink.getId(), egressActLink.getId());
		route.setDistance(unsharedDistance);
		route.setTravelTime(maxTravelTime);
		route.setDirectRideTime(unsharedRideTime);
		route.setMaxWaitTime(drtCfg.getMaxWaitTime());
		return route;
	}
}
