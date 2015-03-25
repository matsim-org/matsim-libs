/* *********************************************************************** *
 * project: org.matsim.*
 * RandomSearchReplannerFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.garageParkingSearchNoInfo;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import playground.wrashid.parkingSearch.withindayFW.core.ParkingAgentsTracker;

public class GPSNIReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private final Scenario scenario;
	private final ParkingAgentsTracker parkingAgentsTracker;
	private final TripRouterFactory tripRouterFactory;
	private final RoutingContext routingContext;
	
	public GPSNIReplannerFactory(WithinDayEngine withinDayEngine, Scenario scenario, ParkingAgentsTracker parkingAgentsTracker,
			TripRouterFactory tripRouterFactory, RoutingContext routingContext) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.tripRouterFactory = tripRouterFactory;
		this.routingContext = routingContext;
	}

	@Override
	public GPSNIReplanner createReplanner() {
		GPSNIReplanner replanner = new GPSNIReplanner(super.getId(), this.scenario, 
				this.getWithinDayEngine().getActivityRescheduler(), this.parkingAgentsTracker,
				this.tripRouterFactory.instantiateAndConfigureTripRouter(this.routingContext));
		return replanner;
	}

}
