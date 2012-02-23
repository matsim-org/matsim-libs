/* *********************************************************************** *
 * project: org.matsim.*
 * JoinedHouseholdsReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.utils.EditRoutes;

import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.PassengerDepartureHandler;
import playground.christoph.evacuation.withinday.replanning.identifiers.JoinedHouseholdsIdentifier;

/**
 * Replanner for agents that are currently together with the other
 * members of their household. They evacuate together to a predefined
 * facility in the secure area.
 * 
 * @author cdobler
 */
public class JoinedHouseholdsReplanner extends WithinDayDuringActivityReplanner {

	private static final Logger log = Logger.getLogger(JoinedHouseholdsReplanner.class);
	
	public static final String activityType = "rescue";
	
	private final HouseholdsTracker householdsTracker;
	private final JoinedHouseholdsIdentifier identifier;
	
	public JoinedHouseholdsReplanner(Id id, Scenario scenario, HouseholdsTracker householdsTracker, JoinedHouseholdsIdentifier identifier) {
		super(id, scenario);
		this.householdsTracker = householdsTracker;
		this.identifier = identifier;
	}

	@Override
	public boolean doReplanning(PlanBasedWithinDayAgent withinDayAgent) {
		
		// If we don't have a valid PersonAgent
		if (withinDayAgent == null) return false;
	
		PlanImpl executedPlan = (PlanImpl)withinDayAgent.getSelectedPlan();

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
		
		double oldDepartureTime = withinDayAgent.getActivityEndTime();
		
		/*
		 * Check whether the agent is already at the meeting point.
		 * If yes, remove activities that are scheduled at a later point in time.
		 * Otherwise create a new activity at the meeting point, add it to the plan
		 * and remove all other remaining activities.
		 */
		Id householdId = householdsTracker.getPersonsHouseholdId(withinDayAgent.getId());
		Id meetingPointId = identifier.getHouseholdMeetingPointMapping().get(householdId);
		
		// set new meeting point
		this.householdsTracker.getHouseholdPosition(householdId).setMeetingPointFacilityId(meetingPointId);
		
		/*
		 * The agent is currently not at the Meeting Point. Therefore, we create a new Activity
		 * which is located there. Additionally, we set a new end time for the current Activity.
		 */
		ActivityFacility meetingFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(meetingPointId);
		Activity meetingActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(activityType, meetingFacility.getLinkId());
		((ActivityImpl) meetingActivity).setFacilityId(meetingPointId);
		((ActivityImpl)meetingActivity).setCoord(meetingFacility.getCoord());
		meetingActivity.setEndTime(Double.POSITIVE_INFINITY);
		
		/*
		 * Create Leg from the current Activity to the Meeting Point
		 */		
		// identify the TransportMode
		String transportMode = this.identifier.getTransportModeMapping().get(withinDayAgent.getId());
		
		/*
		 * If its a passengerTransportMode leg, set the mode to Ride. Otherwise
		 * the router cannot handle it.
		 */
		Leg legToMeeting;
		if (transportMode.equals(PassengerDepartureHandler.passengerTransportMode)) {
			legToMeeting = scenario.getPopulation().getFactory().createLeg(TransportMode.ride);
		} else {
			legToMeeting = scenario.getPopulation().getFactory().createLeg(transportMode);
		}
		
		double newEndTime = this.time;
		currentActivity.setMaximumDuration(newEndTime - currentActivity.getStartTime());
		currentActivity.setEndTime(newEndTime);
		legToMeeting.setDepartureTime(newEndTime);
		
		// add new activity
		int position = executedPlan.getActLegIndex(currentActivity) + 1;
		executedPlan.insertLegAct(position, legToMeeting, meetingActivity);
		
		// calculate route for the leg to the rescue facility
		new EditRoutes().replanFutureLegRoute(executedPlan, position, routeAlgo);
		
		// set correct transport mode
		legToMeeting.setMode(transportMode);
		
		
		// set vehicle in the route, if it is a car leg
		if (transportMode.equals(TransportMode.car)) {
			Id vehicleId = this.identifier.getDriverVehicleMapping().get(withinDayAgent.getId());
			NetworkRoute route = (NetworkRoute) legToMeeting.getRoute();
			route.setVehicleId(vehicleId);
		}
		
		meetingActivity.setStartTime(legToMeeting.getDepartureTime() + legToMeeting.getTravelTime());
		
		/*
		 * Reschedule the currently performed Activity in the Mobsim - there
		 * the activityEndsList has to be updated.
		 */
		// yyyy a method getMobsim in MobimAgent would be useful here. cdobler, Oct'10
		// Intuitively I would agree.  We should think about where to set this so that, under normal circumstances,
		// it can't become null.  kai, oct'10
		if (withinDayAgent instanceof PersonDriverAgentImpl) {			
			
			((ExperimentalBasicWithindayAgent) withinDayAgent).calculateDepartureTime(currentActivity);
			double newDepartureTime = withinDayAgent.getActivityEndTime();
			((PersonDriverAgentImpl) withinDayAgent).getMobsim().rescheduleActivityEnd(withinDayAgent, oldDepartureTime, newDepartureTime);
						
			return true;
		}
		else {
			log.warn("PersonAgent is no PersonDriverAgentImpl - the new departure time cannot be calculated!");
			return false;
		}
	}

}
