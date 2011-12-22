/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsToPickupIdentifier.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

/**
 * Identifies agents that perform a walk leg in the insecure area. They might be
 * picked up by a vehicle coming by.
 * 
 * Collect vehicles that have just entered a link in the previous time step. If
 * they have free capacity, they might pick up additional agents.
 * 
 * @author cdobler
 */
public class AgentsToPickupIdentifier extends DuringLegIdentifier implements LinkEnterEventHandler, 
		SimulationInitializedListener, SimulationAfterSimStepListener {
	
	private final InsecureLegPerformingIdentifier insecureLegPerformingIdentifier;

	private Map<Id, List<Id>> lastTimeStepLinkEnteredVehicles;
	private Map<Id, List<Id>> recentLinkEnteredVehicles;
	private Map<Id, QVehicle> vehicles; 
	
	private final Map<Id, Id> agentsToPickup;	// <AgentId, VehicleId>
	
	/*package*/ AgentsToPickupIdentifier(Scenario scenario, InsecureLegPerformingIdentifier insecureLegPerformingIdentifier) {
		this.insecureLegPerformingIdentifier = insecureLegPerformingIdentifier;
		
		this.agentsToPickup = new TreeMap<Id, Id>();
	}
	
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {	
		Set<PlanBasedWithinDayAgent> insecureLegPerformingAgents = insecureLegPerformingIdentifier.getAgentsToReplan(time);
		Set<PlanBasedWithinDayAgent> agentsToReplan = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		for (MobsimAgent personAgent : insecureLegPerformingAgents) {
			/*
			 * The agent wants to be picked up if its leg mode is walk.
			 */
			if (personAgent.getMode().equals(TransportMode.walk)) {
				
				/*
				 * Check whether there are vehicle available on the link
				 */
				List<Id> vehicleIds = lastTimeStepLinkEnteredVehicles.get(personAgent.getCurrentLinkId());
				
				if (vehicleIds == null) continue;

				/*
				 * Check whether one of the vehicles has free capacity
				 */
				for (Id vehicleId : vehicleIds) {
					
					agentsToReplan.add((PlanBasedWithinDayAgent)personAgent);
					break;
				}
				
			}
		}
		return agentsToReplan;
	}

	@Override
	public void reset(int iteration) {
		this.agentsToPickup.clear();
		lastTimeStepLinkEnteredVehicles = new HashMap<Id, List<Id>>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		List<Id> list = recentLinkEnteredVehicles.get(event.getLinkId());
		if (list == null) {
			list = new ArrayList<Id>();
			recentLinkEnteredVehicles.put(event.getLinkId(), list);
		}
		list.add(event.getVehicleId());
	}

	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
		lastTimeStepLinkEnteredVehicles = recentLinkEnteredVehicles;
		recentLinkEnteredVehicles = new HashMap<Id, List<Id>>();
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		
	}
	
}
