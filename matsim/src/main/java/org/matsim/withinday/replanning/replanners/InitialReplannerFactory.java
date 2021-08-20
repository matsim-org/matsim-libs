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
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.time_interpreter.TimeInterpreter;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplannerFactory;

import javax.inject.Provider;

public class InitialReplannerFactory extends WithinDayInitialReplannerFactory {

	private final Scenario scenario;
	private final Provider<TripRouter> tripRouterFactory;
	private final TimeInterpreter.Factory timeInterpreterFactory;

	public InitialReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine,
								   Provider<TripRouter> tripRouterFactory, TimeInterpreter.Factory timeInterpreterFactory) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.tripRouterFactory = tripRouterFactory;
		this.timeInterpreterFactory = timeInterpreterFactory;
	}

	@Override
	public WithinDayInitialReplanner createReplanner() {
		WithinDayInitialReplanner replanner = new InitialReplanner(super.getId(), scenario, 
				this.getWithinDayEngine().getActivityRescheduler(),
				new PlanRouter(this.tripRouterFactory.get(),
						this.scenario.getActivityFacilities(), timeInterpreterFactory));
		return replanner;
	}

}
