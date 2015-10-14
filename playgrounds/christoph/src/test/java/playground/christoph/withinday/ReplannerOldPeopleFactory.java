/* *********************************************************************** *
 * project: org.matsim.*
 * ReplannerOldPeopleFactory.java
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

package playground.christoph.withinday;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.TripRouter;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;

import javax.inject.Provider;

public class ReplannerOldPeopleFactory extends WithinDayDuringActivityReplannerFactory {

	private final Scenario scenario;
	private final Provider<TripRouter> tripRouterFactory;
	private final RoutingContext routingContext;
	
	public ReplannerOldPeopleFactory(Scenario scenario, WithinDayEngine withinDayEngine,
									 Provider<TripRouter> tripRouterFactory, RoutingContext routingContext) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.tripRouterFactory = tripRouterFactory;
		this.routingContext = routingContext;
	}

	@Override
	public WithinDayDuringActivityReplanner createReplanner() {
		WithinDayDuringActivityReplanner replanner = new ReplannerOldPeople(super.getId(), this.scenario,
				this.getWithinDayEngine().getActivityRescheduler(),
				this.tripRouterFactory.get());
		return replanner;
	}

}