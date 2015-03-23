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

package playground.christoph.evacuation.withinday.replanning.replanners.old;

import org.matsim.api.core.v01.Coord;
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
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.utils.ReplacePlanElements;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.controler.EvacuationConstants;

public class CurrentLegToRescueFacilityReplanner extends WithinDayDuringLegReplanner {

	protected final TripRouter tripRouter;
	
	/*package*/ CurrentLegToRescueFacilityReplanner(Id id, Scenario scenario, ActivityEndRescheduler internalInterface,
			TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.tripRouter = tripRouter;
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {
		
		// do Replanning only in the timestep where the Evacuation has started.
		if (this.time > EvacuationConfig.evacuationTime) return true;
				
		// If we don't have a valid WithinDayPersonAgent
		if (withinDayAgent == null) return false;
		
		Plan executedPlan = WithinDayAgentUtils.getModifiablePlan(withinDayAgent);

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		Leg currentLeg = WithinDayAgentUtils.getModifiableCurrentLeg(withinDayAgent);
		int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent);
		Activity nextActivity = (Activity) executedPlan.getPlanElements().get(WithinDayAgentUtils.getCurrentPlanElementIndex(withinDayAgent) + 1);
				
		// If it is not a car Leg we don't replan it.
//		if (!currentLeg.getMode().equals(TransportMode.car)) return false;
		
		/*
		 * Create new Activity at the rescue facility.
		 */
		Activity rescueActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(EvacuationConstants.RESCUE_ACTIVITY, 
				Id.create(EvacuationConstants.RESCUE_LINK, Link.class));
		((ActivityImpl)rescueActivity).setFacilityId(Id.create(EvacuationConstants.RESCUE_FACILITY, ActivityFacility.class));
		rescueActivity.setEndTime(Time.UNDEFINED_TIME);

		Coord rescueCoord = ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().get(Id.create(EvacuationConstants.RESCUE_FACILITY, ActivityFacility.class)).getCoord();
		((ActivityImpl)rescueActivity).setCoord(rescueCoord);
		
		/*
		 * If the Agent wants to perform the next Activity at the current Link we
		 * cannot replace that Activity at the moment (Simulation logic...).
		 * Therefore we set the Duration of the next Activity to 0 and add a new
		 * Rescue Activity afterwards.
		 */
		if (withinDayAgent.getDestinationLinkId().equals(nextActivity.getLinkId())) {
			nextActivity.setStartTime(this.time);
			nextActivity.setEndTime(this.time);
			
			// Remove all legs and activities after the next activity.
			int nextActivityIndex = executedPlan.getPlanElements().indexOf(nextActivity);
			
			while (executedPlan.getPlanElements().size() - 1 > nextActivityIndex) {
				executedPlan.getPlanElements().remove(executedPlan.getPlanElements().size() - 1);
			}
			
			Leg newLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
			executedPlan.addLeg(newLeg);
			executedPlan.addActivity(rescueActivity);
			
			this.editRoutes.relocateFutureLegRoute(newLeg, withinDayAgent.getCurrentLinkId(), rescueActivity.getLinkId(), executedPlan.getPerson(), scenario.getNetwork(), tripRouter);
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
			this.editRoutes.relocateCurrentLegRoute(currentLeg, executedPlan.getPerson(), currentLinkIndex, 
					rescueActivity.getLinkId(), time, scenario.getNetwork(), tripRouter);
			
			// Remove all legs and activities after the next activity.
			int nextActivityIndex = executedPlan.getPlanElements().indexOf(rescueActivity);
			
			while (executedPlan.getPlanElements().size() - 1 > nextActivityIndex) {
				executedPlan.getPlanElements().remove(executedPlan.getPlanElements().size() - 1);
			}
			
			// Finally reset the cached Values of the PersonAgent - they may have changed!
			WithinDayAgentUtils.resetCaches(withinDayAgent);
		}
		
		return true;
	}
}