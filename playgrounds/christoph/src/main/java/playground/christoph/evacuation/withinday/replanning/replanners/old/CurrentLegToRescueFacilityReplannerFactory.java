/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentLegToRescueFacilityReplannerFactory.java
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

package playground.christoph.evacuation.withinday.replanning.replanners.old;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.TripRouter;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import javax.inject.Provider;

public class CurrentLegToRescueFacilityReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private final Scenario scenario;
	private final Provider<TripRouter> tripRouterFactory;
	private final RoutingContext routingContext;
	
	public CurrentLegToRescueFacilityReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine,
													  Provider<TripRouter> tripRouterFactory, RoutingContext routingContext) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.tripRouterFactory = tripRouterFactory;
		this.routingContext = routingContext;
	}

	@Override
	public WithinDayDuringLegReplanner createReplanner() {
		WithinDayDuringLegReplanner replanner = new CurrentLegToRescueFacilityReplanner(super.getId(), 
				this.scenario, this.getWithinDayEngine().getActivityRescheduler(),
				this.tripRouterFactory.get());
		return replanner;
	}
}