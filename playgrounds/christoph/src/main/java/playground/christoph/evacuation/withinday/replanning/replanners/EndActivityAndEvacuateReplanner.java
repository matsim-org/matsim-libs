/* *********************************************************************** *
 * project: org.matsim.*
 * EndActivityAndEvacuateReplanner.java
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

package playground.christoph.evacuation.withinday.replanning.replanners;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.utils.EditRoutes;

import playground.christoph.evacuation.withinday.replanning.replanners.EndActivityAndEvacuateReplanner;

public class EndActivityAndEvacuateReplanner extends WithinDayDuringActivityReplanner {
	
	private static final Logger log = Logger.getLogger(EndActivityAndEvacuateReplanner.class);
	
	/*package*/ EndActivityAndEvacuateReplanner(Id id, Scenario scenario) {
		super(id, scenario);
	}
	
	@Override
	public boolean doReplanning(WithinDayAgent withinDayAgent) {		
		
		// If we don't have a valid PersonAgent
		if (withinDayAgent == null) return false;
	
		PlanImpl executedPlan = (PlanImpl)withinDayAgent.getExecutedPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;
		
		Activity currentActivity;
		
		/*
		 *  Get the current PlanElement and check if it is an Activity
		 */
		PlanElement currentPlanElement = withinDayAgent.getCurrentPlanElement();
		if (currentPlanElement instanceof Activity) {
			currentActivity = (Activity) currentPlanElement;
		} else return false;
				
		/*
		 * If the agent is already at the end of his scheduled plan then
		 * the simulation counter has been decreased by one. We re-enable the
		 * agent so we have to increase the counter again.
		 */
		if (currentActivity.getEndTime() == Time.UNDEFINED_TIME) this.agentCounter.incLiving();
		
		// Set the end time of the current activity to the current time.
		currentActivity.setEndTime(this.time);

		// get the index of the currently performed activity in the selected plan
		int currentActivityIndex = executedPlan.getActLegIndex(currentActivity);

		// identify the TransportMode for the rescueLeg
		String transportMode = identifyTransportMode(currentActivityIndex, executedPlan);
		
		// Remove all legs and activities after the current activity.
		while (executedPlan.getPlanElements().size() - 1 > currentActivityIndex) {
			executedPlan.removeActivity(executedPlan.getPlanElements().size() - 1);
		}
		
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		/*
		 * Now we add a new Activity at the rescue facility.
		 * We add no endtime therefore the activity will last until the end of
		 * the simulation.
		 */
		Activity rescueActivity = factory.createActivityFromLinkId("rescue", scenario.createId("rescueLink"));
		((ActivityImpl)rescueActivity).setFacilityId(scenario.createId("rescueFacility"));
		
		Coord rescueCoord = ((ScenarioImpl)scenario).getActivityFacilities().getFacilities().get(scenario.createId("rescueFacility")).getCoord();
		((ActivityImpl)rescueActivity).setCoord(rescueCoord);
		
		// by default we use a car...
//		Leg legToRescue = factory.createLeg(TransportMode.car);
		Leg legToRescue = factory.createLeg(transportMode);
			
		// add new activity
		int position = executedPlan.getActLegIndex(currentActivity) + 1;
		executedPlan.insertLegAct(position, legToRescue, rescueActivity);
			
		// calculate route for the leg to the rescue facility
		new EditRoutes().replanFutureLegRoute(executedPlan, position, routeAlgo);

		/*
		 * Reschedule the currently performed Activity in the Mobsim - there
		 * the activityEndsList has to be updated.
		 */
		// yyyy a method getMobsim in MobimAgent would be useful here. cdobler, Oct'10
		// Intuitively I would agree.  We should think about where to set this so that, under normal circumstances,
		// it can't become null.  kai, oct'10
		if (withinDayAgent instanceof PersonDriverAgentImpl) {
			// yyyy do we have to check that? We have a currentActivity... cdobler, Oct'10
			boolean found = ((PersonDriverAgentImpl) withinDayAgent).getMobsim().getActivityEndsList().contains(this);
			
			// If the agent is not in the activityEndsList return without doing anything else.
			if (!found) return false;
			
			double oldDepartureTime = withinDayAgent.getActivityEndTime();
		
			((ExperimentalBasicWithindayAgent) withinDayAgent).calculateDepartureTime(currentActivity);
			double newDepartureTime = withinDayAgent.getActivityEndTime();
			((PersonDriverAgentImpl) withinDayAgent).getMobsim().rescheduleActivityEnd(withinDayAgent, oldDepartureTime, newDepartureTime);
			return true;
		}
		else {
			log.warn("PersonAgent is no DefaultPersonDriverAgent - the new departure time cannot be calcualted!");
			return false;
		}		
	}

	/*
	 * By default we try to use a car. We can do this, if the previous or the next 
	 * Leg are performed with a car.
	 * The order is as following:
	 * car is preferred to ride is preferred to pt is preferred to bike if preferred to walk 
	 */
	private String identifyTransportMode(int currentActivityIndex, Plan selectedPlan) {
		
		boolean hasCar = false;
		boolean hasBike = false;
		boolean hasPt = false;
		boolean hasRide = false;
		
		if (currentActivityIndex > 0) {
			Leg previousLeg = (Leg) selectedPlan.getPlanElements().get(currentActivityIndex - 1);
			String transportMode = previousLeg.getMode();
			if (transportMode.equals(TransportMode.car)) hasCar = true;
			else if (transportMode.equals(TransportMode.bike)) hasBike = true;
			else if (transportMode.equals(TransportMode.pt)) hasPt = true;
			else if (transportMode.equals(TransportMode.ride)) hasRide = true;
		}
		
		if (currentActivityIndex + 1 < selectedPlan.getPlanElements().size()) {
			Leg nextLeg = (Leg) selectedPlan.getPlanElements().get(currentActivityIndex + 1);
			String transportMode = nextLeg.getMode();
			if (transportMode.equals(TransportMode.car)) hasCar = true;
			else if (transportMode.equals(TransportMode.bike)) hasBike = true;
			else if (transportMode.equals(TransportMode.pt)) hasPt = true;
			else if (transportMode.equals(TransportMode.ride)) hasRide = true;
		}
		
		if (hasCar) return TransportMode.car;
		else if (hasRide) return TransportMode.ride;
		else if (hasPt) return TransportMode.pt;
		else if (hasBike) return TransportMode.bike;
		else return TransportMode.walk;
	}	
}
