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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;

public class ParkingSearchIdentifier extends DuringLegIdentifier implements MobsimInitializedListener {
	
	private final ParkingAgentsTracker parkingAgentsTracker;
	private final ParkingInfrastructure parkingInfrastructure;
	private final Map<Id, PlanBasedWithinDayAgent> agents;
	
	public ParkingSearchIdentifier(ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructure parkingInfrastructure) {
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.parkingInfrastructure = parkingInfrastructure;
		
		this.agents = new HashMap<Id, PlanBasedWithinDayAgent>();
	}
	
	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {

		/*
		 * Get all agents that are searching and have entered a new link in the last
		 * time step.
		 */
		Set<Id> linkEnteredAgents = this.parkingAgentsTracker.getLinkEnteredAgents();		
		Set<PlanBasedWithinDayAgent> identifiedAgents = new HashSet<PlanBasedWithinDayAgent>();
		
		for (Id agentId : linkEnteredAgents) {
			PlanBasedWithinDayAgent agent = this.agents.get(agentId);
			
			/*
			 * If the agent has not selected a parking facility yet.
			 */
			if (requiresReplanning(agent)) {
				
				Id linkId = agent.getCurrentLinkId();
				boolean foundParking = false;
				
				List<Id> facilityIds = parkingInfrastructure.getFreeParkingFacilitiesOnLink(linkId, "streetParking");
				if (facilityIds != null && facilityIds.size() > 0) {
					Id facilityId = facilityIds.get(0);

					/*
					 * If the agent accepts the parking, select it.
					 * The parkingAgentsTracker then reserves the parking lot.
					 */
					if (acceptParking(agent, facilityId)) { 
						this.parkingAgentsTracker.setSelectedParking(agentId, facilityId, false);
						foundParking = true;
					}
				}
				
				// else: check whether the agent is also willing to wait for a free parking
				
				if (!foundParking) {
					facilityIds = parkingInfrastructure.getFreeWaitingFacilitiesOnLink(linkId, "streetParking");
					if (facilityIds != null && facilityIds.size() > 0) {
						Id facilityId = facilityIds.get(0);

						/*
						 * If the agent accepts the parking, select it.
						 * The parkingAgentsTracker then reserves the parking lot.
						 */
						if (acceptParking(agent, facilityId) && isWillingToWaitForParking(agent, facilityId)) { 
							this.parkingAgentsTracker.setSelectedParking(agentId, facilityId, true);
							foundParking = true;
						}
					}
				}

				
				identifiedAgents.add(agent);
			}
		}
		
		return identifiedAgents;
	}
	
	private boolean acceptParking(PlanBasedWithinDayAgent agent, Id facilityId) {
		// TODO: allow the agent to refuse the parking
		return true;
	}
	
	private boolean isWillingToWaitForParking(PlanBasedWithinDayAgent agent, Id facilityId) {
		// TODO: add a behavioural model, that e.g. takes the facilities capacity into account
		return false;
	}
	
	/*
	 * If no parking is selected for the current agent, the agent requires
	 * a replanning.
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