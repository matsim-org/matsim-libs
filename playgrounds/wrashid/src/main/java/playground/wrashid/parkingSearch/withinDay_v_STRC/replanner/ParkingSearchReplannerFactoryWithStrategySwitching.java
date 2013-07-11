/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.wrashid.parkingSearch.withinDay_v_STRC.replanner;

import java.util.LinkedList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.withinday.mobsim.WithinDayEngine;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.withinday.replanner.ParkingSearchReplanner;
import playground.christoph.parking.withinday.replanner.ParkingSearchReplannerFactory;
import playground.christoph.parking.withinday.replanner.strategy.ParkingSearchStrategy;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.christoph.parking.withinday.utils.ParkingRouter;
import playground.christoph.parking.withinday.utils.ParkingRouterFactory;

public class ParkingSearchReplannerFactoryWithStrategySwitching extends ParkingSearchReplannerFactory {

	private LinkedList<ParkingSearchStrategy> strategies;

	public ParkingSearchReplannerFactoryWithStrategySwitching(WithinDayEngine withindayDayEngine,
			Scenario scenario, ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructure parkingInfrastructure,
			ParkingRouterFactory parkingRouterFactory) {
		super(withindayDayEngine, scenario, parkingAgentsTracker, parkingInfrastructure, parkingRouterFactory);
	}

	@Override
	public ParkingSearchReplanner createReplanner() {
		ParkingRouter parkingRouter = this.parkingRouterFactory.createParkingRouter();
		ParkingSearchReplanner replanner = new ParkingSearchReplannerWithStrategySwitching(super.getId(), scenario, 
				this.getWithinDayEngine().getInternalInterface(), parkingAgentsTracker, parkingInfrastructure, parkingRouter, strategies);
		return replanner;
	}


}

