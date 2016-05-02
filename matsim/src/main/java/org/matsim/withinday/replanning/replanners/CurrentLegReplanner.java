/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentLegReplanner.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;
import org.matsim.withinday.utils.EditRoutes;

/*
 * The CurrentLegReplanner can be used while an Agent travels from
 * one Activity to another one.
 *
 * MATSim Routers use Plan as Input Data. To be able to use them, we have to create
 * a new Plan from the current Position to the location of the next Activity.
 *
 * This Replanner is called, if a person is somewhere on a Route between two Activities.
 * First the current Route is splitted into two parts - the already passed links and
 * the ones which are still to go.
 * Next a new Plan is created with an Activity at the current Position and an Endposition
 * that is equal to the one from the original plan.
 * This Plan is handed over to the Router and finally the new route is merged with the
 * Links that already have been passed by the Person.
 */
public class CurrentLegReplanner extends WithinDayDuringLegReplanner {

	private final LeastCostPathCalculator pathCalculator;
	private RouteFactoryImpl modeRouteFactory;
	
	/*package*/ CurrentLegReplanner(Id<WithinDayReplanner> id, Scenario scenario, ActivityEndRescheduler internalInterface, 
			LeastCostPathCalculator pathCalculator, RouteFactoryImpl modeRouteFactory) {
		super(id, scenario, internalInterface);
		this.pathCalculator = pathCalculator;
		this.modeRouteFactory = modeRouteFactory;
	}

	/*
	 * Replan Route every time the End of a Link is reached.
	 *
	 * Idea:
	 * - create a new Activity at the current Location
	 * - create a new Route from the current Location to the Destination
	 * - merge already passed parts of the current Route with the new created Route
	 */
	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {

		Plan executedPlan = WithinDayAgentUtils.getModifiablePlan(withinDayAgent);

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		PlanElement currentPlanElement = WithinDayAgentUtils.getCurrentPlanElement(withinDayAgent);
		if (!(currentPlanElement instanceof Leg)) return false;
		Leg currentLeg = (Leg) currentPlanElement;
		int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent);

		EditRoutes editRoutes = new EditRoutes( scenario.getNetwork(), pathCalculator, modeRouteFactory ) ;

		// new Route for current Leg
//		EditRoutes.replanCurrentLegRoute(currentLeg, executedPlan.getPerson(), currentLinkIndex, this.time, 
//				scenario.getNetwork(), tripRouter);
		editRoutes.replanCurrentLegRoute(currentLeg, executedPlan.getPerson(), currentLinkIndex, this.time ) ;
		

		// Finally reset the cached Values of the PersonAgent - they may have changed!
		WithinDayAgentUtils.resetCaches(withinDayAgent);

		return true;
	}

}