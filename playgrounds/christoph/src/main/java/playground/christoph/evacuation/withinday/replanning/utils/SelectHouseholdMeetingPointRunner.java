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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import org.matsim.withinday.replanning.modules.ReplanningModule;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.HouseholdPosition;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.PassengerDepartureHandler;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.Tracker.Position;

public class SelectHouseholdMeetingPointRunner implements Runnable {

	private static final Logger log = Logger.getLogger(SelectHouseholdMeetingPointRunner.class);
	
	private final Scenario scenario;
	private final HouseholdsTracker householdsTracker;
	private final VehiclesTracker vehiclesTracker;
	private final CoordAnalyzer coordAnalyzer;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final PlanAlgorithm toHomeFacilityPlanAlgo;
	private final PlanAlgorithm evacuationPlanAlgo;
	
	private final List<Household> householdsToCheck;
	private double time;
	
	public SelectHouseholdMeetingPointRunner(Scenario scenario, ReplanningModule toHomeFacilityRouter, 
			ReplanningModule fromHomeFacilityRouter, HouseholdsTracker householdsTracker, 
			VehiclesTracker vehiclesTracker, CoordAnalyzer coordAnalyzer, ModeAvailabilityChecker modeAvailabilityChecker) {
		this.scenario = scenario;
		this.householdsTracker = householdsTracker;
		this.vehiclesTracker = vehiclesTracker;
		this.coordAnalyzer = coordAnalyzer;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		
		this.toHomeFacilityPlanAlgo = toHomeFacilityRouter.getPlanAlgoInstance();
		this.evacuationPlanAlgo = fromHomeFacilityRouter.getPlanAlgoInstance();
		
		this.householdsToCheck = new ArrayList<Household>();
	}
	
	@Override
	public void run() {
		for (Household household : householdsToCheck) {
			
			HouseholdDecisionData hdd = new HouseholdDecisionData(household, this.scenario, this.householdsTracker, this.vehiclesTracker);
			Id homeFacilityId = hdd.homeFacilityId;
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
			
			calculateHouseholdTimes(hdd, time);
			calculateLatestAcceptedLeaveTime(hdd, time);
			
			calculateHouseholdReturnHomeTime(hdd, time);
			calculateEvacuationTimeFromHome(hdd, hdd.householdReturnHomeTime);
			calculateHouseholdDirectEvacuationTime(hdd, time);
			
			selectHouseholdMeetingPoint(hdd, time);
		}
	}
	
	public void setTime(double time) {
		this.time = time;
	}
	
	public void addHouseholdToCheck(Household household) {
		this.householdsToCheck.add(household);
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
		
		Id newMeetingPointId = scenario.createId("rescueFacility");
		/*
		 * All agents are outside the affected area. They decide to stay outside.
		 */
		if (affected == 0) {
			hdd.householdPosition.setMeetingPointFacilityId(newMeetingPointId);
		} 
		/*
		 * So far: if at least one agent is inside the affected area, the household meets at home.
		 */
		else {
			double fromHome = hdd.householdEvacuateFromHomeTime;
			double direct = hdd.householdDirectEvacuationTime;
			
			/*
			 * If the household can meet at home and leave the evacuation area before
			 * the latest accepted evacuation time, the household will meet at home.
			 * If not, the household selects the faster alternative between direct
			 * evacuation and evacuation via meeting at home.
			 */
			if (hdd.householdEvacuateFromHomeTime < hdd.latestAcceptedLeaveTime) {
				hdd.householdPosition.setMeetingPointFacilityId(hdd.householdPosition.getHomeFacilityId());
			} else {
				if (direct < fromHome) {
					hdd.householdPosition.setMeetingPointFacilityId(newMeetingPointId);
				} else {
					hdd.householdPosition.setMeetingPointFacilityId(hdd.householdPosition.getHomeFacilityId());				
				}				
			}
		}
	}
	
	/*
	 * TODO: add a model to calculate this value...
	 */
	private void calculateLatestAcceptedLeaveTime(HouseholdDecisionData hdd, double time) {
		hdd.latestAcceptedLeaveTime = EvacuationConfig.evacuationTime + 2*3600;
	}
	
	private void calculateHouseholdReturnHomeTime(HouseholdDecisionData hdd, double time) {
		Household household = hdd.household;
		
		double returnHomeTime = Double.MIN_VALUE;
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = hdd.personDecisionData.get(personId);
			
			double t = pdd.agentReturnHomeTime;
			if (t > returnHomeTime) returnHomeTime = t;
		}
		hdd.householdReturnHomeTime = returnHomeTime;
	}
	
	private void calculateHouseholdDirectEvacuationTime(HouseholdDecisionData hdd, double time) {
		Household household = hdd.household;
		
		double agentDirectEvacuationTime = Double.MIN_VALUE;
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = hdd.personDecisionData.get(personId);
			
			double t = pdd.agentDirectEvacuationTime;
			if (t > agentDirectEvacuationTime) agentDirectEvacuationTime = t;
		}
		hdd.householdDirectEvacuationTime = agentDirectEvacuationTime;
	}	
	
	private void calculateHouseholdTimes(HouseholdDecisionData hdd, double time) {
		
		Household household = hdd.household;
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = hdd.personDecisionData.get(personId);
			calculateAgentTimes(pdd, hdd.homeLinkId, hdd.homeFacilityId, time);
		}
	}
	
	private void calculateAgentTimes(PersonDecisionData pdd, Id homeLinkId, Id homeFacilityId, double time) {
		
		Id personId = pdd.personId;
		AgentPosition agentPosition = householdsTracker.getAgentPosition(personId);
		Position positionType = agentPosition.getPositionType();
		
		/*
		 * Identify the transport mode that the agent could use for its trip home.
		 */
		Id fromLinkId = null;
		String mode = null;
		boolean needsHomeRouting = true;
		if (positionType == Position.LINK) {
			mode = agentPosition.getTransportMode();
			fromLinkId = agentPosition.getPositionId();
			
			Link link = this.scenario.getNetwork().getLinks().get(fromLinkId);
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
				needsHomeRouting = false;
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
			
			/*
			 * If the agent has a vehicle available, the car will be available at the
			 * home location for the evacuation. 
			 */
			if (mode.equals(TransportMode.car)) {
				pdd.agentReturnHomeVehicleId = possibleVehicleId;
			}	
		} else if (positionType == Position.VEHICLE) {
			mode = agentPosition.getTransportMode();
			
			if (!mode.equals(TransportMode.car)) {
				throw new RuntimeException("Agent's position is VEHICLE but its transport mode is " + mode);
			}
			
			Id vehicleId = agentPosition.getPositionId();
			pdd.agentReturnHomeVehicleId = vehicleId;
			fromLinkId = this.vehiclesTracker.getVehicleLinkId(vehicleId);
			
			Link link = this.scenario.getNetwork().getLinks().get(fromLinkId);
			pdd.isAffected = this.coordAnalyzer.isLinkAffected(link);
		} else {
			log.error("Found unknown position type: " + positionType.toString());
			return;						
		}
		
		/*
		 * Calculate the travel time from the agents current position to its home facility.
		 */
		if (needsHomeRouting) {
			double t = calculateTravelTime(toHomeFacilityPlanAlgo, personId, fromLinkId, homeLinkId, mode, time);
			pdd.agentReturnHomeTime = t;
		} else pdd.agentReturnHomeTime = time;
		pdd.agentTransportMode = mode;
		
		/*
		 * Calculate the travel time from the agents current position to a secure place.
		 */
		if (!pdd.isAffected) pdd.agentDirectEvacuationTime = 0.0;
		else {
			Id toLinkId = this.scenario.createId("exitLink");
			double t = calculateTravelTime(evacuationPlanAlgo, personId, fromLinkId, toLinkId, mode, time);
			pdd.agentDirectEvacuationTime = t;
		}
	}
	
	private void calculateEvacuationTimeFromHome(HouseholdDecisionData hdd, double time) {
				
		Household household = hdd.household;
		
		/*
		 * Get all vehicles that are already located at the home facility.
		 * Then add all vehicles that are used by agents to return to
		 * the home facility.
		 */
		Set<Id> availableVehicles = new HashSet<Id>();
		availableVehicles.addAll(this.modeAvailabilityChecker.getAvailableCars(household, hdd.homeFacilityId)); 
		for (PersonDecisionData pdd : hdd.personDecisionData.values()) {
			if (pdd.agentReturnHomeVehicleId != null) {
				availableVehicles.add(pdd.agentReturnHomeVehicleId);
			}
		}
		
		if (availableVehicles.size() > household.getVehicleIds().size()) {
			throw new RuntimeException("To many available vehicles identified!");
		}
		
		HouseholdModeAssignment assignment = this.modeAvailabilityChecker.getHouseholdModeAssignment(household.getMemberIds(), availableVehicles, hdd.homeFacilityId);
		
		Id toLinkId = this.scenario.createId("exitLink");
		double vehicularTravelTime = Double.MIN_VALUE;
		double nonVehicularTravelTime = Double.MIN_VALUE;
		
		for (Entry<Id, String> entry : assignment.getTransportModeMap().entrySet()) {
			String mode = entry.getValue();
			if (mode.equals(TransportMode.car)) {
				// Calculate a car travel time only once since it should not be person dependent.
				if (vehicularTravelTime == Double.MIN_VALUE) {
					vehicularTravelTime = calculateTravelTime(evacuationPlanAlgo, entry.getKey(), hdd.homeLinkId, toLinkId, mode, time);
				} else continue;
			}
			else if (mode.equals(TransportMode.ride)) continue;
			else if (mode.equals(PassengerDepartureHandler.passengerTransportMode)) continue;
			else {
				double tt = calculateTravelTime(evacuationPlanAlgo, entry.getKey(), hdd.homeLinkId, toLinkId, mode, time);
				if (tt > nonVehicularTravelTime) nonVehicularTravelTime = tt;
			}
		}
		
		hdd.householdEvacuateFromHomeTime = Math.max(vehicularTravelTime, nonVehicularTravelTime);
	}
	
	private double calculateTravelTime(PlanAlgorithm planAlgo, Id personId, Id fromLinkId, Id toLinkId, String mode, double time) {
		
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		Plan plan = factory.createPlan();
		plan.setPerson(scenario.getPopulation().getPersons().get(personId));
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
		
		final Id homeLinkId;
		final Id homeFacilityId;
		final Household household;
		final HouseholdPosition householdPosition;
		final Map<Id, PersonDecisionData> personDecisionData = new HashMap<Id, PersonDecisionData>();
		
		boolean homeFacilityIsAffected = false;
		double latestAcceptedLeaveTime = Double.MAX_VALUE;
		double householdReturnHomeTime = Double.MAX_VALUE;
		double householdEvacuateFromHomeTime = Double.MAX_VALUE;
		double householdDirectEvacuationTime = Double.MAX_VALUE;
		
		public HouseholdDecisionData(Household household, Scenario scenario, HouseholdsTracker householdsTracker, VehiclesTracker vehiclesTracker) {
			this.household = household;
			this.householdPosition = householdsTracker.getHouseholdPosition(household.getId());
			this.homeFacilityId = householdPosition.getHomeFacilityId();
			this.homeLinkId = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(homeFacilityId).getLinkId();
			
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
		double agentDirectEvacuationTime = Double.MAX_VALUE;
		String agentTransportMode = null;
		Id agentReturnHomeVehicleId = null;
		
		public PersonDecisionData(Id personId, AgentPosition agentPosition) {
			this.personId = personId;
			this.agentPosition = agentPosition;
		}
		
	}
}
