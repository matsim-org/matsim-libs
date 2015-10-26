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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Provider;

public class ParkingRouterFactory {

	private final Scenario scenario;
	private final TravelTime carTravelTime;
	private final TravelTime walkTravelTime;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final Provider<TripRouter> tripRouterFactory;
	private final int nodesToCheck;

	public ParkingRouterFactory(Scenario scenario, TravelTime carTravelTime, TravelTime walkTravelTime,
			TravelDisutilityFactory travelDisutilityFactory, Provider<TripRouter> tripRouterFactory, int nodesToCheck) {
		this.scenario = scenario;
		this.carTravelTime = carTravelTime;
		this.walkTravelTime = walkTravelTime;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.tripRouterFactory = tripRouterFactory;
		this.nodesToCheck = nodesToCheck;
	}
	
	public ParkingRouter createParkingRouter() {
		TravelDisutility travelDisutility = this.travelDisutilityFactory.createTravelDisutility(carTravelTime, 
				this.scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, carTravelTime);
		return new ParkingRouter(scenario, carTravelTime, walkTravelTime, travelDisutilityFactory, 
				tripRouterFactory.get(), nodesToCheck);
	}
}
