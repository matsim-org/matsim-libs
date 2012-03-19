/* *********************************************************************** *
 * project: org.matsim.*
 * SelectHouseholdMeetingPointRunner.java
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

package playground.christoph.evacuation.withinday.replanning.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.replanning.modules.ReplanningModule;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.HouseholdPosition;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.Tracker.Position;

public class SelectHouseholdMeetingPointRunner implements Runnable {

	private static final Logger log = Logger.getLogger(SelectHouseholdMeetingPointRunner.class);
	
	private final Scenario scenario;
	private final HouseholdsTracker householdsTracker;
	private final VehiclesTracker vehiclesTracker;
	private final CoordAnalyzer coordAnalyzer;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final PlanAlgorithm planAlgo;
	
	private final List<Household> householdsToCheck;
	private double time;
	
	public SelectHouseholdMeetingPointRunner(Scenario scenario, ReplanningModule replanningModule,
			HouseholdsTracker householdsTracker, VehiclesTracker vehiclesTracker, CoordAnalyzer coordAnalyzer) {
		this.scenario = scenario;
		this.householdsTracker = householdsTracker;
		this.vehiclesTracker = vehiclesTracker;
		this.coordAnalyzer = coordAnalyzer;
		
		this.modeAvailabilityChecker = new ModeAvailabilityChecker(scenario, vehiclesTracker);
		this.planAlgo = replanningModule.getPlanAlgoInstance();
		
		this.householdsToCheck = new ArrayList<Household>();
	}
	
	@Override
	public void run() {
		for (Household household : householdsToCheck) {
			
			HouseholdDecisionData hdd = new HouseholdDecisionData(household, this.householdsTracker, this.vehiclesTracker);
			Id homeFacilityId = hdd.householdPosition.getHomeFacilityId();
			ActivityFacility homeFacility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(homeFacilityId);
			hdd.homeFacilityIsAffected = this.coordAnalyzer.isFacilityAffected(homeFacility);
			
			/*
			 * So far we assume that all households outside the affected area follow
			 * the evacuation order and evacuate to their home directly.
			 */
			if (!hdd.homeFacilityIsAffected) {
				hdd.householdPosition.setMeetingPointFacilityId(hdd.householdPosition.getHomeFacilityId());
				continue;
			}
			
			calculateHouseholdReturnHomeTime(hdd, time);
			calculateLatestAcceptedLeaveTime(hdd, time);
			
			selectHouseholdMeetingPoint(hdd, time);
		}
	}
	
	public void setTime(double time) {
		this.time = time;
	}
	
	/*
	 * TODO implement a decision model based on the survey data...
	 */
	private void selectHouseholdMeetingPoint(HouseholdDecisionData hdd, double time) {
		
		int affected = 0;
		int notAffected = 0;
				
		for (PersonDecisionData pdd : hdd.personDecisionData.values()) {
			if (pdd.isAffected) affected++;
			else notAffected++;
		}
		
		/*
		 * All agents are outside the affected area. They decide to stay outside.
		 */
		if (affected == 0) {
			Id newMeetingPointId = scenario.createId("rescueFacility");
			hdd.householdPosition.setMeetingPointFacilityId(newMeetingPointId);
		} 
		/*
		 * So far: if at least one agent is inside the affected area, the household meets at home.
		 */
		else {
			hdd.householdPosition.setMeetingPointFacilityId(hdd.householdPosition.getHomeFacilityId());
		}
	}
	
	private void calculateLatestAcceptedLeaveTime(HouseholdDecisionData hdd, double time) {
		// TODO
	}
	
	private void calculateHouseholdReturnHomeTime(HouseholdDecisionData hdd, double time) {
		
		Household household = hdd.household;
		HouseholdPosition householdPosition = householdsTracker.getHouseholdPosition(household.getId());
		Id homeFacilityId = householdPosition.getHomeFacilityId();
		Id homeLinkId = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(homeFacilityId).getLinkId();
		
		double returnHomeTime = Double.MIN_VALUE;
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = hdd.personDecisionData.get(personId);
			calculateAgentReturnHomeTime(pdd, homeLinkId, homeFacilityId, time);
			
			double t = pdd.agentReturnHomeTime;
			if (t > returnHomeTime) returnHomeTime = t;
		}
		hdd.householdReturnHomeTime = returnHomeTime;
	}
	
	private void calculateAgentReturnHomeTime(PersonDecisionData pdd, Id homeLinkId, Id homeFacilityId, double time) {
		
		Id personId = pdd.personId;
		AgentPosition agentPosition = householdsTracker.getAgentPosition(personId);
		Position positionType = agentPosition.getPositionType();
		
		/*
		 * Identify the transport mode that the agent could use for its trip home.
		 */
		Id fromLinkId = null;
		String mode = null;
		if (positionType == Position.LINK) {
			mode = agentPosition.getTransportMode();
			fromLinkId = agentPosition.getPositionId();
			
			Link link = this.scenario.getNetwork().getLinks().get(agentPosition.getPositionId());
			pdd.isAffected = this.coordAnalyzer.isLinkAffected(link);
			
		} else if (positionType == Position.FACILITY) {

			Id facilityId = agentPosition.getPositionId();
			ActivityFacility facility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(facilityId);
			pdd.isAffected = this.coordAnalyzer.isFacilityAffected(facility);

			/*
			 * If the current Activity is performed ad the homeFacilityId, the return
			 * home time is 0.0.
			 */
			if (facilityId.equals(homeFacilityId)) {
				pdd.agentReturnHomeTime = 0.0;
				return;
			}
			
			/*
			 * Otherwise the activity is performed at another facility. Get the link where
			 * the facility is attached to the network.
			 */
			// get the index of the currently performed activity in the selected plan
			MobsimAgent mobsimAgent = agentPosition.getAgent();
			PlanAgent planAgent = (PlanAgent) mobsimAgent;
			
			PlanImpl executedPlan = (PlanImpl) planAgent.getSelectedPlan();
			
			Activity currentActivity = (Activity) planAgent.getCurrentPlanElement();
			int currentActivityIndex = executedPlan.getActLegIndex(currentActivity);
			
			Id possibleVehicleId = getVehicleId(executedPlan);
			mode = this.modeAvailabilityChecker.identifyTransportMode(currentActivityIndex, executedPlan, possibleVehicleId);
			fromLinkId = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(agentPosition.getPositionId()).getLinkId();
		} else if (positionType == Position.VEHICLE) {
			/*
			 * Should not occur since passengers are only created after the evacuation has started.
			 */
			log.warn("Found passenger agent. Passengers should not be created before the evacuation has started!");
			return;
		} else {
			log.error("Found unknown position type: " + positionType.toString());
			return;						
		}
		
		/*
		 * Calculate the travel time from the agents current position to its home facility.
		 */
		double t = calculateTravelTime(fromLinkId, homeLinkId, mode, time);
		pdd.agentReturnHomeTime = t;
	}
	
	private double calculateEvacuationTimeFromHome(Household household, double time) {
		
		int householdSize = household.getMemberIds().size();
		int vehicleCapacity = 0;
		for (Id vehicleId : household.getVehicleIds()) {
			Vehicle vehicle = ((ScenarioImpl) scenario).getVehicles().getVehicles().get(vehicleId);
			vehicleCapacity += vehicle.getType().getCapacity().getSeats();
		}
		
		double vehicleTravelTime = Double.MAX_VALUE;
		
		if (vehicleCapacity > 0) {
//			calculateTravelTime
		}
		
		if (vehicleCapacity >= householdSize) {
			
		}
		
		return 0.0;
	}
	
	private double calculateTravelTime(Id fromLinkId, Id toLinkId, String mode, double time) {
		
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		Plan plan = factory.createPlan();
		Activity fromActivity = factory.createActivityFromLinkId("current", fromLinkId);
		Activity toActivity = factory.createActivityFromLinkId("home", toLinkId);
		Leg leg = factory.createLeg(mode);
		
		fromActivity.setEndTime(time);
		leg.setDepartureTime(time);
		
		plan.addActivity(fromActivity);
		plan.addLeg(leg);
		plan.addActivity(toActivity);
		
		planAlgo.run(plan);
		
		return time + leg.getTravelTime();
	}
	
	/*
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
	
	private static class HouseholdDecisionData {
		
		final Household household;
		final HouseholdPosition householdPosition;
		final Map<Id, PersonDecisionData> personDecisionData = new HashMap<Id, PersonDecisionData>();
		
		boolean homeFacilityIsAffected = false;
		double latestAcceptedLeaveTime = Double.MAX_VALUE;
		double householdReturnHomeTime = Double.MAX_VALUE;
		
		public HouseholdDecisionData(Household household, HouseholdsTracker householdsTracker, VehiclesTracker vehiclesTracker) {
			this.household = household;
			this.householdPosition = householdsTracker.getHouseholdPosition(household.getId());
			
			for (Id personId : household.getMemberIds()) {
				AgentPosition agentPosition = householdsTracker.getAgentPosition(personId);
				personDecisionData.put(personId, new PersonDecisionData(personId, agentPosition));
			}
		}
	}
	
	private static class PersonDecisionData {
		
		final Id personId;
		final AgentPosition agentPosition;
		
		boolean isAffected = false;
		double agentReturnHomeTime = Double.MAX_VALUE;
		
		public PersonDecisionData(Id personId, AgentPosition agentPosition) {
			this.personId = personId;
			this.agentPosition = agentPosition;
		}
		
	}
}
