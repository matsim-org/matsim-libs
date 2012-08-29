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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
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
import playground.christoph.evacuation.mobsim.PassengerDepartureHandler;
import playground.christoph.evacuation.mobsim.Tracker.Position;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisiondata.PersonDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.EvacuationDecision;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;

/**
 * Decision logic for SelectHouseholdMeetingPoint class. Can be executed parallel
 * on multiple threads.
 *  
 * @author cdobler
 */
public class SelectHouseholdMeetingPointRunner implements Runnable {

	private static final Logger log = Logger.getLogger(SelectHouseholdMeetingPointRunner.class);
		
	private final Scenario scenario;
	private final VehiclesTracker vehiclesTracker;
	private final CoordAnalyzer coordAnalyzer;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final DecisionDataProvider decisionDataProvider;
	private final PlanAlgorithm toHomeFacilityPlanAlgo;
	private final PlanAlgorithm evacuationPlanAlgo;
	
	private final List<Household> householdsToCheck;
	private double time;
	
	private final CyclicBarrier startBarrier;
	private final CyclicBarrier endBarrier;
	private final AtomicBoolean allMeetingsPointsSelected;
	private final Random random;
	
	public SelectHouseholdMeetingPointRunner(Scenario scenario, ReplanningModule toHomeFacilityRouter, 
			ReplanningModule fromHomeFacilityRouter, VehiclesTracker vehiclesTracker, CoordAnalyzer coordAnalyzer, 
			ModeAvailabilityChecker modeAvailabilityChecker, DecisionDataProvider decisionDataProvider, 
			CyclicBarrier startBarrier, CyclicBarrier endBarrier, AtomicBoolean allMeetingsPointsSelected) {
		this.scenario = scenario;
		this.vehiclesTracker = vehiclesTracker;
		this.coordAnalyzer = coordAnalyzer;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.decisionDataProvider = decisionDataProvider;
		
		this.startBarrier = startBarrier;
		this.endBarrier = endBarrier;
		this.allMeetingsPointsSelected = allMeetingsPointsSelected;
		
		this.random = MatsimRandom.getLocalInstance();
		this.toHomeFacilityPlanAlgo = toHomeFacilityRouter.getPlanAlgoInstance();
		this.evacuationPlanAlgo = fromHomeFacilityRouter.getPlanAlgoInstance();
		
		this.householdsToCheck = new ArrayList<Household>();
	}
	
	@Override
	public void run() {
		
		/*
		 * The loop is ended when the "allMeetingsPointsSelected" Flag is set to false.
		 */
		while(true) {
			try {
				/*
				 * The Threads wait at the startBarrier until they are triggered in the 
				 * next TimeStep by the run() method in SelectHouseholdMeetingPoint.
				 */
				startBarrier.await();

				/*
				 * Check if all meeting points have selected. If yes, we can end the threads.
				 */
				if (allMeetingsPointsSelected.get()) {
					return;
				}
				
				/*
				 * Check all households that have been informed in the current time step
				 * and therefore have to be checked.
				 */
				for (Household household : householdsToCheck) {
					
					/*
					 * Set seed in random object based on households id and current simulation time.
					 * This should be deterministic and not depend on the thread that handles the
					 * household.
					 */
					random.setSeed(household.getId().hashCode() + (long) time);
					
					HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId());
					
					boolean householdParticipates;
					Participating participating = this.decisionDataProvider.getHouseholdDecisionData(household.getId()).getParticipating();
					if (participating == Participating.TRUE) householdParticipates = true;
					else if (participating == Participating.FALSE) householdParticipates = false;
					else throw new RuntimeException("Households participation state is undefined: " + household.getId().toString());
					
					/*
					 * If the household does not evacuate, set its meeting point to its home facility.
					 */
					if (!householdParticipates) {
						hdd.setMeetingPointFacilityId(hdd.getHomeFacilityId());
						continue;
					}
					
					/*
					 * So far we assume that all households who's home facility is not affected
					 * follow the evacuation order and evacuate directly to their home facility.
					 */
					if (!hdd.isHomeFacilityIsAffected()) {
						hdd.setMeetingPointFacilityId(hdd.getHomeFacilityId());
						continue;
					}
					
					/*
					 * The household evacuates but its home facility is located inside the affected area.
					 * 
					 * Therefore we compare the evacuation times "current location -> home -> rescue" to
					 * "current location -> rescue". The first option allows households to meet at home and
					 * evacuate united which is preferred by them. Households might be even willing to accept
					 * longer evacuation times if this allows them to meet at home.
					 */
					calculateHouseholdTimes(household, time);
					calculateLatestAcceptedLeaveTime(household, time);
					
					calculateHouseholdReturnHomeTime(household, time);
					calculateEvacuationTimeFromHome(household, hdd.getHouseholdReturnHomeTime());
					calculateHouseholdDirectEvacuationTime(household, time);
					
					selectHouseholdMeetingPoint(household, time);
				}
				
				// clear list for next time step
				householdsToCheck.clear();
				
				/*
				 * The End of the Moving is synchronized with the endBarrier. If all Threads 
				 * reach this Barrier the main Thread can go on.
				 */
				endBarrier.await();
			} catch (InterruptedException e) {
				Gbl.errorMsg(e);
			} catch (BrokenBarrierException e) {
            	Gbl.errorMsg(e);
            }
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
	private void selectHouseholdMeetingPoint(Household household, double time) {
		
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId());
		EvacuationDecision evacuationDecision = hdd.getEvacuationDecision();
		
		/*
		 * If the household does not evacuate, we do not check other influence factors like
		 * the household members' current positions. We set the meeting point to the
		 * household's home facility.
		 */
		if (evacuationDecision == EvacuationDecision.NEVER) {
			hdd.setMeetingPointFacilityId(hdd.getHomeFacilityId());
			return;
		}
		
		/*
		 * Count number of agents affected and not affected.
		 */
		int affected = 0;
		int notAffected = 0;
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = this.decisionDataProvider.getPersonDecisionData(personId);
			if (pdd.isAffected()) affected++;
			else notAffected++;
		}
		
		Id newMeetingPointId = scenario.createId("rescueFacility");
		
		/*
		 * If all agents of the household are outside the affected area, they decide to stay outside.
		 * TODO: if there is enough time available, they might decide to still meet at home.
		 */
		if (affected == 0) {
			hdd.setMeetingPointFacilityId(newMeetingPointId);
		}
		
		/*
		 * The household compare its evacuation options: meet at home and leave together or leave
		 * immediately - and probably split - and meet outside.
		 */
		else {
			double fromHome = hdd.getHouseholdEvacuateFromHomeTime();
			double direct = hdd.getHouseholdDirectEvacuationTime();
			
			/*
			 * If the household can meet at home and leave the evacuation area before
			 * the latest accepted evacuation time, the household will meet at home.
			 * If not, the household selects the faster alternative between direct
			 * evacuation and evacuation via meeting at home.
			 */
			if (hdd.getHouseholdEvacuateFromHomeTime() < hdd.getLatestAcceptedLeaveTime()) {
				hdd.setMeetingPointFacilityId(hdd.getHomeFacilityId());
			} else {
				if (direct < fromHome) {
					hdd.setMeetingPointFacilityId(newMeetingPointId);
				} else {
					hdd.setMeetingPointFacilityId(hdd.getHomeFacilityId());
				}				
			}
		}
	}
	
	/*
	 * The time until a household wants to have left the affected area. 
	 * Note: this is NOT the time when the household wants to start its evacuation trip!
	 */
	private void calculateLatestAcceptedLeaveTime(Household household, double time) {
		
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId());
		EvacuationDecision evacuationDecision = hdd.getEvacuationDecision();

		if (evacuationDecision == EvacuationDecision.IMMEDIATELY) {
			hdd.setLatestAcceptedLeaveTime(EvacuationConfig.evacuationTime);
		} 
		/*
		 * The household defines its latest accepted evacuation time as:
		 * (latest time given by the government (e.g. 8 hours time) - current time) * 0.75..1.00
		 * As a result households 
		 */
		else if (evacuationDecision == EvacuationDecision.LATER) {
			
			// calculate random double value between 0.75 and 1.0
			double rand = 0.75 + (this.random.nextDouble() / 4);
			
			double dt = EvacuationConfig.evacuationDelayTime * rand;
			hdd.setLatestAcceptedLeaveTime(EvacuationConfig.evacuationTime + dt);			
		} else if (evacuationDecision == EvacuationDecision.NEVER) {
			hdd.setLatestAcceptedLeaveTime(EvacuationConfig.evacuationTime);
		} else {
			throw new RuntimeException("Household's EvacuationDecision is undefined " + evacuationDecision);
		}
		
		// old behavioral model
//		hdd.setLatestAcceptedLeaveTime(EvacuationConfig.evacuationTime + 2*3600);
	}
	
	private void calculateHouseholdReturnHomeTime(Household household, double time) {
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId());
		
		double returnHomeTime = Double.MIN_VALUE;
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = this.decisionDataProvider.getPersonDecisionData(personId);
			
			double t = pdd.getAgentReturnHomeTime();
			if (t > returnHomeTime) returnHomeTime = t;
		}
		hdd.setHouseholdReturnHomeTime(returnHomeTime);
	}
	
	private void calculateHouseholdDirectEvacuationTime(Household household, double time) {
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId());
		
		double agentDirectEvacuationTime = Double.MIN_VALUE;
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = this.decisionDataProvider.getPersonDecisionData(personId);
			
			double t = pdd.getAgentDirectEvacuationTime();
			if (t > agentDirectEvacuationTime) agentDirectEvacuationTime = t;
		}
		hdd.setHouseholdDirectEvacuationTime(agentDirectEvacuationTime);
	}	
	
	/*
	 * Calculates (estimates) the time until all members of a household have returned home.
	 */
	private void calculateHouseholdTimes(Household household, double time) {
		
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId());
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = this.decisionDataProvider.getPersonDecisionData(personId);
			calculateAgentTimes(pdd, personId, hdd.getHomeLinkId(), hdd.getHomeFacilityId(), time);
		}
	}
	
	/*
	 * Calculates (estimates) the time until a person has returned home.
	 */
	private void calculateAgentTimes(PersonDecisionData pdd, Id personId, Id homeLinkId, Id homeFacilityId, double time) {

		AgentPosition agentPosition = this.decisionDataProvider.getPersonDecisionData(personId).getAgentPosition();
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
			pdd.setAffected(this.coordAnalyzer.isLinkAffected(link));
		} else if (positionType == Position.FACILITY) {

			Id facilityId = agentPosition.getPositionId();
			ActivityFacility facility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(facilityId);
			pdd.setAffected(this.coordAnalyzer.isFacilityAffected(facility));
			
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
				pdd.setAgentReturnHomeVehicleId(possibleVehicleId);
			}	
		} else if (positionType == Position.VEHICLE) {
			mode = agentPosition.getTransportMode();
			
			if (!mode.equals(TransportMode.car)) {
				throw new RuntimeException("Agent's position is VEHICLE but its transport mode is " + mode);
			}
			
			Id vehicleId = agentPosition.getPositionId();
			pdd.setAgentReturnHomeVehicleId(vehicleId);
			fromLinkId = this.vehiclesTracker.getVehicleLinkId(vehicleId);
			
			Link link = this.scenario.getNetwork().getLinks().get(fromLinkId);
			pdd.setAffected(this.coordAnalyzer.isLinkAffected(link));
		} else {
			log.error("Found unknown position type: " + positionType.toString());
			return;						
		}
		
		/*
		 * Calculate the travel time from the agents current position to its home facility.
		 */
		if (needsHomeRouting) {
			double t = calculateTravelTime(toHomeFacilityPlanAlgo, personId, fromLinkId, homeLinkId, mode, time);
			pdd.setAgentReturnHomeTime(t);
		} else pdd.setAgentReturnHomeTime(time);
		pdd.setAgentTransportMode(mode);
		
		/*
		 * Calculate the travel time from the agents current position to a secure place.
		 */
		if (!pdd.isAffected()) pdd.setAgentDirectEvacuationTime(0.0);
		else {
			Id toLinkId = this.scenario.createId("exitLink");
			double t = calculateTravelTime(evacuationPlanAlgo, personId, fromLinkId, toLinkId, mode, time);
			pdd.setAgentDirectEvacuationTime(t);
		}
	}
	
	/*
	 * Calculates (estimates) a households evacuation time from home to a rescue facility.
	 */
	private void calculateEvacuationTimeFromHome(Household household, double time) {
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(household.getId());
		
		/*
		 * Get all vehicles that are already located at the home facility.
		 * Then add all vehicles that are used by agents to return to
		 * the home facility.
		 */
		Set<Id> availableVehicles = new HashSet<Id>();
		availableVehicles.addAll(this.modeAvailabilityChecker.getAvailableCars(household, hdd.getHomeFacilityId()));
		for (Id personId : household.getMemberIds()) {
			PersonDecisionData pdd = this.decisionDataProvider.getPersonDecisionData(personId);
			if (pdd.getAgentReturnHomeVehicleId() != null) {
				availableVehicles.add(pdd.getAgentReturnHomeVehicleId());
			}
		}
		
		if (availableVehicles.size() > household.getVehicleIds().size()) {
			throw new RuntimeException("To many available vehicles identified!");
		}
		
		HouseholdModeAssignment assignment = this.modeAvailabilityChecker.getHouseholdModeAssignment(household.getMemberIds(), 
				availableVehicles, hdd.getHomeFacilityId());
		
		Id toLinkId = this.scenario.createId("exitLink");
		double vehicularTravelTime = Double.MIN_VALUE;
		double nonVehicularTravelTime = Double.MIN_VALUE;
		
		for (Entry<Id, String> entry : assignment.getTransportModeMap().entrySet()) {
			String mode = entry.getValue();
			if (mode.equals(TransportMode.car)) {
				// Calculate a car travel time only once since it should not be person dependent.
				if (vehicularTravelTime == Double.MIN_VALUE) {
					vehicularTravelTime = calculateTravelTime(evacuationPlanAlgo, entry.getKey(), hdd.getHomeLinkId(), toLinkId, mode, time);
				} else continue;
			}
			else if (mode.equals(TransportMode.ride)) continue;
			else if (mode.equals(PassengerDepartureHandler.passengerTransportMode)) continue;
			else {
				double tt = calculateTravelTime(evacuationPlanAlgo, entry.getKey(), hdd.getHomeLinkId(), toLinkId, mode, time);
				if (tt > nonVehicularTravelTime) nonVehicularTravelTime = tt;
			}
		}
		
		hdd.setHouseholdEvacuateFromHomeTime(Math.max(vehicularTravelTime, nonVehicularTravelTime));
	}
	
	/*
	 * Calculates (estimates) the travel time from a fromLink to a toLink using a given mode.
	 */
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
	 * Returns the id of the first vehicle used by the agent. Without Within-Day Replanning, 
	 * an agent will use the same vehicle during the whole day. When Within-Day Replanning
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