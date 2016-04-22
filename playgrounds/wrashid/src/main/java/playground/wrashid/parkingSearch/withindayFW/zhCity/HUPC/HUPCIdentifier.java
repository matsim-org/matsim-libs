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

package playground.wrashid.parkingSearch.withindayFW.zhCity.HUPC;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;

import playground.wrashid.parkingSearch.withindayFW.core.ParkingAgentsTracker;
import playground.wrashid.parkingSearch.withindayFW.util.ActivityDurationEstimator;
import playground.wrashid.parkingSearch.withindayFW.zhCity.ParkingInfrastructureZH;

public class HUPCIdentifier extends DuringLegAgentSelector implements MobsimInitializedListener {

	private final ParkingAgentsTracker parkingAgentsTracker;
	private final ParkingInfrastructureZH parkingInfrastructure;
	private final Map<Id, MobsimAgent> agents;
	private double searchTimeEstimationConstant;
	private double initialParkingSetRadiusInMeters;
	private final WithinDayAgentUtils withinDayAgentUtils;
	private Scenario scenario;

	public HUPCIdentifier(ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructureZH parkingInfrastructure, MatsimServices controler) {
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.parkingInfrastructure = parkingInfrastructure;
		
		String searchTimeEstimationConstantString = controler.getConfig().findParam("parking", "HUPCIdentifier.searchTimeEstimationConstant");
		searchTimeEstimationConstant=Double.parseDouble(searchTimeEstimationConstantString);
		
		String initialParkingSetRadiusInMetersHUPCString = controler.getConfig().findParam("parking", "HUPCIdentifier.initialParkingSetRadiusInMeters");
		initialParkingSetRadiusInMeters=Double.parseDouble(initialParkingSetRadiusInMetersHUPCString);
		
		this.withinDayAgentUtils = new WithinDayAgentUtils();
		this.agents = new HashMap<Id, MobsimAgent>();
		
		this.scenario = controler.getScenario() ;
	}

	/*
	 * Put stuff here, which cannot run in parallel, the rest you can put in
	 * replanner.
	 */
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {
		/*
		 * Get all agents that are searching and have entered a new link in the
		 * last time step.
		 */
		// Set<Id> linkEnteredAgents =
		// this.parkingAgentsTracker.getLinkEnteredAgents();
		Set<MobsimAgent> identifiedAgents = new HashSet<MobsimAgent>();

		Set<Id> searchingAgentsAssignedToThisIdentifier = this.parkingAgentsTracker.getActiveReplanningIdentifiers().getValueSet(
				this);

		if (this.getAgentSelectorFactory() == null && searchingAgentsAssignedToThisIdentifier == null) {
			return identifiedAgents;
		} else {
			// System.out.println();
		}
		
		

		for (Id agentId : searchingAgentsAssignedToThisIdentifier) {
			MobsimAgent agent = this.agents.get(agentId);

			Id personId = agent.getId();
			Plan selectedPlan = this.withinDayAgentUtils.getModifiablePlan(agent);
			List<PlanElement> planElements = selectedPlan.getPlanElements();
			Integer currentPlanElementIndex = this.withinDayAgentUtils.getCurrentPlanElementIndex(agent);

			if (currentPlanElementIndex == 33) {

				

				// DebugLib.traceAgent(personId);
			}

			if (time==15395.0){
				DebugLib.traceAgent(personId, 24);
			}
			
			/*
			 * If the agent has not selected a parking facility yet.
			 */
			if (requiresReplanning(agent)) {
				double parkingDuration=0;
				double estimatedParkingSearchTimeInMinutes=0;

				// get all parking within 1000m (of destination) or at least on
				// parking, if that set is empty.
				Activity nextNonParkingAct = (Activity) selectedPlan.getPlanElements().get(currentPlanElementIndex + 3);

				ActivityFacility freePrivateParking = parkingInfrastructure.getFreePrivateParking(
						nextNonParkingAct.getFacilityId(), nextNonParkingAct.getType());

				Id parkingFacilityId = null;
				if (privateParkingAvailable(freePrivateParking)) {
					parkingFacilityId = freePrivateParking.getId();
				} else {
					double radius=initialParkingSetRadiusInMeters;
					Collection<ActivityFacility> parkings = parkingInfrastructure.getAllFreeParkingWithinDistance(initialParkingSetRadiusInMeters,
							nextNonParkingAct.getCoord());
					while (parkings.size() == 0) {
						radius*=2;
						parkings=parkingInfrastructure.getAllFreeParkingWithinDistance(radius,
								nextNonParkingAct.getCoord());
						//parkings.add(parkingInfrastructure.getClosestFreeParkingFacility(nextNonParkingAct.getCoord()));
					}
					
					//estimate parking street search time
					estimatedParkingSearchTimeInMinutes= estimateParkingSearchTime(parkings);
					
					// get best parking
					PriorityQueue<SortableMapObject<ActivityFacility>> priorityQueue = new PriorityQueue<SortableMapObject<ActivityFacility>>();

					for (ActivityFacility parkingFacility : parkings) {
						
						if (parkingFacility.getId().toString().contains("gp") || parkingFacility.getId().toString().contains("stp")){
							DebugLib.emptyFunctionForSettingBreakPoint();
						}
						
						
						double walkingDistance = GeneralLib.getDistance(parkingFacility.getCoord(), nextNonParkingAct.getCoord());

						
						if (isLastParkingOfDay(personId)) {
							parkingDuration = ActivityDurationEstimator.estimateActivityDurationLastParkingOfDay(time,
									parkingAgentsTracker.getFirstCarDepartureTimeOfDay().getTime(agentId));
							
							
						} else {
							parkingDuration = ActivityDurationEstimator.estimateActivityDurationParkingDuringDay(time,
									planElements, currentPlanElementIndex, this.scenario.getConfig().plansCalcRoute() );
						}

						double activityDuration = parkingDuration;
						double walkScore = parkingAgentsTracker.getWalkScore(personId, activityDuration,
								GeneralLib.getWalkingTravelDuration(walkingDistance, this.scenario.getConfig().plansCalcRoute())/60);
						double costScore = parkingAgentsTracker.getParkingCostScore(personId, time, parkingDuration,
								parkingFacility.getId());
						
						double searchTimeScore = 0; 
						
						if (parkingFacility.getId().toString().contains("stp")){
								searchTimeScore+=parkingAgentsTracker.getSearchTimeScore(personId, activityDuration, estimatedParkingSearchTimeInMinutes);;
						}
						
						if (walkScore>0 || costScore>0 || searchTimeScore>0){
							DebugLib.stopSystemAndReportInconsistency();
						}

						priorityQueue.add(new SortableMapObject<ActivityFacility>(parkingFacility, -(walkScore + costScore + searchTimeScore)));
					
					
					
					}

					SortableMapObject<ActivityFacility> poll = priorityQueue.poll();
					ActivityFacility bestParkingFacility = poll.getKey();
					double bestParkingScore=poll.getWeight();

					if (bestParkingScore>1000){
						
						
						double walkingDistance = GeneralLib.getDistance(bestParkingFacility.getCoord(), nextNonParkingAct.getCoord());
						
						double activityDuration = parkingDuration;
						double walkScore = parkingAgentsTracker.getWalkScore(personId, activityDuration,
								GeneralLib.getWalkingTravelDuration(walkingDistance, this.scenario.getConfig().plansCalcRoute())/60);
						double costScore = parkingAgentsTracker.getParkingCostScore(personId, time, parkingDuration,
								bestParkingFacility.getId());
						
						DebugLib.emptyFunctionForSettingBreakPoint();
					}
					
					
					parkingFacilityId = bestParkingFacility.getId();
					
					
				}
				
				if (parkingFacilityId.toString().contains("stp") && estimatedParkingSearchTimeInMinutes!=0.0){
						parkingAgentsTracker.getSearchStartTime().put(agentId, -1*estimatedParkingSearchTimeInMinutes*60);
				} else {
					markFlagForNoSearchTime(agentId);
				}
				
				if (parkingFacilityId != null) {
					parkingInfrastructure.parkVehicle(parkingFacilityId);
					parkingAgentsTracker.setSelectedParking(agentId, parkingFacilityId);
					identifiedAgents.add(agent);
				}

			}
		}

		return identifiedAgents;
	}

	private double estimateParkingSearchTime(Collection<ActivityFacility> parkings) {
		double estimatedParkingSearchTimeInMinutes;
		double sumParkingCapacity=0;
		double sumFreeCapacity=0;
		for (ActivityFacility parkingFacility : parkings) {
			if (parkingFacility.getId().toString().contains("stp")){
				sumParkingCapacity+=parkingInfrastructure.getFacilityCapacities().get(parkingFacility.getId());
				sumFreeCapacity+=parkingInfrastructure.getFreeCapacity(parkingFacility.getId());
			}
		}
		
		if (sumFreeCapacity==0){
			// this means, probably outside of city
			// this value is actually irrelevant (using mean value).
			return 30.0/60.0;
		}
		
		if (sumFreeCapacity==sumParkingCapacity){
			return 0.0;
		}
		
		
		estimatedParkingSearchTimeInMinutes=(searchTimeEstimationConstant/(sumFreeCapacity/sumParkingCapacity)-searchTimeEstimationConstant)/60.0;
		// => for 10% free parking, we have 180 seconds search time
		// => for 100% free parking, we have 0 seconds search time
		return estimatedParkingSearchTimeInMinutes;
	}

	private boolean privateParkingAvailable(ActivityFacility freePrivateParking) {
		return freePrivateParking != null;
	}

	private boolean isLastParkingOfDay(Id personId) {
		DebugLib.traceAgent(personId, 23);
		
		
		MobsimAgent agent = this.agents.get(personId);

		List<PlanElement> planElements = this.withinDayAgentUtils.getModifiablePlan(agent).getPlanElements();
		Integer currentPlanElementIndex = this.withinDayAgentUtils.getCurrentPlanElementIndex(agent);

		for (int i = planElements.size() - 1; i > 0; i--) {
			if (planElements.get(i) instanceof Leg) {
				Leg leg = (Leg) planElements.get(i);

				if (leg.getMode().equals(TransportMode.car)) {
					if (i > currentPlanElementIndex){
						return false;
					} else {
						return true;
					}
				}
			}
		}

		DebugLib.stopSystemAndReportInconsistency();
		
		return false;
	}

	private void markFlagForNoSearchTime(Id agentId) {
		//if (!parkingAgentsTracker.getSearchStartTime().containsKey(agentId)) {
			parkingAgentsTracker.getSearchStartTime().put(agentId, Double.NEGATIVE_INFINITY);
		//}
	}

	/*
	 * If no parking is selected for the current agent, the agent requires a
	 * replanning.
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
