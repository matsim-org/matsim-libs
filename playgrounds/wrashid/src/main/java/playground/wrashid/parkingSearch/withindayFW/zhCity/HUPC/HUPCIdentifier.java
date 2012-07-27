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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.utils.EditRoutes;

import playground.wrashid.artemis.smartCharging.ChargingTime;
import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.SortableMapObject;
import playground.wrashid.parkingSearch.withindayFW.controllers.kti.HUPCControllerKTIzh;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingAgentsTracker;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingStrategy;
import playground.wrashid.parkingSearch.withindayFW.util.ActivityDurationEstimator;
import playground.wrashid.parkingSearch.withindayFW.zhCity.ParkingInfrastructureZH;

public class HUPCIdentifier extends DuringLegIdentifier implements MobsimInitializedListener {

	private final ParkingAgentsTracker parkingAgentsTracker;
	private final ParkingInfrastructureZH parkingInfrastructure;
	private final Map<Id, PlanBasedWithinDayAgent> agents;
	private double searchTimeEstimationConstant;
	private double initialParkingSetRadiusInMeters;

	public HUPCIdentifier(ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructureZH parkingInfrastructure, Controler controler) {
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.parkingInfrastructure = parkingInfrastructure;
		
		String searchTimeEstimationConstantString = controler.getConfig().findParam("parking", "HUPCIdentifier.searchTimeEstimationConstant");
		searchTimeEstimationConstant=Double.parseDouble(searchTimeEstimationConstantString);
		
		String initialParkingSetRadiusInMetersHUPCString = controler.getConfig().findParam("parking", "HUPCIdentifier.initialParkingSetRadiusInMeters");
		initialParkingSetRadiusInMeters=Double.parseDouble(initialParkingSetRadiusInMetersHUPCString);
		

		this.agents = new HashMap<Id, PlanBasedWithinDayAgent>();
	}

	/*
	 * Put stuff here, which cannot run in parallel, the rest you can put in
	 * replanner.
	 */
	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		/*
		 * Get all agents that are searching and have entered a new link in the
		 * last time step.
		 */
		// Set<Id> linkEnteredAgents =
		// this.parkingAgentsTracker.getLinkEnteredAgents();
		Set<PlanBasedWithinDayAgent> identifiedAgents = new HashSet<PlanBasedWithinDayAgent>();

		Set<Id> searchingAgentsAssignedToThisIdentifier = this.parkingAgentsTracker.getActiveReplanningIdentifiers().getValueSet(
				this);

		if (this.getIdentifierFactory() == null && searchingAgentsAssignedToThisIdentifier == null) {
			return identifiedAgents;
		} else {
			// System.out.println();
		}

		for (Id agentId : searchingAgentsAssignedToThisIdentifier) {
			PlanBasedWithinDayAgent agent = this.agents.get(agentId);

			Id personId = agent.getId();
			Plan selectedPlan = agent.getSelectedPlan();
			List<PlanElement> planElements = selectedPlan.getPlanElements();
			Integer currentPlanElementIndex = agent.getCurrentPlanElementIndex();

			if (agents.get(personId).getCurrentPlanElementIndex() == 33) {

				

				// DebugLib.traceAgent(personId);
			}

			DebugLib.traceAgent(personId, 22);
			
			/*
			 * If the agent has not selected a parking facility yet.
			 */
			if (requiresReplanning(agent)) {
				
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
					Collection<ActivityFacility> parkings = parkingInfrastructure.getAllFreeParkingWithinDistance(initialParkingSetRadiusInMeters,
							nextNonParkingAct.getCoord());
					if (parkings.size() == 0) {
						parkings.add(parkingInfrastructure.getClosestFreeParkingFacility(nextNonParkingAct.getCoord()));
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

						double parkingDuration;
						if (isLastParkingOfDay(personId)) {
							parkingDuration = ActivityDurationEstimator.estimateActivityDurationLastParkingOfDay(time,
									parkingAgentsTracker.getFirstCarDepartureTimeOfDay().getTime(agentId));
						} else {
							parkingDuration = ActivityDurationEstimator.estimateActivityDurationParkingDuringDay(time,
									planElements, currentPlanElementIndex);
						}

						double activityDuration = ActivityDurationEstimator.estimateActivityDurationParkingDuringDay(time,
								planElements, currentPlanElementIndex);
						double walkScore = parkingAgentsTracker.getWalkScore(personId, activityDuration,
								GeneralLib.getWalkingTravelDuration(walkingDistance)/60);
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
					double bestParkingScore=poll.getScore();

					if (bestParkingScore<-1000){
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
		PlanBasedWithinDayAgent agent = this.agents.get(personId);

		List<PlanElement> planElements = agent.getSelectedPlan().getPlanElements();
		Integer currentPlanElementIndex = agent.getCurrentPlanElementIndex();

		for (int i = planElements.size() - 1; i > 0; i--) {
			if (planElements.get(i) instanceof Leg) {
				Leg leg = (Leg) planElements.get(i);

				if (leg.getMode().equals(TransportMode.car) && i == currentPlanElementIndex) {
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
		//if (!parkingAgentsTracker.getSearchStartTime().containsKey(agentId)) {
			parkingAgentsTracker.getSearchStartTime().put(agentId, Double.NEGATIVE_INFINITY);
		//}
	}

	/*
	 * If no parking is selected for the current agent, the agent requires a
	 * replanning.
	 */
	private boolean requiresReplanning(PlanBasedWithinDayAgent agent) {
		return parkingAgentsTracker.getSelectedParking(agent.getId()) == null;
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		this.agents.clear();
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (ExperimentalBasicWithindayAgent) agent);
		}
	}

}
