/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingSearchIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.parking.withinday.identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;

import playground.christoph.parking.ParkingTypes;
import playground.christoph.parking.core.interfaces.ParkingCostCalculator;
import playground.christoph.parking.core.mobsim.InsertParkingActivities;
import playground.christoph.parking.core.mobsim.ParkingFacility;
import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;

/**
 * Some methods in here could be put behind interfaces, e.g. getAgentsParkingTypes(...),
 * acceptParking(...) and isWillingToWaitForParking(...).
 * 
 * @author cdobler
 */
public class ParkingSearchIdentifier extends DuringLegAgentSelector {
	
	private final ParkingAgentsTracker parkingAgentsTracker;
	private final ParkingInfrastructure parkingInfrastructure;
	private final ParkingCostCalculator parkingCostCalculator;
	private final MobsimDataProvider mobsimDataProvider;
	
	// TODO: make this agent dependent!
	private final List<String> freeParking;
	private final List<String> restrictedParking;
	
	public ParkingSearchIdentifier(ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructure parkingInfrastructure,
			ParkingCostCalculator parkingCostCalculator, MobsimDataProvider mobsimDataProvider) {
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.parkingInfrastructure = parkingInfrastructure;
		this.parkingCostCalculator = parkingCostCalculator;
		this.mobsimDataProvider = mobsimDataProvider;

		this.freeParking = new ArrayList<String>();
		this.freeParking.add(ParkingTypes.PRIVATEINSIDEPARKING);
		this.freeParking.add(ParkingTypes.PRIVATEOUTSIDEPARKING);
		this.freeParking.add(ParkingTypes.STREETPARKING);
		this.freeParking.add(ParkingTypes.GARAGEPARKING);
		
		this.restrictedParking = new ArrayList<String>();
		this.restrictedParking.add(ParkingTypes.STREETPARKING);
		this.restrictedParking.add(ParkingTypes.GARAGEPARKING);
	}
	
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {

		/*
		 * Get all agents that are searching and have entered a new link in the last
		 * time step.
		 */
		Set<Id> linkEnteredAgents = this.parkingAgentsTracker.getLinkEnteredAgents();		
		Set<MobsimAgent> identifiedAgents = new HashSet<MobsimAgent>();
		
		for (Id agentId : linkEnteredAgents) {
			
			/*
			 * If the agent has not selected a parking facility yet.
			 */
			if (requiresReplanning(agentId)) {
				MobsimAgent agent = this.mobsimDataProvider.getAgent(agentId);
				parkOnLink(agent, time);				
				
				identifiedAgents.add(agent);
			}
		}
		
		return identifiedAgents;
	}
	
	/**
	 * @param agent
	 * @return true if a parking facility is attached to the agent's current link where
	 * 	the agent is going to park or false if the agent has to continue its search.
	 * 
	 * At the moment, this cannot be moved to the ParkingSearchReplanner since the order
	 * in which parking lots are selected and reserved would not be deterministic anymore!
	 */
	private boolean parkOnLink(MobsimAgent agent, double now) {
		
		Id agentId = agent.getId();
		Id vehicleId = this.mobsimDataProvider.getDriversVehicle(agentId).getId();
		Id linkId = agent.getCurrentLinkId();
		
		Id minCostParkingFacilityId = null;
		boolean reserveAsWaiting = false;
		double minParkingCosts = Double.MAX_VALUE;
		double parkingDuration = getAgentsParkingDuration(agent, now);
		
		// check for all parking types that the agent is willing to use
		// TODO: calculate costs and compare...
		for (String parkingType : getAgentsParkingTypes(agent)) {
			List<Id<ParkingFacility>> facilityIds = parkingInfrastructure.getFreeParkingFacilitiesOnLink(linkId, parkingType);
			
			if (facilityIds != null) {
				// we have to check all facilities since they might have different costs
				for (Id facilityId : facilityIds) {			
					/*
					 * If the agent accepts the parking, select it.
					 * The parkingAgentsTracker then reserves the parking lot.
					 */
					if (acceptParking(agent, facilityId)) {
						double parkingCosts = this.parkingCostCalculator.getParkingCost(facilityId, vehicleId, agentId, now, parkingDuration);
						if (parkingCosts < minParkingCosts) {
							minCostParkingFacilityId = facilityId;
							minParkingCosts = parkingCosts;
							reserveAsWaiting = false;
						}
					}					
				}	
			}
			
			// else: check whether the agent is also willing to wait for a free parking
			else {
				facilityIds = parkingInfrastructure.getFreeWaitingFacilitiesOnLink(linkId, parkingType);
				if (facilityIds != null) {
					// we have to check all facilities since they might have different costs
					for (Id facilityId : facilityIds) {
						/*
						 * If the agent accepts the parking, select it.
						 * The parkingAgentsTracker then reserves the parking lot.
						 */
						if (acceptParking(agent, facilityId) && isWillingToWaitForParking(agent, facilityId)) {
							double parkingCosts = this.parkingCostCalculator.getParkingCost(facilityId, vehicleId, agentId, now, parkingDuration);
							if (parkingCosts < minParkingCosts) {
								minCostParkingFacilityId = facilityId;
								minParkingCosts = parkingCosts;
								reserveAsWaiting = true;
							}
						}						
					}		
				}
			}			
		}
		
		// if the agent accepted a parking facility
		if (minCostParkingFacilityId != null) {
			this.parkingAgentsTracker.setSelectedParking(agentId, minCostParkingFacilityId, reserveAsWaiting);
			return true;
		}
		
		// no parking was found on this link, therefore return null
		return false;
	}
	
	private List<String> getAgentsParkingTypes(MobsimAgent agent) {
		
		String nextActivityType = getAgentsNextNonParkingActivityType(agent);
		
		if (nextActivityType.startsWith("home") || nextActivityType.startsWith("work")) {
			return this.freeParking;
		} else {
			return this.restrictedParking;
		}
	}
	
	private boolean acceptParking(MobsimAgent agent, Id facilityId) {
		// TODO: allow the agent to refuse the parking, e.g. if it is to expensive
		return true;
	}
	
	private boolean isWillingToWaitForParking(MobsimAgent agent, Id facilityId) {
		// TODO: add a behavioural model, that e.g. takes the facilities capacity into account
		return true;
	}
	
	private String getAgentsNextNonParkingActivityType(MobsimAgent agent) {
		
		int currentPlanElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		List<PlanElement> planElements = WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements();
		for (int i = currentPlanElementIndex; i < planElements.size(); i++) {
			PlanElement planElement = planElements.get(i);
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				if (!InsertParkingActivities.PARKINGACTIVITY.equals(activity.getType())) return activity.getType();
			}
		}
		
		return null;
	}
	
	private double getAgentsParkingDuration(MobsimAgent agent, double now) {
		
		int currentPlanElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		List<PlanElement> planElements = WithinDayAgentUtils.getModifiablePlan(agent).getPlanElements();
		for (int i = currentPlanElementIndex + 1; i < planElements.size(); i++) {
			PlanElement planElement = planElements.get(i);
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (TransportMode.car.equals(leg.getMode())) {
					double parkingDuration = leg.getDepartureTime() - now;
					if (parkingDuration > 0.0) return parkingDuration;
					else return 0.0;
				}
			}
		}
		
		// agent does not use the car anymore
		return Double.MAX_VALUE;
	}
	
	/*
	 * If no parking is selected for the current agent, the agent requires
	 * a replanning.
	 */
	private boolean requiresReplanning(Id agentId) {
		return parkingAgentsTracker.getSelectedParking(agentId) == null;
	}

}