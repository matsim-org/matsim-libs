/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.andreas.P2.performance;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

/**
 * 
 * @author aneumann
 *
 */
final class PReRouteStrategyModule implements PlanStrategyModule{
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PReRouteStrategyModule.class);

	private final Scenario scenario;
	private PPlanRouter planRouter;

	public PReRouteStrategyModule(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void handlePlan(Plan plan) {
		this.planRouter.run(plan);
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
		this.planRouter = new PPlanRouter(
				replanningContext.getTripRouter(),
				this.scenario.getActivityFacilities());
	}

}
