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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.passenger.DvrpLoadFromTrip;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.DefaultMainLegRouter;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 * @author Kai Nagel
 * @author Sebastian Hörl, IRT SystemX
 */
public class DrtRouteCreator implements DefaultMainLegRouter.RouteCreator {
	private final DrtConfigGroup drtCfg;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;

	private final DrtRouteConstraintsCalculator routeConstraintsCalculator;
	private final DvrpLoadFromTrip loadFromPerson;
	private final DvrpLoadType loadType;

	public DrtRouteCreator(DrtConfigGroup drtCfg, Network modalNetwork,
                           LeastCostPathCalculatorFactory leastCostPathCalculatorFactory, TravelTime travelTime,
                           TravelDisutilityFactory travelDisutilityFactory,
                           DrtRouteConstraintsCalculator routeConstraintsCalculator,
						   DvrpLoadFromTrip loadCreator, DvrpLoadType loadType) {
		this.drtCfg = drtCfg;
		this.travelTime = travelTime;
        this.routeConstraintsCalculator = routeConstraintsCalculator;
		this.loadFromPerson = loadCreator;
		this.loadType = loadType;
        router = leastCostPathCalculatorFactory.createPathCalculator(modalNetwork,
				travelDisutilityFactory.createTravelDisutility(travelTime), travelTime);
	}



	public Route createRoute(double departureTime, Link accessActLink, Link egressActLink, Person person,
			Attributes tripAttributes, RouteFactories routeFactories) {
		VrpPathWithTravelData unsharedPath = VrpPaths.calcAndCreatePath(accessActLink, egressActLink, departureTime,
				router, travelTime);
		double unsharedRideTime = unsharedPath.getTravelTime();//includes first & last link
		double unsharedDistance = VrpPaths.calcDistance(unsharedPath);//includes last link

		DrtRouteConstraints constraints = routeConstraintsCalculator.calculateRouteConstraints(departureTime, accessActLink, egressActLink, person,
				tripAttributes, unsharedRideTime, unsharedDistance);

		DrtRoute route = routeFactories.createRoute(DrtRoute.class, accessActLink.getId(), egressActLink.getId());
		route.setDistance(unsharedDistance);
		route.setTravelTime(constraints.maxTravelTime());
		route.setMaxRideTime(constraints.maxRideTime());
		route.setDirectRideTime(unsharedRideTime);
		route.setMaxWaitTime(constraints.maxWaitTime());

		DvrpLoad load = loadFromPerson.getLoad(person, tripAttributes);
		route.setLoad(load, loadType);

		if (this.drtCfg.storeUnsharedPath) {
			route.setUnsharedPath(unsharedPath);
		}

		return route;
	}
}