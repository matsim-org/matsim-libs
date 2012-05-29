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

package playground.christoph.evacuation.withinday.replanning.replanners;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.utils.EditRoutes;

import playground.christoph.evacuation.analysis.CoordAnalyzer;

/*
 * Removes all legs and activities after the next activity.
 * If the next activity is located at a facility which is not secure,
 * it is relocated to a rescue facility.
 */
public class CurrentLegInitialReplanner extends WithinDayDuringLegReplanner {

	private final CoordAnalyzer coordAnalyzer;
	
	/*package*/ CurrentLegInitialReplanner(Id id, Scenario scenario, InternalInterface internalInterface,
			CoordAnalyzer coordAnalyzer) {
		super(id, scenario, internalInterface);
		this.coordAnalyzer = coordAnalyzer;
	}

	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {

		// If we don't have a valid Replanner.
		if (this.routeAlgo == null) return false;

		// If we don't have a valid WithinDayPersonAgent
		if (withinDayAgent == null) return false;
		
		PlanImpl executedPlan = (PlanImpl)withinDayAgent.getSelectedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		int currentLegIndex = withinDayAgent.getCurrentPlanElementIndex();
		int currentLinkIndex = withinDayAgent.getCurrentRouteLinkIdIndex();
		Activity nextActivity = (Activity) executedPlan.getPlanElements().get(withinDayAgent.getCurrentPlanElementIndex() + 1);
		nextActivity.setEndTime(Time.UNDEFINED_TIME);
		Facility facility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(nextActivity.getFacilityId());
		boolean isAffected = coordAnalyzer.isFacilityAffected(facility);
		
		// Remove all legs and activities after the next activity.
		int nextActivityIndex = executedPlan.getActLegIndex(nextActivity);
		while (executedPlan.getPlanElements().size() - 1 > nextActivityIndex) {
			executedPlan.removeActivity(executedPlan.getPlanElements().size() - 1);
		}
		
		/*
		 * If the facility is affected, we have to relocate the activity.
		 * First, relocate it to the facility located at the rescueLink.
		 * Then relocate it again to the last-non rescue link. This is necessary
		 * for walk2d legs. Alternatively we could add switch-to-walk activities.
		 */
		if (isAffected) {
			nextActivity.setType("rescue");
			((ActivityImpl) nextActivity).setFacilityId(scenario.createId("rescueFacility"));
			((ActivityImpl) nextActivity).setLinkId(scenario.createId("rescueLink"));
			
			// new Route for current Leg
			new EditRoutes().replanCurrentLegRoute(executedPlan, currentLegIndex, currentLinkIndex, routeAlgo, time);
			
			/*
			 * Identify the last non-rescue link
			 */
			nextActivity.setType("rescue");
			Leg currentLeg = (Leg) executedPlan.getPlanElements().get(currentLegIndex);
			NetworkRoute route = (NetworkRoute) currentLeg.getRoute();
			Id endLinkId = route.getLinkIds().get(route.getLinkIds().size() - 2);
			((ActivityImpl) nextActivity).setFacilityId(scenario.createId("rescueFacility" + endLinkId.toString()));
			((ActivityImpl) nextActivity).setLinkId(endLinkId);
			
			// new Route for current Leg
//			new EditRoutes().replanCurrentLegRoute(executedPlan, currentLegIndex, currentLinkIndex, routeAlgo, time);
			currentLeg.setRoute(route.getSubRoute(route.getStartLinkId(), endLinkId));
			
			// Finally reset the cached Values of the PersonAgent - they may have changed!
			withinDayAgent.resetCaches();
		}
		
		return true;
	}
}