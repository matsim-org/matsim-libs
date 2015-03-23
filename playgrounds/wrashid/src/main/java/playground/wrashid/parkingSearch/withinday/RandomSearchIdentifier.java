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

package playground.wrashid.parkingSearch.withinday;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;

import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;

public class RandomSearchIdentifier extends DuringLegAgentSelector implements MobsimInitializedListener {
	
	private final ParkingAgentsTracker parkingAgentsTracker;
	private final ParkingInfrastructure parkingInfrastructure;
	private final Map<Id, MobsimAgent> agents;
	
	public RandomSearchIdentifier(ParkingAgentsTracker parkingAgentsTracker, ParkingInfrastructure parkingInfrastructure) {
		this.parkingAgentsTracker = parkingAgentsTracker;
		this.parkingInfrastructure = parkingInfrastructure;
		
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
		Set<Id> linkEnteredAgents = this.parkingAgentsTracker.getLinkEnteredAgents();		
		Set<MobsimAgent> identifiedAgents = new HashSet<MobsimAgent>();
		
		for (Id agentId : linkEnteredAgents) {
			MobsimAgent agent = this.agents.get(agentId);
			
			/*
			 * If the agent has not selected a parking facility yet.
			 */
			if (requiresReplanning(agent)) {
				Id linkId = agent.getCurrentLinkId();
				Id facilityId = parkingInfrastructure.getFreeParkingFacilityOnLink(linkId,null);
				if (facilityId != null) {
					parkingInfrastructure.parkVehicle(facilityId);
					parkingAgentsTracker.setSelectedParking(agentId, facilityId);
				}
				
				identifiedAgents.add(agent);
			}
		}
		
		return identifiedAgents;
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
