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

package playground.vsp.analysis.modules.bvgAna.anaLevel1.agentId2PersonEnterLeaveVehicle;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;

import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;

/**
 * Collects all <code>PersonEntersVehicleEvent</code> and <code>PersonLeavesVehicleEvent</code> for a given set of agents.
 * 
 * @author ikaddoura, aneumann
 *
 */
public class AgentId2PersonEnterLeaveVehicleHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	private PtDriverIdAnalyzer ptDriverIdAnalyzer;
	private final Logger log = Logger.getLogger(AgentId2PersonEnterLeaveVehicleHandler.class);
	
	private TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> agentId2EnterEventMap = new TreeMap<Id, ArrayList<PersonEntersVehicleEvent>>();
	private TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> agentId2LeaveEventMap = new TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>>();

	public AgentId2PersonEnterLeaveVehicleHandler(PtDriverIdAnalyzer ptDriverPrefixAnalyzer) {
		this.ptDriverIdAnalyzer = ptDriverPrefixAnalyzer;
		log.warn("Ignoring the pt driver. Is that right or is the pt driver supposed to be considered? ik");
		log.warn("Not differentiating between public and private vehicles. Is that supposed to happen here? ik");
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
		if(this.ptDriverIdAnalyzer.isPtDriver(agentId)){
			// pt driver
		} else {
			if(this.agentId2EnterEventMap.get(agentId) == null){
				this.agentId2EnterEventMap.put(agentId, new ArrayList<PersonEntersVehicleEvent>());
			}
			this.agentId2EnterEventMap.get(agentId).add(event);
		}		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id agentId = event.getPersonId();
		if(this.ptDriverIdAnalyzer.isPtDriver(agentId)){
			// pt driver
		} else {
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
