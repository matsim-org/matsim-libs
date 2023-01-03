/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.google.inject.name.Named;

/**
 * @author Michal Maciejewski (michalm)
 */
public class GenericRouteCreator implements DefaultMainLegRouter.RouteCreator {
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;

	public GenericRouteCreator(LeastCostPathCalculatorFactory leastCostPathCalculatorFactory, Network modalNetwork,
			TravelTime travelTime,
			TravelDisutilityFactory travelDisutilityFactory) {
		this.travelTime = travelTime;
		router = leastCostPathCalculatorFactory.createPathCalculator(modalNetwork,
				travelDisutilityFactory.createTravelDisutility(travelTime), travelTime);
	}

	@Override
	public Route createRoute(double departureTime, Link accessActLink, Link egressActLink,
			Person person, Attributes tripAttributes, RouteFactories routeFactories) {
		VrpPathWithTravelData unsharedPath = VrpPaths.calcAndCreatePath(accessActLink, egressActLink, departureTime,
				router, travelTime);
		double travelTime = unsharedPath.getTravelTime();//includes first & last link
		double distance = VrpPaths.calcDistance(unsharedPath);//includes last link

		Route route = routeFactories.createRoute(GenericRouteImpl.class, accessActLink.getId(), egressActLink.getId());
		route.setDistance(distance);
		route.setTravelTime(travelTime);
		return route;
	}
}
