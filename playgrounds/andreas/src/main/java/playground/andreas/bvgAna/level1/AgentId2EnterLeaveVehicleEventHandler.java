/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.level1;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

/**
 * Collects all <code>PersonEntersVehicleEvent</code> and <code>PersonLeavesVehicleEvent</code> for a given set of agents.
 * 
 * @author aneumann
 *
 */
public class AgentId2EnterLeaveVehicleEventHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private final Logger log = Logger.getLogger(AgentId2EnterLeaveVehicleEventHandler.class);
	private final Level logLevel = Level.DEBUG;
	
	private Set<Id> agentIds;
	private TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> agentId2EnterEventMap = new TreeMap<Id, ArrayList<PersonEntersVehicleEvent>>();
	private TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> agentId2LeaveEventMap = new TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>>();
	
	public AgentId2EnterLeaveVehicleEventHandler(Set<Id> agentIds){
		this.log.setLevel(this.logLevel);
		this.agentIds = agentIds;
	}	
	
	/**
	 * @return Returns a map containing all <code>PersonEntersVehicleEvent</code> for each agent id.
	 */
	public TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> getAgentId2EnterEventMap() {
		return this.agentId2EnterEventMap;
	}

	/**
	 * @return Returns a map containing all <code>PersonLeavesVehicleEvent</code> for each agent id.
	 */
	public TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> getAgentId2LeaveEventMap() {
		return this.agentId2LeaveEventMap;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id agentId = event.getPersonId();
		if(this.agentIds.contains(agentId)){
			if(this.agentId2EnterEventMap.get(agentId) == null){
				this.agentId2EnterEventMap.put(agentId, new ArrayList<PersonEntersVehicleEvent>());
			}
			this.agentId2EnterEventMap.get(agentId).add(event);
		}		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id agentId = event.getPersonId();
		if(this.agentIds.contains(agentId)){
			if(this.agentId2LeaveEventMap.get(agentId) == null){
				this.agentId2LeaveEventMap.put(agentId, new ArrayList<PersonLeavesVehicleEvent>());
			}
			this.agentId2LeaveEventMap.get(agentId).add(event);
		}		
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");		
	}

}
