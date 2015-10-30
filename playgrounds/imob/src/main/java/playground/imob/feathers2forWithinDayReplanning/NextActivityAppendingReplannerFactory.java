/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.imob.feathers2forWithinDayReplanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.TripRouter;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;

import javax.inject.Provider;

/**
 * @author nagel
 *
 */
final class NextActivityAppendingReplannerFactory extends WithinDayDuringActivityReplannerFactory {
	private final Scenario scenario;
	private final Provider<TripRouter> tripRouterFactory;

	NextActivityAppendingReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine,
										  Provider<TripRouter> tripRouterFactory) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.tripRouterFactory = tripRouterFactory;
	}

	@Override
	public WithinDayDuringActivityReplanner createReplanner() {
		WithinDayDuringActivityReplanner replanner = new NextActivityAppendingReplanner(super.getId(), scenario, 
				this.getWithinDayEngine().getActivityRescheduler(),
				this.tripRouterFactory.get());
		return replanner;
	}

}
