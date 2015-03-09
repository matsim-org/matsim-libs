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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDeparture;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;

import playground.christoph.evacuation.controler.EvacuationConstants;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.PersonDecisionData;
import playground.christoph.evacuation.trafficmonitoring.SwissPTTravelTimeCalculator;
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
	
	private final DecisionDataProvider decisionDataProvider;
	private final JoinedHouseholdsIdentifier identifier;
	private final SwissPTTravelTimeCalculator ptTravelTime;
	private final TripRouter tripRouter;
	
	public JoinedHouseholdsReplanner(Id id, Scenario scenario, InternalInterface internalInterface, 
			DecisionDataProvider decisionDataProvider, JoinedHouseholdsIdentifier identifier,
			SwissPTTravelTimeCalculator ptTravelTime, TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.decisionDataProvider = decisionDataProvider;
		this.identifier = identifier;
		this.ptTravelTime = ptTravelTime;
		this.tripRouter = tripRouter;
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {
		
		// If we don't have a valid PersonAgent
		if (withinDayAgent == null) return false;
	
		Plan executedPlan = WithinDayAgentUtils.getModifiablePlan(withinDayAgent);

		// If we don't have an executed plan
		if (executedPlan == null) return false;
		
		Activity currentActivity;

		/*
		 *  Get the current PlanElement and check if it is an Activity
		 */
		PlanElement currentPlanElement = WithinDayAgentUtils.getCurrentPlanElement(withinDayAgent);
		if (currentPlanElement instanceof Activity) {
			currentActivity = (Activity) currentPlanElement;
		} else return false;
		
		/*
		 * Check whether the agent is already at the meeting point.
		 * If yes, remove activities that are scheduled at a later point in time.
		 * Otherwise create a new activity at the meeting point, add it to the plan
		 * and remove all other remaining activities.
		 */
		PersonDecisionData pdd = this.decisionDataProvider.getPersonDecisionData(withinDayAgent.getId());
		Id householdId = pdd.getHouseholdId();
		Id meetingPointId = identifier.getJoinedHouseholdsContext().getHouseholdMeetingPointMap().get(householdId);
		
		// set new meeting point
		this.decisionDataProvider.getHouseholdDecisionData(householdId).setMeetingPointFacilityId(meetingPointId);
		
		/*
		 * The agent is currently not at the Meeting Point. Therefore, we create a new Activity
		 * which is located there. Additionally, we set a new end time for the current Activity.
		 */
		ActivityFacility meetingFacility = scenario.getActivityFacilities().getFacilities().get(meetingPointId);
		Activity meetingActivity = scenario.getPopulation().getFactory().createActivityFromLinkId(
				EvacuationConstants.RESCUE_ACTIVITY, meetingFacility.getLinkId());
		((ActivityImpl) meetingActivity).setFacilityId(meetingPointId);
		((ActivityImpl)meetingActivity).setCoord(meetingFacility.getCoord());
		meetingActivity.setEndTime(Double.POSITIVE_INFINITY);
		
		/*
		 * Create Leg from the current Activity to the Meeting Point
		 */
		// identify the TransportMode
		String transportMode = this.identifier.getJoinedHouseholdsContext().getTransportModeMap().get(withinDayAgent.getId());
		
		/*
		 * If its a passengerTransportMode leg, set the mode to Ride. Otherwise
		 * the router cannot handle it.
		 */
		Leg legToMeeting;
		if (transportMode.equals(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE)) {
			legToMeeting = scenario.getPopulation().getFactory().createLeg(TransportMode.ride);
		} else {
			legToMeeting = scenario.getPopulation().getFactory().createLeg(transportMode);
		}
		
		double newEndTime = this.time;
		currentActivity.setMaximumDuration(newEndTime - currentActivity.getStartTime());
		currentActivity.setEndTime(newEndTime);
		legToMeeting.setDepartureTime(newEndTime);
		
		// add new activity
		int position = executedPlan.getPlanElements().indexOf(currentActivity) + 1;
		executedPlan.getPlanElements().add(position, meetingActivity);
		executedPlan.getPlanElements().add(position, legToMeeting);
		
		// calculate route for the leg to the rescue facility
		this.editRoutes.relocateFutureLegRoute(legToMeeting, currentActivity.getLinkId(), meetingActivity.getLinkId(), 
				executedPlan.getPerson(), scenario.getNetwork(), tripRouter);
		
		// set correct transport mode
		legToMeeting.setMode(transportMode);
		
		// set vehicle in the route, if it is a car leg
		if (transportMode.equals(TransportMode.car)) {
			Id vehicleId = this.identifier.getJoinedHouseholdsContext().getDriverVehicleMap().get(withinDayAgent.getId());
			NetworkRoute route = (NetworkRoute) legToMeeting.getRoute();
			route.setVehicleId(vehicleId);
		}
		
		// if the person has to walk, we additionally try pt
		if (transportMode.equals(TransportMode.walk)) {
			Tuple<Double, Coord> tuple = ptTravelTime.calcSwissPtTravelTime(currentActivity, meetingActivity, this.time, executedPlan.getPerson()); 
			double travelTimePT = tuple.getFirst();
			double travelTimeWalk = legToMeeting.getTravelTime();
			
			// If using pt is faster than walking switch to pt.
			if (travelTimePT < travelTimeWalk) {
				
//				log.info("PT (" + travelTimePT  + ") is faster than walking (" + travelTimeWalk + 
//						"). Person " + withinDayAgent.getId().toString() +
//						", age " + ((PersonImpl) withinDayAgent.getSelectedPlan().getPerson()).getAge() +
//						", walk distance (in route) " + legToMeeting.getRoute().getDistance());
				
				legToMeeting.setMode(TransportMode.pt);
				
				// calculate route for the leg to the rescue facility
				this.editRoutes.relocateFutureLegRoute(legToMeeting, currentActivity.getLinkId(), meetingActivity.getLinkId(), 
						executedPlan.getPerson(), scenario.getNetwork(), tripRouter);
				
				// set travel time
				legToMeeting.getRoute().setTravelTime(travelTimePT);
				
				// set speed
				double routeLength = RouteUtils.calcDistance((NetworkRoute) legToMeeting.getRoute(), scenario.getNetwork());
				ptTravelTime.setPersonSpeed(withinDayAgent.getId(), routeLength / travelTimePT);
			}
		}
		
		meetingActivity.setStartTime(legToMeeting.getDepartureTime() + legToMeeting.getTravelTime());
		
		// assign joint departure to agent's car/ride leg
		if (legToMeeting.getMode().equals(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE) || 
				legToMeeting.getMode().equals(TransportMode.car)) {
			Id agentId = withinDayAgent.getId();
			JointDeparture jointDeparture = this.identifier.getJointDeparture(agentId);
			this.identifier.getJointDepartureOrganizer().assignAgentToJointDeparture(agentId, legToMeeting, jointDeparture);
		}
		
		// mark agent as departed
		this.identifier.incPerformedDepartures(householdId);
		
		/*
		 * Reschedule the currently performed Activity in the Mobsim - there
		 * the activityEndsList has to be updated.
		 */
		// yyyy a method getMobsim in MobimAgent would be useful here. cdobler, Oct'10
		// Intuitively I would agree.  We should think about where to set this so that, under normal circumstances,
		// it can't become null.  kai, oct'10
		WithinDayAgentUtils.calculateAndSetDepartureTime(withinDayAgent, currentActivity);
		this.internalInterface.rescheduleActivityEnd(withinDayAgent);
		return true;
	}
}