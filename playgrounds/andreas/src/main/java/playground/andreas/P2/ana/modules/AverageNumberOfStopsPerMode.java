/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.andreas.P2.ana.modules;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;


/**
 * Calculates the average trip distance per ptModes specified. A trip starts by entering a vehicle and end by leaving one.
 * 
 * @author aneumann
 *
 */
public class AverageNumberOfStopsPerMode extends AbstractPAnalyisModule implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler{
	
	private final static Logger log = Logger.getLogger(AverageNumberOfStopsPerMode.class);
	
	private HashMap<Id, String> vehId2ptModeMap;
	private HashMap<String, Integer> ptMode2NumberOfStopsTravelledMap;
	private HashMap<String, Integer> ptMode2TripCountMap;
	private HashMap<Id,HashMap<Id,Integer>> vehId2AgentId2StopCountMap = new HashMap<Id, HashMap<Id, Integer>>();

	
	public AverageNumberOfStopsPerMode(String ptDriverPrefix){
		super("AverageNumberOfStopsPerMode",ptDriverPrefix);
		log.info("enabled");
	}

	@Override
	public String getResult() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModes) {
			strB.append(", " + (this.ptMode2NumberOfStopsTravelledMap.get(ptMode).doubleValue() / this.ptMode2TripCountMap.get(ptMode).doubleValue()));
		}
		return strB.toString();
	}
	
	@Override
	public void reset(int iteration) {
		this.vehId2ptModeMap = new HashMap<Id, String>();
		this.ptMode2NumberOfStopsTravelledMap = new HashMap<String, Integer>();
		this.ptMode2TripCountMap = new HashMap<String, Integer>();
		this.vehId2AgentId2StopCountMap = new HashMap<Id, HashMap<Id, Integer>>();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		String ptMode = this.lineIds2ptModeMap.get(event.getTransitLineId());
		if (ptMode == null) {
			log.warn("Should not happen");
			ptMode = "no valid pt mode found";
		}
		this.vehId2ptModeMap.put(event.getVehicleId(), ptMode);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		String ptMode = this.vehId2ptModeMap.get(event.getVehicleId());
		if (ptMode2NumberOfStopsTravelledMap.get(ptMode) == null) {
			ptMode2NumberOfStopsTravelledMap.put(ptMode, new Integer(0));
		}
		if (ptMode2TripCountMap.get(ptMode) == null) {
			ptMode2TripCountMap.put(ptMode, new Integer(0));
		}
		
		if (this.vehId2AgentId2StopCountMap.get(event.getVehicleId()) == null) {
			this.vehId2AgentId2StopCountMap.put(event.getVehicleId(), new HashMap<Id, Integer>());
		}
		
		if(!event.getPersonId().toString().startsWith(ptDriverPrefix)){
			this.vehId2AgentId2StopCountMap.get(event.getVehicleId()).put(event.getPersonId(), new Integer(0));
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(!event.getPersonId().toString().startsWith(ptDriverPrefix)){
			String ptMode = this.vehId2ptModeMap.get(event.getVehicleId());
			this.ptMode2NumberOfStopsTravelledMap.put(ptMode, new Integer(this.ptMode2NumberOfStopsTravelledMap.get(ptMode) + this.vehId2AgentId2StopCountMap.get(event.getVehicleId()).get(event.getPersonId()).intValue()));
			this.ptMode2TripCountMap.put(ptMode, new Integer(this.ptMode2TripCountMap.get(ptMode) + 1));
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		for (Id agentId : this.vehId2AgentId2StopCountMap.get(event.getVehicleId()).keySet()) {
			int oldValue = this.vehId2AgentId2StopCountMap.get(event.getVehicleId()).get(agentId).intValue();
			this.vehId2AgentId2StopCountMap.get(event.getVehicleId()).put(agentId, new Integer(oldValue + 1));
		}
	}

}
