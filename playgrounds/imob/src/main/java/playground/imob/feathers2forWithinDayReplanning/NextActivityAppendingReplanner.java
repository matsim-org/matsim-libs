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

package playground.imob.feathers2forWithinDayReplanning;



import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.network.NetworkUtils;
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
final class NextActivityAppendingReplanner extends WithinDayDuringActivityReplanner {
	
	private static Logger logger = Logger.getLogger(NextActivityAppendingReplanner.class);

	private final TripRouter tripRouter;
	
	/*package*/ NextActivityAppendingReplanner(Id<WithinDayReplanner> id, Scenario scenario, ActivityEndRescheduler internalInterface, TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.tripRouter = tripRouter;
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {
		if (withinDayAgent.getId().equals(Id.createPersonId("66128"))) {
			return true;
		}
		
		logger.info("Replanning agent " + withinDayAgent.getId() + " at " + getTime());
		Plan executedPlan = WithinDayAgentUtils.getModifiablePlan(withinDayAgent);
		
		// Get the activity currently performed by the agent as well as the subsequent trip.
		Activity currentActivity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(withinDayAgent);

		// get a random link:
		Link[] links = NetworkUtils.getSortedLinks( scenario.getNetwork() ) ;
		int random = MatsimRandom.getLocalInstance().nextInt( links.length ) ;
		Id<Link> linkId = links[random].getId() ;
		
		Activity nextActivity = scenario.getPopulation().getFactory().createActivityFromLinkId("dummy", linkId) ;
		nextActivity.setMaximumDuration(666.0);
		
		executedPlan.addActivity(nextActivity);
		
		Trip trip = EditRoutes.getTrip(executedPlan, currentActivity, this.tripRouter);

		// If there is no trip after the activity.
		if (trip == null) {
			return false;
		}
		
		String mainMode = this.tripRouter.getMainModeIdentifier().identifyMainMode(trip.getTripElements());
		double departureTime = EditRoutes.getDepatureTime(trip);

		EditRoutes.replanFutureTrip(trip, executedPlan, mainMode, departureTime, scenario.getNetwork(), this.tripRouter);
		
		executedPlan.addLeg(scenario.getPopulation().getFactory().createLeg(mainMode));
		
		return true;
	}

}