/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentLegInitialReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.controler.EvacuationConstants;

/*
 * Removes all legs and activities after the next activity.
 * If the next activity is located at a facility which is not secure,
 * it is relocated to a rescue facility.
 */
public class CurrentLegInitialReplanner extends WithinDayDuringLegReplanner {

	private final CoordAnalyzer coordAnalyzer;
	private final TripRouter tripRouter;
	
	/*package*/ CurrentLegInitialReplanner(Id id, Scenario scenario, ActivityEndRescheduler internalInterface,
			CoordAnalyzer coordAnalyzer, TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.coordAnalyzer = coordAnalyzer;
		this.tripRouter = tripRouter;
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {

		// If we don't have a valid WithinDayPersonAgent
		if (withinDayAgent == null) return false;
		
		Plan executedPlan = WithinDayAgentUtils.getModifiablePlan(withinDayAgent);

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent);
		Activity nextActivity = (Activity) executedPlan.getPlanElements().get(WithinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent) + 1);
		nextActivity.setEndTime(Time.UNDEFINED_TIME);
		Facility facility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(nextActivity.getFacilityId());
		boolean isAffected = coordAnalyzer.isFacilityAffected(facility);
		
		// Remove all legs and activities after the next activity.
		int nextActivityIndex = executedPlan.getPlanElements().indexOf(nextActivity);
		while (executedPlan.getPlanElements().size() - 1 > nextActivityIndex) {
			executedPlan.getPlanElements().remove(executedPlan.getPlanElements().size() - 1);
		}
		
		/*
		 * If the facility is affected, we have to relocate the activity.
		 * First, relocate it to the facility located at the rescueLink.
		 * Then relocate it again to the last-non rescue link. This is necessary
		 * for walk2d legs. Alternatively we could add switch-to-walk activities.
		 */
		if (isAffected) {
			nextActivity.setType(EvacuationConstants.RESCUE_ACTIVITY);
			((ActivityImpl) nextActivity).setFacilityId(Id.create(EvacuationConstants.RESCUE_FACILITY, ActivityFacility.class));
			((ActivityImpl) nextActivity).setLinkId(Id.create(EvacuationConstants.RESCUE_LINK, Link.class));
			
			// for walk2d legs: switch mode to walk for routing
			Leg currentLeg = WithinDayAgentUtils.getModifiableCurrentLeg(withinDayAgent);
			boolean isWalk2d = currentLeg.getMode().equals("walk2d");
			
			// switch to walk mode for routing
			if (isWalk2d) {
				currentLeg.setMode(TransportMode.walk);
			}

			// new Route for current Leg
			this.editRoutes.relocateCurrentLegRoute(currentLeg, executedPlan.getPerson(), currentLinkIndex, 
					Id.create(EvacuationConstants.RESCUE_LINK, Link.class), time, scenario.getNetwork(), tripRouter);
			
			// switch back to walk2d
			if (isWalk2d) {
				currentLeg.setMode("walk2d");
			}
			/*
			 * Identify the last non-rescue link
			 */			
			nextActivity.setType(EvacuationConstants.RESCUE_ACTIVITY);
			NetworkRoute route = (NetworkRoute) currentLeg.getRoute();
			Id endLinkId = route.getLinkIds().get(route.getLinkIds().size() - 2);
			((ActivityImpl) nextActivity).setFacilityId(Id.create(EvacuationConstants.RESCUE_FACILITY + endLinkId.toString(), ActivityFacility.class));
			((ActivityImpl) nextActivity).setLinkId(endLinkId);
			
			// new Route for current Leg
//			new EditRoutes().replanCurrentLegRoute(executedPlan, currentLegIndex, currentLinkIndex, routeAlgo, time);
			List<Id<Link>> newLinkIds = new ArrayList<Id<Link>>(route.getLinkIds().subList(0, route.getLinkIds().size() - 2));
			route.setLinkIds(route.getStartLinkId(), newLinkIds, endLinkId);
			
			// Finally reset the cached Values of the PersonAgent - they may have changed!
			WithinDayAgentUtils.resetCaches(withinDayAgent);
		}
		
		return true;
	}
}