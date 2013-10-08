/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.andreas.virginia;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;

/**
 * 
 * @author aneumann
 *
 */
public class Events2CSVHandler implements AgentWaitingForPtEventHandler, VehicleDepartsAtFacilityEventHandler{
	
	private static final Logger log = Logger.getLogger(Events2CSVHandler.class);
	private LinkedHashMap<Id, Double> agentId2TimeMap;
	private LinkedHashMap<Id, Double> vehicle2TimeMap;

	
	public Events2CSVHandler() {
		this.agentId2TimeMap = new LinkedHashMap<Id, Double>();
		this.vehicle2TimeMap = new LinkedHashMap<Id, Double>();
	}
	
	@Override
	public void reset(int iteration) {
		// Nothing to do here - only one iteration
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		if (this.agentId2TimeMap.get(event.getPersonId()) == null) {
			this.agentId2TimeMap.put(event.getPersonId(), new Double(event.getTime()));
		} else {
			log.warn("Second AgentWaitingForPtEvent for agent " + event.getPersonId() + " found. For the scenario this analysis has been written, this should not be possible. Please check.");
		}
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		if (this.vehicle2TimeMap.get(event.getVehicleId()) == null) {
			this.vehicle2TimeMap.put(event.getVehicleId(), new Double(event.getTime()));
		} else {
			// additional departure will not bee tracked
		}
	}

	public HashMap<Id, Double> getAgentId2TimeMap() {
		return this.agentId2TimeMap;
	}

	public HashMap<Id, Double> getVehicle2TimeMap() {
		return this.vehicle2TimeMap;
	}

}
