/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingSearchReplannerFactory.java
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

package playground.christoph.parking.withinday.replanner;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.christoph.parking.withinday.utils.ParkingRouter;
import playground.christoph.parking.withinday.utils.ParkingRouterFactory;

public class ParkingSearchReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private final Scenario scenario;
	private final ParkingAgentsTracker parkingAgentsTracker;
	private final ParkingInfrastructure parkingInfrastructure;
	private final ParkingRouterFactory parkingRouterFactory;
	
	public ParkingSearchReplannerFactory(WithinDayEngine withindayDayEngine, AbstractMultithreadedModule abstractMultithreadedModule,
			double replanningProbability, Scenario scenario, ParkingAgentsTracker parkingAgentsTracker, 
			ParkingInfrastructure parkingInfrastructure, ParkingRouterFactory parkingRouterFactory) {
		super(withindayDayEngine, abstractMultithreadedModule, replanningProbability);
		
		this.scenario = scenario;
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.parkingInfrastructure = parkingInfrastructure;
		this.parkingRouterFactory = parkingRouterFactory;
	}

	@Override
	public ParkingSearchReplanner createReplanner() {
		ParkingRouter parkingRouter = this.parkingRouterFactory.createParkingRouter();
		ParkingSearchReplanner replanner = new ParkingSearchReplanner(super.getId(), scenario, 
				this.getReplanningManager().getInternalInterface(), parkingAgentsTracker, parkingInfrastructure, parkingRouter);
		super.initNewInstance(replanner);
		return replanner;
	}

}