/* *********************************************************************** *
 * project: org.matsim.*
 * SearchParkingAgentsIdentifier.java
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

public class SearchParkingAgentsIdentifier extends DuringLegIdentifier implements SimulationInitializedListener {
	
	/*
	 * TODO:
	 * Add a datastructure that logs when an agent has been replanned
	 */
	private final ParkingAgentsTracker parkingAgentsTracker;
	private final Map<Id, PlanBasedWithinDayAgent> agents;
	
	public SearchParkingAgentsIdentifier(ParkingAgentsTracker parkingAgentsTracker) {
		this.parkingAgentsTracker = parkingAgentsTracker;
		
		this.agents = new HashMap<Id, PlanBasedWithinDayAgent>();
	}
	
	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {

		Set<Id> searchingAgents = this.parkingAgentsTracker.getSearchingAgents();		
		Set<PlanBasedWithinDayAgent> identifiedAgents = new TreeSet<PlanBasedWithinDayAgent>();
		
		for (Id agentId : searchingAgents) {
			PlanBasedWithinDayAgent agent = this.agents.get(agentId);
			if (requiresReplanning(agent)) identifiedAgents.add(agent);
		}
		
		return identifiedAgents;
	}
	
	private boolean requiresReplanning(PlanBasedWithinDayAgent agent) {
		return false;
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		this.agents.clear();
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (ExperimentalBasicWithindayAgent) agent);
		}
	}


}
