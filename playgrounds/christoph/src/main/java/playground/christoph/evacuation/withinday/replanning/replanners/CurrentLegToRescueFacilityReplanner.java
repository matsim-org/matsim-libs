/* *********************************************************************** *
 * project: org.matsim.*
 * CurrentLegToRescueFacilityReplanner.java
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.withinday.mobsim.WithinDayPersonAgent;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.utils.EditRoutes;
import org.matsim.withinday.utils.ReplacePlanElements;

import playground.christoph.evacuation.config.EvacuationConfig;

public class CurrentLegToRescueFacilityReplanner extends WithinDayDuringLegReplanner {

	/*package*/ CurrentLegToRescueFacilityReplanner(Id id, Scenario scenario) {
		super(id, scenario);
	}

	@Override
	public boolean doReplanning(WithinDayAgent withinDayAgent) {
		
		// do Replanning only in the timestep where the Evacuation has started.
		if (this.time > EvacuationConfig.evacuationTime) return true;
				
		// If we don't have a valid Replanner.
		if (this.routeAlgo == null) return false;

		// If we don't have a valid WithinDayPersonAgenwithinDayAgentthinDayAgent == null) return false;

		WithinDayPersonAgent withinDayPersonAgent = null;
		if (!(withinDayAgent instanceof WithinDayPersonAgent)) return false;
		else {
			withinDayPersonAgent = (WithinDayPersonAgent) withinDayAgent;
		}

		PlanImpl executedPlan = (PlanImpl)withinDayAgent.getExecutedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		int currentLegIndex = withinDayAgent.getCurrentPlanElementIndex();
		int currentLinkIndex = withinDayAgent.getCurrentRouteLinkIdIndex();
		Activity nextActivity = (Activity) executedPlan.getPlanElements().get(withinDayAgent.getCurrentPlanElementIndex() + 1);
				
		// If it is not a car Leg we don't replan it.
//		if (!currentLeg.getMode().equals(TransportMode.car)) return false;
		
		/*
		 * Create new Activity at the rescue facility.
		 */
		Activity rescueActivity = scenario.getPopulation().getFactory().createActivityFromLinkId("rescue", scenario.createId("rescueLink"));
		((ActivityImpl)rescueActivity).setFacilityId(scenario.createId("rescueFacility"));
		rescueActivity.setEndTime(Time.UNDEFINED_TIME);

		Coord rescueCoord = ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().get(scenario.createId("rescueFacility")).getCoord();
		((ActivityImpl)rescueActivity).setCoord(rescueCoord);
		
		/*
		 * If the Agent wants to perform the next Activity at the current Link we
		 * cannot replace that Activity at the moment (Simulation logic...).
		 * Therefore we set the Duration of the next Activity to 0 and add a new
		 * Rescue Activity afterwithinDayAgent*/
		if (withinDayAgent.getDestinationLinkId().equals(nextActivity.getLinkId())) {
			nextActivity.setStartTime(this.time);
			nextActivity.setEndTime(this.time);
			
			// Remove all legs and activities after the next activity.
			int nextActivityIndex = executedPlan.getActLegIndex(nextActivity);
			
			while (executedPlan.getPlanElements().size() - 1 > nextActivityIndex) {
				executedPlan.removeActivity(executedPlan.getPlanElements().size() - 1);
			}
			
			Leg newLeg = executedPlan.createAndAddLeg(TransportMode.car);
			int position = executedPlan.getPlanElements().size() - 1; 
			executedPlan.addActivity(rescueActivity);
			
			new EditRoutes().replanFutureLegRoute(executedPlan, position, routeAlgo);
		}
		
		else {
			/*
			 * Now we add a new Activity at the rescue facility.
			 */
//			Activity rescueActivity = scenario.getPopulation().getFactory().createActivityFromLinkId("rescue", scenario.createId("rescueLink"));
//			((ActivityImpl)rescueActivity).setFacilityId(scenario.createId("rescueFacility"));
//			rescueActivity.setEndTime(Time.UNDEFINED_TIME);
			
			new ReplacePlanElements().replaceActivity(executedPlan, nextActivity, rescueActivity);
			
			// new Route for current Leg
			new EditRoutes().replanCurrentLegRoute(executedPlan, currentLegIndex, currentLinkIndex, routeAlgo, time);
			
			// Remove all legs and activities after the next activity.
			int nextActivityIndex = executedPlan.getActLegIndex(rescueActivity);
			
			while (executedPlan.getPlanElements().size() - 1 > nextActivityIndex)
			{
				executedPlan.removeActivity(executedPlan.getPlanElements().size() - 1);
			}
			
			// Finally reset the cached Values of the PersonAgent - they may have changed!
			withinDayPersonAgent.resetCaches();
		}
		
		return true;
	}
}