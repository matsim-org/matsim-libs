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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

import playground.christoph.parking.ParkingTypes;
import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;

public class ParkingSearchIdentifier extends DuringLegIdentifier {
	
	private final ParkingAgentsTracker parkingAgentsTracker;
	private final ParkingInfrastructure parkingInfrastructure;
	private final MobsimDataProvider mobsimDataProvider;
	
	public ParkingSearchIdentifier(ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructure parkingInfrastructure,
			MobsimDataProvider mobsimDataProvider) {
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.parkingInfrastructure = parkingInfrastructure;
		this.mobsimDataProvider = mobsimDataProvider;
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
				parkOnLink(agent);				
				
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
	private boolean parkOnLink(MobsimAgent agent) {
				
		Id agentId = agent.getId();
		Id linkId = agent.getCurrentLinkId();
	
		List<Id> facilityIds = parkingInfrastructure.getFreeParkingFacilitiesOnLink(linkId, ParkingTypes.PARKING);
		if (facilityIds != null && facilityIds.size() > 0) {
			Id facilityId = facilityIds.get(0);

			/*
			 * If the agent accepts the parking, select it.
			 * The parkingAgentsTracker then reserves the parking lot.
			 */
			if (acceptParking(agent, facilityId)) { 
				this.parkingAgentsTracker.setSelectedParking(agentId, facilityId, false);
				return true;
			}
		}
		
		// else: check whether the agent is also willing to wait for a free parking
		else {
			facilityIds = parkingInfrastructure.getFreeWaitingFacilitiesOnLink(linkId, ParkingTypes.PARKING);
			if (facilityIds != null && facilityIds.size() > 0) {
				Id facilityId = facilityIds.get(0);

				/*
				 * If the agent accepts the parking, select it.
				 * The parkingAgentsTracker then reserves the parking lot.
				 */
				if (acceptParking(agent, facilityId) && isWillingToWaitForParking(agent, facilityId)) { 
					this.parkingAgentsTracker.setSelectedParking(agentId, facilityId, true);
					return true;
				}
			}
		}
		
		// no parking was found on this link, therefore return null
		return false;
	}
	
	private boolean acceptParking(MobsimAgent agent, Id facilityId) {
		// TODO: allow the agent to refuse the parking
		return true;
	}
	
	private boolean isWillingToWaitForParking(MobsimAgent agent, Id facilityId) {
		// TODO: add a behavioural model, that e.g. takes the facilities capacity into account
		return true;
	}
	
	/*
	 * If no parking is selected for the current agent, the agent requires
	 * a replanning.
	 */
	private boolean requiresReplanning(Id agentId) {
		return parkingAgentsTracker.getSelectedParking(agentId) == null;
	}

}