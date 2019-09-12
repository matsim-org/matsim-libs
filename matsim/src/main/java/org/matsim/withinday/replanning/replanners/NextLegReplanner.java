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
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;
import org.matsim.withinday.utils.EditTrips;

/**
 * The NextLegReplanner can be used while an agent is performing an activity. The
 * replanner creates a new trip from the current activity to the next main activity 
 * in the agent's plan.
 * 
 * In fact this should be renamed to NextTripReplanner. cdobler, apr'14
 */
public class NextLegReplanner extends WithinDayDuringActivityReplanner {

	private final TripRouter tripRouter;

	NextLegReplanner(Id<WithinDayReplanner> id, Scenario scenario, ActivityEndRescheduler internalInterface, TripRouter tripRouter) {
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
		Trip trip = TripStructureUtils.findTripStartingAtActivity(currentActivity, executedPlan, this.tripRouter.getStageActivityTypes() );

		// If there is no trip after the activity.
		if (trip == null) return false;
		
		String mainMode = this.tripRouter.getMainModeIdentifier().identifyMainMode(trip.getTripElements());
		double departureTime = TripStructureUtils.getDepartureTime(trip);
		// To replan pt legs, we would need internalInterface of type InternalInterface.class
		new EditTrips( this.tripRouter, scenario, null ).replanFutureTrip(trip, executedPlan, mainMode, departureTime );
		
		return true;
	}

}
