/* *********************************************************************** *
 * project: org.matsim.*
 * RandomSearchIdentifier.java
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

package playground.wrashid.parkingSearch.withindayFW.psHighestUtilityParkingChoice;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;

import playground.wrashid.parkingSearch.withindayFW.core.ParkingAgentsTracker;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.util.ActivityDurationEstimator;

public class HUPCIdentifier extends DuringLegAgentSelector implements MobsimInitializedListener {
	
	private final ParkingAgentsTracker parkingAgentsTracker;
	private final ParkingInfrastructure parkingInfrastructure;
	private final Map<Id, MobsimAgent> agents;
	private final WithinDayAgentUtils withinDayAgentUtils;
	
	public HUPCIdentifier(ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructure parkingInfrastructure) {
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.parkingInfrastructure = parkingInfrastructure;
		
		this.withinDayAgentUtils = new WithinDayAgentUtils();
		this.agents = new HashMap<Id, MobsimAgent>();
	}
	
	
	/*
	 * Put stuff here, which cannot run in parallel, the rest you can put in replanner. 
	 */
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {
		/*
		 * Get all agents that are searching and have entered a new link in the last
		 * time step.
		 */
		//Set<Id> linkEnteredAgents = this.parkingAgentsTracker.getLinkEnteredAgents();
		Set<MobsimAgent> identifiedAgents = new HashSet<MobsimAgent>();
		
		Set<Id> searchingAgentsAssignedToThisIdentifier = this.parkingAgentsTracker.getActiveReplanningIdentifiers().getValueSet(this);
		
		if (this.getAgentSelectorFactory()==null && searchingAgentsAssignedToThisIdentifier==null){
			return identifiedAgents;
		} else {
			//System.out.println();
		}
		
		
		
		
		for (Id agentId : searchingAgentsAssignedToThisIdentifier) {
			MobsimAgent agent = this.agents.get(agentId);
			
			Id personId =agent.getId();
			Plan selectedPlan = this.withinDayAgentUtils.getModifiablePlan(agent);
			List<PlanElement> planElements = selectedPlan.getPlanElements();
			Integer currentPlanElementIndex = this.withinDayAgentUtils.getCurrentPlanElementIndex(agent);
			
			if (this.withinDayAgentUtils.getCurrentPlanElementIndex(agents.get(personId)) == 33) {

				DebugLib.traceAgent(personId, 10);
				
				// DebugLib.traceAgent(personId);
			}
			
			/*
			 * If the agent has not selected a parking facility yet.
			 */
			if (requiresReplanning(agent)) {
				
				markFlagForNoSearchTime(agentId);
				
				
				
				
				// get all parking within 1000m (of destination) or at least on parking, if that set is empty.
				Activity nextNonParkingAct=(Activity) selectedPlan.getPlanElements().get(currentPlanElementIndex+3);
				
				Collection<ActivityFacility> parkings = parkingInfrastructure.getAllFreeParkingWithinDistance(1000, nextNonParkingAct.getCoord());
				if (parkings.size()==0){
					parkings.add(parkingInfrastructure.getClosestFreeParkingFacility(nextNonParkingAct.getCoord()));
				}
				
				
				// get best parking
				PriorityQueue<SortableMapObject<ActivityFacility>> priorityQueue = new PriorityQueue<SortableMapObject<ActivityFacility>>();
				
				
				for (ActivityFacility parkingFacility:parkings){
					double walkingDistance=GeneralLib.getDistance(parkingFacility.getCoord(), nextNonParkingAct.getCoord());
					
					double parkingDuration;
					if (isLastParkingOfDay(personId)){
						parkingDuration=ActivityDurationEstimator.estimateActivityDurationLastParkingOfDay(time, parkingAgentsTracker.getFirstCarDepartureTimeOfDay().getTime(agentId));
					} else {
						parkingDuration=ActivityDurationEstimator.estimateActivityDurationParkingDuringDay(time, planElements, currentPlanElementIndex);
					}
					
					double activityDuration=ActivityDurationEstimator.estimateActivityDurationParkingDuringDay(time, planElements, currentPlanElementIndex);
					double walkScore = parkingAgentsTracker.getWalkScore(personId, activityDuration, GeneralLib.getWalkingTravelDuration(walkingDistance));
					double costScore = parkingAgentsTracker.getParkingCostScore(personId,time , parkingDuration, parkingFacility.getId());
					
					priorityQueue.add(new SortableMapObject<ActivityFacility>(parkingFacility, walkScore+costScore));
				}
				
				ActivityFacility bestParkingFacility = priorityQueue.poll().getKey();
				
				Id facilityId = bestParkingFacility.getId();
				
				if (facilityId != null) {
					parkingInfrastructure.parkVehicle(facilityId);
					parkingAgentsTracker.setSelectedParking(agentId, facilityId);
					identifiedAgents.add(agent);
				}
				
			}
		}
		
		return identifiedAgents;
	}


	private boolean isLastParkingOfDay(Id personId) {
		MobsimAgent agent = this.agents.get(personId);
		
		List<PlanElement> planElements = this.withinDayAgentUtils.getModifiablePlan(agent).getPlanElements();
		Integer currentPlanElementIndex = this.withinDayAgentUtils.getCurrentPlanElementIndex(agent);
		
		for (int i=planElements.size()-1;i>0;i--){
			if (planElements.get(i) instanceof Leg){
				Leg leg=(Leg) planElements.get(i);
				
				if (leg.getMode().equals(TransportMode.car) && i==currentPlanElementIndex){
					return true;
				} else {
					return false;
				}
			}
		}
		
		DebugLib.stopSystemAndReportInconsistency("assumption broken");
		
		return false;
	}


	private void markFlagForNoSearchTime(Id agentId) {
		if (!parkingAgentsTracker.getSearchStartTime().containsKey(agentId)){
			parkingAgentsTracker.getSearchStartTime().put(agentId, Double.NEGATIVE_INFINITY);
		}
	}
	


	/*
	 * If no parking is selected for the current agent, the agent requires
	 * a replanning.
	 */
	private boolean requiresReplanning(MobsimAgent agent) {
		return parkingAgentsTracker.getSelectedParking(agent.getId()) == null;
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		this.agents.clear();
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), agent);
		}
	}


}
