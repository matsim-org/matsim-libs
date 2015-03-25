/* *********************************************************************** *
 * project: org.matsim.*
 * InitialReplannerFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.withinday.replanning.replanners;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplannerFactory;

public class InitialReplannerFactory extends WithinDayInitialReplannerFactory {

	private final Scenario scenario;
	private final TripRouterFactory tripRouterFactory;
	private final RoutingContext routingContext;
	
	public InitialReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine,
			TripRouterFactory tripRouterFactory, RoutingContext routingContext) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.tripRouterFactory = tripRouterFactory;
		this.routingContext = routingContext;
	}

	@Override
	public WithinDayInitialReplanner createReplanner() {
		WithinDayInitialReplanner replanner = new InitialReplanner(super.getId(), scenario, 
				this.getWithinDayEngine().getActivityRescheduler(),
				new PlanRouter(this.tripRouterFactory.instantiateAndConfigureTripRouter(routingContext), 
						this.scenario.getActivityFacilities()));
		return replanner;
	}

}
