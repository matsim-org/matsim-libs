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
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class ParkingRouterFactory {

	private final Scenario scenario;
	private final Map<String, TravelTime> travelTimes;
	private final TravelDisutility travelDisutility;
	private final TripRouterFactory tripRouterFactory;
	private final int nodesToCheck;

	public ParkingRouterFactory(Scenario scenario, Map<String, TravelTime> travelTimes, TravelDisutility travelDisutility,
			TripRouterFactory tripRouterFactory, int nodesToCheck) {
		this.scenario = scenario;
		this.travelTimes = travelTimes;
		this.travelDisutility = travelDisutility;
		this.tripRouterFactory = tripRouterFactory;
		this.nodesToCheck = nodesToCheck;
	}
	
	public ParkingRouter createParkingRouter() {
		return new ParkingRouter(scenario, travelTimes, travelDisutility, tripRouterFactory.createTripRouter(), nodesToCheck);
	}
}
