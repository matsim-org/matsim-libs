/* *********************************************************************** *
 * project: org.matsim.*
 * SelectHouseholdMeetingPoint.java
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

package playground.christoph.evacuation.withinday.replanning.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.Time;
import org.matsim.households.Household;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.Tracker.Position;

/**
 * Decides where a household will meet after the evacuation order has been given.
 * This could be either at home or at another location, if the home location is
 * not treated to be secure. However, households might meet at their insecure home
 * location and then evacuate as a unit.
 * 
 * By default, all households meet at home and the select another location, if
 * their home location is not secure.
 * 
 * @author cdobler
 */
public class SelectHouseholdMeetingPoint implements SimulationInitializedListener, SimulationBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(SelectHouseholdMeetingPoint.class);
	
	private final Scenario scenario;
	private final HouseholdsTracker householdsTracker;
	private final VehiclesTracker vehiclesTracker;
	private final CoordAnalyzer coordAnalyzer;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	
	private double time = Time.UNDEFINED_TIME;
	
	public SelectHouseholdMeetingPoint(Scenario scenario,
			HouseholdsTracker householdsTracker, VehiclesTracker vehiclesTracker, CoordAnalyzer coordAnalyzer) {
		this.scenario = scenario;
		this.householdsTracker = householdsTracker;
		this.vehiclesTracker = vehiclesTracker;
		this.coordAnalyzer = coordAnalyzer;
		
		this.modeAvailabilityChecker = new ModeAvailabilityChecker(scenario, vehiclesTracker);
	}
	
	/*
	 *  At the moment, there is only a single rescue facility.
	 *  Instead, multiple *real* rescue facilities could be defined.
	 */
	public Id selectRescueMeetingPoint(Id householdId) {
//		HouseholdInfo householdInfo = householdsUtils.getHouseholdInfoMap().get(householdId);
//		Id oldMeetingPointId = householdInfo.getMeetingPointId();
		Id newMeetingPointId = scenario.createId("rescueFacility");

//		/*
//		 * If the meeting point is not changed we have nothing to do.
//		 */
//		if (oldMeetingPointId == newMeetingPointId) return;
//		
//		/*
//		 * If the household is currently joined at the old meeting point and
//		 * a new meeting point is set.
//		 */
//		if (householdInfo.allMembersAtMeetingPoint()) {
//			Event leaveEvent = new HouseholdLeaveMeetingPointEventImpl(this.time, householdId, newMeetingPointId);
//			eventsManager.processEvent(leaveEvent);	
//		}
//			
//		householdsUtils.setMeetingPoint(householdId, newMeetingPointId);
//		
//		/*
//		 * If the household is currently joined at the new meeting point.
//		 */
//		if (householdInfo.allMembersAtMeetingPoint()) {
//			Event enterEvent = new HouseholdEnterMeetingPointEventImpl(this.time, householdId, newMeetingPointId);
//			eventsManager.processEvent(enterEvent);	
//		}
//		
//		Event setEvent = new HouseholdSetMeetingPointEventImpl(this.time, householdId, newMeetingPointId);
//		eventsManager.processEvent(setEvent);
		
		return newMeetingPointId;
	}

	/*
	 * Check, whether the home location of a household is secure or not.
	 */
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
//		for (HouseholdInfo householdInfo : householdsUtils.getHouseholdInfoMap().values()) {
//			setHomeFacilitySecurity(householdInfo);
//		}
	}

	/*
	 * Get the actual simulation time.
	 */
	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		this.time = e.getSimulationTime();
	}
	
	private double calculateHouseholdReturnHomeTime(Household household) {
		
		double returnHomeTime = Double.MIN_VALUE;
		for (Id personId : household.getMemberIds()) {
			double time = calculateAgentReturnHomeTime(personId);
			if (time > returnHomeTime) returnHomeTime = time;
		}
		return returnHomeTime;	
	}
	
	private double calculateAgentReturnHomeTime(Id personId) {
		
		AgentPosition agentPosition = householdsTracker.getAgentPosition(personId);
		Position positionType = agentPosition.getPositionType();
		
		/*
		 * Identify the transport mode that the agent could use for its trip home.
		 */
		String mode = null;
		if (positionType == Position.LINK) {
			mode = agentPosition.getTransportMode();
		} else if (positionType == Position.FACILITY) {

			// get the index of the currently performed activity in the selected plan
			MobsimAgent mobsimAgent = agentPosition.getAgent();
			PlanAgent planAgent = (PlanAgent) mobsimAgent;
			
			PlanImpl executedPlan = (PlanImpl) planAgent.getSelectedPlan();
			
			Activity currentActivity = (Activity) planAgent.getCurrentPlanElement();
			int currentActivityIndex = executedPlan.getActLegIndex(currentActivity);
			
			Id possibleVehicleId = getVehicleId(executedPlan);
			mode = this.modeAvailabilityChecker.identifyTransportMode(currentActivityIndex, executedPlan, possibleVehicleId);		
		} else if (positionType == Position.VEHICLE) {
			/*
			 * Should not occur since passengers are only created after the evacuation has started.
			 */
			log.warn("Found passenger agent. Passengers should not be created before the evacuation has started!");
			return 0.0;
		} else {
			log.error("Found unknown position type: " + positionType.toString());
			return 0.0;						
		}
		
		/*
		 * Calculate the travel time from the agents current position to its home facility.
		 */
		double t = 0.0;
		
		// TODO calculate travel time...
		
		return t;
	}
	
	/**
	 * Return the id of the first vehicle used by the agent.
	 * Without Within-Day Replanning, an agent will use the same
	 * vehicle during the whole day. When Within-Day Replanning
	 * is enabled, this method should not be called anymore...
	 */
	private Id getVehicleId(Plan plan) {
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getMode().equals(TransportMode.car)) {
					Route route = leg.getRoute();
					if (route instanceof NetworkRoute) {
						NetworkRoute networkRoute = (NetworkRoute) route;
						return networkRoute.getVehicleId();
					}					
				}
			}
		}
		return null;
	}
	
}
