/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingRouterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.parking.withinday.utils;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;

public class ParkingRouterFactory {

	private final Scenario scenario;
	private final Map<String, TravelTime> travelTimes;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final TripRouterFactory tripRouterFactory;
	private final int nodesToCheck;

	public ParkingRouterFactory(Scenario scenario, Map<String, TravelTime> travelTimes, 
			TravelDisutilityFactory travelDisutilityFactory,
			TripRouterFactory tripRouterFactory, int nodesToCheck) {
		this.scenario = scenario;
		this.travelTimes = travelTimes;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.tripRouterFactory = tripRouterFactory;
		this.nodesToCheck = nodesToCheck;
	}
	
	public ParkingRouter createParkingRouter() {
		RoutingContext routingContext = new RoutingContextImpl(this.travelDisutilityFactory, 
				this.travelTimes.get(TransportMode.car), this.scenario.getConfig().planCalcScore());
		return new ParkingRouter(scenario, travelTimes, travelDisutilityFactory, 
				tripRouterFactory.instantiateAndConfigureTripRouter(routingContext), nodesToCheck);
	}
}
