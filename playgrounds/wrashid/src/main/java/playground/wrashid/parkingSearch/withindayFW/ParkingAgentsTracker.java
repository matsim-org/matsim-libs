/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingAgentsTracker.java
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

package playground.wrashid.parkingSearch.withindayFW;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.HashMapHashSetConcat;
import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingChoice.ParkingManager;
import playground.wrashid.parkingSearch.withinday.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.impl.ParkingStrategyManager;
import playground.wrashid.parkingSearch.withindayFW.randomTestStrategyFW.ParkingStrategy;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

// TODO: clearly inspect, which variables have not been reset at beginning of 1st iteration (after 0th iteration).
public class ParkingAgentsTracker implements LinkEnterEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler,
		MobsimInitializedListener, MobsimAfterSimStepListener, ActivityEndEventHandler, AfterMobsimListener, ActivityStartEventHandler {

	private final Scenario scenario;
	private final double distance;

	private final Set<Id> carLegAgents;
	private final Set<Id> searchingAgents;
	private final Set<Id> linkEnteredAgents;
	private final Set<Id> lastTimeStepsLinkEnteredAgents;
	private final Map<Id, ActivityFacility> nextActivityFacilityMap;
	private final Map<Id, ExperimentalBasicWithindayAgent> agents;
	private final Map<Id, Id> selectedParkingsMap;
	private final Map<Id, Activity> nextNonParkingActivity;
	private final ParkingInfrastructure parkingInfrastructure;
	private Map<Id, Id> lastParkingFacilityId;
	private Map<Id, Double> lastCarArrivalTimeAtParking;
	private DoubleValueHashMap<Id> parkingIterationScoreSum;
	private ParkingStrategyManager parkingStrategyManager;
	private HashMapHashSetConcat<DuringLegIdentifier,Id> activeReplanningIdentifiers;
	private Map<Id, Double> previousNonParkingActivityStartTime;
	private Map<Id, Double> firstParkingWalkTime;
	private Map<Id, Double> secondParkingWalkTime;
	private Map<Id, Double> searchStartTime;
	private Map<Id, Double> lastCarMovementRegistered;

	/**
	 * Tracks agents' car legs and check whether they have to start their
	 * parking search.
	 * 
	 * @param scenario
	 * @param distance
	 *            defines in which distance to the destination of a car trip an
	 *            agent starts its parking search
	 * @param parkingInfrastructure
	 */
	public ParkingAgentsTracker(Scenario scenario, double distance, ParkingInfrastructure parkingInfrastructure) {
		this.scenario = scenario;
		this.distance = distance;
		this.parkingInfrastructure = parkingInfrastructure;

		this.carLegAgents = new HashSet<Id>();
		this.linkEnteredAgents = new HashSet<Id>();
		this.selectedParkingsMap = new HashMap<Id, Id>();
		this.lastTimeStepsLinkEnteredAgents = new TreeSet<Id>(); // This set has
																	// to be be
																	// deterministic!
		this.searchingAgents = new HashSet<Id>();
		this.nextActivityFacilityMap = new HashMap<Id, ActivityFacility>();
		this.agents = new HashMap<Id, ExperimentalBasicWithindayAgent>();
		this.nextNonParkingActivity = new HashMap<Id, Activity>();
		this.lastCarArrivalTimeAtParking = new HashMap<Id, Double>();
		this.parkingIterationScoreSum = new DoubleValueHashMap<Id>();
		this.setActiveReplanningIdentifiers(new HashMapHashSetConcat<DuringLegIdentifier, Id>());
		this.previousNonParkingActivityStartTime=new HashMap<Id, Double>();
		firstParkingWalkTime=new HashMap<Id, Double>();;
		secondParkingWalkTime=new HashMap<Id, Double>();
		this.setSearchStartTime(new HashMap<Id, Double>());
		this.lastParkingFacilityId=new HashMap<Id, Id>();
		this.lastCarMovementRegistered=new HashMap<Id, Double>();
	}

	public Set<Id> getSearchingAgents() {
		return Collections.unmodifiableSet(this.searchingAgents);
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (ExperimentalBasicWithindayAgent) agent);
		}
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		lastTimeStepsLinkEnteredAgents.clear();
		lastTimeStepsLinkEnteredAgents.addAll(linkEnteredAgents);
		linkEnteredAgents.clear();
	}

	public Set<Id> getLinkEnteredAgents() {
		return lastTimeStepsLinkEnteredAgents;
	}

	public void setSelectedParking(Id agentId, Id parkingFacilityId) {
		selectedParkingsMap.put(agentId, parkingFacilityId);
	}

	public Id getSelectedParking(Id agentId) {
		return selectedParkingsMap.get(agentId);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		
		if (event.getLegMode().equals(TransportMode.car)) {
			Id personId = event.getPersonId();
			getLastCarMovementRegistered().put(personId, event.getTime());
			parkingInfrastructure.unParkVehicle(lastParkingFacilityId.get(personId));

			this.carLegAgents.add(personId);

			ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
			Plan executedPlan = agent.getSelectedPlan();
			int planElementIndex = agent.getCurrentPlanElementIndex();
			
			TwoHashMapsConcatenated<Id, Integer, ParkingStrategy> currentlySelectedParkingStrategies = parkingStrategyManager.getCurrentlySelectedParkingStrategies();
			activeReplanningIdentifiers.put(currentlySelectedParkingStrategies.get(personId, planElementIndex).getIdentifier(), personId);
			
			/*
			 * Get the coordinate of the next non-parking activity's facility.
			 * The currentPlanElement is a car leg, which is followed by a
			 * parking activity and a walking leg to the next non-parking
			 * activity.
			 */
			Activity nextNonParkingActivity = (Activity) executedPlan.getPlanElements().get(planElementIndex + 3);
			this.getNextNonParkingActivity().put(agent.getId(), nextNonParkingActivity);

			ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities()
					.get(nextNonParkingActivity.getFacilityId());
			nextActivityFacilityMap.put(personId, facility);

			Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
			double distanceToNextActivity = CoordUtils.calcDistance(facility.getCoord(), coord);

			/*
			 * If the agent is within distance 'd' to target activity or OR If
			 * the agent enters the link where its next non-parking activity is
			 * performed, mark him ash searching Agent.
			 * 
			 * (this is actually handling a special case, where already at
			 * departure time the agent is within distance 'd' of next
			 * activity).
			 */
			if (shouldStartSearchParking(event.getLinkId(), facility.getLinkId(), distanceToNextActivity)) {
				searchingAgents.add(personId);
			}
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id personId = event.getPersonId();
		this.carLegAgents.remove(personId);
		this.searchingAgents.remove(personId);
		this.linkEnteredAgents.remove(personId);
		this.selectedParkingsMap.remove(personId);

		
		
		
		
		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		int planElementIndex = agent.getCurrentPlanElementIndex();
		TwoHashMapsConcatenated<Id, Integer, ParkingStrategy> currentlySelectedParkingStrategies = parkingStrategyManager.getCurrentlySelectedParkingStrategies();
		
		//Set<Integer> keySet2 = currentlySelectedParkingStrategies.getKeySet2(personId);
		
		if (event.getLegMode().equals(TransportMode.car)) {
			logParkingArrivalTime(event);
			getLastCarMovementRegistered().put(personId, event.getTime());
			activeReplanningIdentifiers.removeValue(currentlySelectedParkingStrategies.get(personId, planElementIndex).getIdentifier() , personId);
		}
		
	}

	private void logParkingArrivalTime(AgentArrivalEvent event) {
		ExperimentalBasicWithindayAgent agent = this.agents.get(event.getPersonId());
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();

		Leg previousLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex);
		Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex + 2);

		if (previousLeg.getMode().equals(TransportMode.car) && nextLeg.getMode().equals(TransportMode.walk)) {
			// car arrived
			lastCarArrivalTimeAtParking.put(event.getPersonId(), event.getTime());
		}

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id personId = event.getPersonId();
		getLastCarMovementRegistered().put(personId, event.getTime());
		if (carLegAgents.contains(personId)) {
			if (!searchingAgents.contains(personId)) {
				Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
				ActivityFacility facility = nextActivityFacilityMap.get(personId);
				double distanceToNextActivity = CoordUtils.calcDistance(facility.getCoord(), coord);

				/*
				 * If the agent is within the parking radius
				 */
				/*
				 * If the agent enters the link where its next non-parking
				 * activity is performed.
				 */

				if (shouldStartSearchParking(event.getLinkId(), facility.getLinkId(), distanceToNextActivity)) {
					searchingAgents.add(personId);
					linkEnteredAgents.add(personId);
					updateIdentifierOfAgentForParkingSearch(personId);
				}
			}
			// the agent is already searching: update its position
			else {
				linkEnteredAgents.add(personId);
				updateIdentifierOfAgentForParkingSearch(personId);
			}
		}
	}

	private void updateIdentifierOfAgentForParkingSearch(Id personId) {
		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		int planElementIndex = agent.getCurrentPlanElementIndex();
		
		getActiveReplanningIdentifiers().put(parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, planElementIndex).getIdentifier(), personId);
	}

	private boolean shouldStartSearchParking(Id currentLinkId, Id nextActivityLinkId, double distanceToNextActivity) {
		return distanceToNextActivity <= distance || nextActivityLinkId.equals(currentLinkId);
	}

	@Override
	public void reset(int iteration) {
		agents.clear();
		carLegAgents.clear();
		searchingAgents.clear();
		linkEnteredAgents.clear();
		selectedParkingsMap.clear();
		nextActivityFacilityMap.clear();
		lastTimeStepsLinkEnteredAgents.clear();
		this.parkingIterationScoreSum = new DoubleValueHashMap<Id>();
	}

	public Map<Id, Activity> getNextNonParkingActivity() {
		return nextNonParkingActivity;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equalsIgnoreCase("parking")) {
			lastParkingFacilityId.put(event.getPersonId(), event.getFacilityId());

			updateParkingScore(event);
		} else {
			ExperimentalBasicWithindayAgent agent = this.agents.get(event.getPersonId());
			Plan executedPlan = agent.getSelectedPlan();
			int planElementIndex = agent.getCurrentPlanElementIndex();

			
			Leg previousLeg = null;
			
			if (planElementIndex==0){
				previousLeg=(Leg) executedPlan.getPlanElements().get(executedPlan.getPlanElements().size() - 2);
			} else {
				previousLeg=(Leg) executedPlan.getPlanElements().get(planElementIndex - 1);
			}
			
			Leg nextLeg =null;
			
			if (planElementIndex==executedPlan.getPlanElements().size()-1){
				nextLeg = (Leg) executedPlan.getPlanElements().get(1);
			} else {
				nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex + 1);
			}
			
			if (previousLeg.getMode().equals(TransportMode.car) &&  nextLeg.getMode().equals(TransportMode.walk) && planElementIndex>0){
				firstParkingWalkTime.put(event.getPersonId(), GeneralLib.getIntervalDuration(firstParkingWalkTime.get(event.getPersonId()) , event.getTime()));
			}
			
			if (previousLeg.getMode().equals(TransportMode.walk) &&  nextLeg.getMode().equals(TransportMode.walk)){
				secondParkingWalkTime.put(event.getPersonId(), event.getTime());
			}
			
		}

	}

	private void updateParkingScore(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();

		Leg previousLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex - 1);
		Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex + 1);

		if (previousLeg.getMode().equals(TransportMode.car) &&  nextLeg.getMode().equals(TransportMode.walk)){
			firstParkingWalkTime.put(personId, event.getTime());
		}
		
		
		if (previousLeg.getMode().equals(TransportMode.walk) && nextLeg.getMode().equals(TransportMode.car) && planElementIndex>2) {
			secondParkingWalkTime.put(personId, GeneralLib.getIntervalDuration(secondParkingWalkTime.get(personId) , event.getTime()));
			
			Double parkingArrivalTime = lastCarArrivalTimeAtParking.get(personId);			
			double parkingDuration = GeneralLib.getIntervalDuration(parkingArrivalTime, event.getTime());

			Double parkingCost = parkingInfrastructure.getParkingCostCalculator().getParkingCost(event.getFacilityId(),
					parkingArrivalTime, parkingDuration);

			ParkingPersonalBetas parkingPersonalBetas = parkingStrategyManager.getParkingPersonalBetas();
			parkingIterationScoreSum.incrementBy(personId,parkingPersonalBetas.getParkingCostBeta(personId)*parkingCost);
			
			double activivityDurationInSeconds=GeneralLib.getIntervalDuration(previousNonParkingActivityStartTime.get(personId),event.getTime());
			
			//access and egress time (this can be asymmetric, therefore calculated twice).
			double walkingTimeTotalInMinutes=(firstParkingWalkTime.get(personId) +secondParkingWalkTime.get(personId))/60;
			parkingIterationScoreSum.incrementBy(personId,parkingPersonalBetas.getParkingWalkTimeBeta(personId, activivityDurationInSeconds)*walkingTimeTotalInMinutes);
			
			
			Double startIntervalTime = this.getSearchStartTime().get(personId);
			
			if (startIntervalTime==null){
				System.out.println("#######################");
				return;
			}
			
			double parkingSearchTimeInMinutes=GeneralLib.getIntervalDuration(startIntervalTime,parkingArrivalTime)/60; 
			parkingIterationScoreSum.incrementBy(personId,parkingPersonalBetas.getParkingSearchTimeBeta(personId, activivityDurationInSeconds)*parkingSearchTimeInMinutes);

			getSearchStartTime().remove(personId);
			
			System.out.println("######TODO!!!!!!!!!!!!!################");
			// TODO
			//==================
			// first/last activity walking times => how to handel?
			
			
			// wie kann man die parking behalten vom letzten abend parking fuer den naechsten tag?
			
			// genau begruenden, wieso es das braucht.
			
			
			// auch sagen, dass man nach jeder plan aenderung ggf. eine korrektur braucht, falls andere algorithmen das
			// nicht rightig machen.
			
			// 
		}

	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for (Id personId : this.parkingIterationScoreSum.keySet()) {
			ScoringFunction scoringFunction = event.getControler().getPlansScoring().getScoringFunctionForAgent(personId);
			scoringFunction.addMoney(parkingIterationScoreSum.get(personId));
		}
	}

	public ParkingStrategyManager getParkingStrategyManager() {
		return parkingStrategyManager;
	}
	public void setParkingStrategyManager(ParkingStrategyManager parkingStrategyManager) {
		this.parkingStrategyManager=parkingStrategyManager;
	}

	public HashMapHashSetConcat<DuringLegIdentifier,Id> getActiveReplanningIdentifiers() {
		return activeReplanningIdentifiers;
	}

	public void setActiveReplanningIdentifiers(HashMapHashSetConcat<DuringLegIdentifier,Id> activeReplanningIdentifiers) {
		this.activeReplanningIdentifiers = activeReplanningIdentifiers;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!event.getActType().equalsIgnoreCase("parking")){
			previousNonParkingActivityStartTime.put(event.getPersonId(), event.getTime());
		}
	}
	
	public void putSearchStartTime(Id personId, double searchStartTime){
		this.getSearchStartTime().put(personId,searchStartTime);
	}

	public Map<Id, Double> getSearchStartTime() {
		return searchStartTime;
	}

	public void setSearchStartTime(Map<Id, Double> searchStartTime) {
		this.searchStartTime = searchStartTime;
	}

	public Map<Id, Double> getLastCarMovementRegistered() {
		return lastCarMovementRegistered;
	}

	public void setLastCarMovementRegistered(Map<Id, Double> lastCarMovementRegistered) {
		this.lastCarMovementRegistered = lastCarMovementRegistered;
	}

}
