/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.eventsBasedPTRouter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.contrib.eventsBasedPTRouter.vehicleOccupancy.VehicleOccupancy;

/**
 * Factory for the variable transit router
 * 
 * @author sergioo
 */
public class TransitRouterEventsWSVFactory implements TransitRouterFactory {

	private final TransitRouterConfig config;
	private final TransitRouterNetworkWW routerNetwork;
	private final Scenario scenario;
	private final WaitTime waitTime;
	private final StopStopTime stopStopTime;
	private final VehicleOccupancy vehicleOccupancy;
	
	public TransitRouterEventsWSVFactory(final Scenario scenario, final WaitTime waitTime, final StopStopTime stopStopTime, final VehicleOccupancy vehicleOccupancy) {
		this.config = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
				scenario.getConfig().vspExperimental());
		routerNetwork = TransitRouterNetworkWW.createFromSchedule(scenario.getNetwork(), scenario.getTransitSchedule(), this.config.beelineWalkConnectionDistance);
		this.scenario = scenario;
		this.waitTime = waitTime;
		this.stopStopTime = stopStopTime;
		this.vehicleOccupancy = vehicleOccupancy;
	}
	@Override
	public TransitRouter createTransitRouter() {
		return new TransitRouterVariableImpl(config, new TransitRouterNetworkTravelTimeAndDisutilityWSV(config, routerNetwork, waitTime, stopStopTime, vehicleOccupancy, scenario.getConfig().travelTimeCalculator(), scenario.getConfig().qsim(), new PreparedTransitSchedule(scenario.getTransitSchedule())), routerNetwork);
	}

}
