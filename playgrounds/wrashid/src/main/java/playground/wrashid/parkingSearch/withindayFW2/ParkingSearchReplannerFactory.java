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

package playground.wrashid.parkingSearch.withindayFW2;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.TripRouter;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import javax.inject.Provider;

public class ParkingSearchReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private final Scenario scenario;
	private final ParkingAgentsTracker parkingAgentsTracker;
	private final Provider<TripRouter> tripRouterFactory;

	public ParkingSearchReplannerFactory(WithinDayEngine withinDayEngine, Scenario scenario, ParkingAgentsTracker parkingAgentsTracker, Provider<TripRouter> withinDayTripRouterFactory) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.tripRouterFactory = withinDayTripRouterFactory;
	}

	@Override
	public ParkingSearchReplanner createReplanner() {
		ParkingSearchReplanner replanner = new ParkingSearchReplanner(super.getId(), this.scenario, 
				this.getWithinDayEngine().getActivityRescheduler(), 
				this.tripRouterFactory.get(), this.parkingAgentsTracker);
		return replanner;
	}

}
