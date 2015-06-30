/* *********************************************************************** *
 * project: org.matsim.*
 * NextLegReplanner.java
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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;
import org.matsim.withinday.utils.EditRoutes;

/**
 * The NextLegReplanner can be used while an agent is performing an activity. The
 * replanner creates a new trip from the current activity to the next main activity 
 * in the agent's plan.
 * 
 * In fact this should be renamed to NextTripReplanner. cdobler, apr'14
 */
public class NextLegReplanner extends WithinDayDuringActivityReplanner {

	private final TripRouter tripRouter;
	
	/*package*/ NextLegReplanner(Id<WithinDayReplanner> id, Scenario scenario, ActivityEndRescheduler internalInterface, TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.tripRouter = tripRouter;
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {
		
		Plan executedPlan = WithinDayAgentUtils.getModifiablePlan(withinDayAgent);

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		// Get the activity currently performed by the agent as well as the subsequent trip.
		Activity currentActivity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(withinDayAgent);
		Trip trip = EditRoutes.getTrip(executedPlan, currentActivity, this.tripRouter);

		// If there is no trip after the activity.
		if (trip == null) return false;
		
		String mainMode = this.tripRouter.getMainModeIdentifier().identifyMainMode(trip.getTripElements());
		double departureTime = EditRoutes.getDepatureTime(trip);
		EditRoutes.replanFutureTrip(trip, executedPlan, mainMode, departureTime, scenario.getNetwork(), this.tripRouter);
		
		/*
		 * Updated code to use the TripRouter approach. This might result in problems at a few
		 * places where people assumed that only routes are updated but the leg objects remain
		 * in an agent's plan. 
		 */
//		// if it is a more complex trip than just a single leg use modern approach
//		if (trip.getTripElements().size() > 1) {
//			String mainMode = this.tripRouter.getMainModeIdentifier().identifyMainMode(trip.getTripElements());
//			double departureTime = EditRoutes.getDepatureTime(trip);
//			EditRoutes.replanFutureTrip(trip, executedPlan, mainMode, departureTime, scenario.getNetwork(), this.tripRouter);
//		} else {		
//			// Get the index of the current PlanElement.
//			int currentPlanElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent);
//			
//			// Search next leg in the agent's plan.
//			Leg nextLeg = null;
//			for (int i = currentPlanElementIndex + 1; i < executedPlan.getPlanElements().size(); i++) {
//				PlanElement planElement = executedPlan.getPlanElements().get(i);
//				if (planElement instanceof Leg) {
//					nextLeg = (Leg) planElement;
//					break;
//				}
//			}
//			
//			// check whether a next leg was found
//			if (nextLeg == null) return false;
//			
//			// new Route for next Leg
//			EditRoutes.replanFutureLegRoute(nextLeg, executedPlan.getPerson(), scenario.getNetwork(), tripRouter);
//		}
		
		return true;
	}

}