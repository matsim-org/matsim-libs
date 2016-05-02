/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentLegReplannerFactory.java
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
import org.matsim.core.mobsim.qsim.ActivityEndReschedulerProvider;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

public class CurrentLegReplannerFactory extends WithinDayDuringLegReplannerFactory {

	private final Scenario scenario;
	private final LeastCostPathCalculator pathCalculator;
	private RouteFactoryImpl modeRouteFactory;

	public CurrentLegReplannerFactory(Scenario scenario, ActivityEndReschedulerProvider withinDayEngine,
									  LeastCostPathCalculator pathCalculator, RouteFactoryImpl modeRouteFactory) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.pathCalculator = pathCalculator;
		this.modeRouteFactory = modeRouteFactory;
	}

	@Override
	public WithinDayDuringLegReplanner createReplanner() {
		WithinDayDuringLegReplanner replanner = new CurrentLegReplanner(super.getId(), scenario,
				this.getWithinDayEngine().getActivityRescheduler(), 
				this.pathCalculator, modeRouteFactory);
		return replanner;
	}
}