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

package playground.andreas.bvgAna.stopId2VehEnterLeave;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;

/**
 * Collects <code>PersonEntersVehicleEvent</code> and <code>PersonLeavesVehicleEventHandler</code> for a given set of stop ids.
 * 
 * @author aneumann
 *
 */
public class StopId2PersonEnterLeaveHandler implements VehicleArrivesAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{

	private final Logger log = Logger.getLogger(StopId2PersonEnterLeaveHandler.class);
	private final Level logLevel = Level.DEBUG;
	
	private Set<Id> stopIds;
	private TreeMap<Id, Id> vehId2stopIdMap = new TreeMap<Id, Id>();
	private TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> stopId2PersonEnterEventMap = new TreeMap<Id, ArrayList<PersonEntersVehicleEvent>>();
	private TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> stopId2PersonLeaveEventMap = new TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>>();
	
	public StopId2PersonEnterLeaveHandler(Set<Id> stopIds){
		this.log.setLevel(this.logLevel);
		this.stopIds = stopIds;
	}
	
	/**
	 * @return A map containing all <code>PersonEntersVehicleEvent</code> sorted by stop id
	 */
	public TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> getStopId2PersonEnterEventMap() {
		return this.stopId2PersonEnterEventMap;
	}

	/**
	 * @return A map containing all <code>PersonLeavesVehicleEvent</code> sorted by stop id
	 */
	public TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> getStopId2PersonLeaveEventMap() {
		return this.stopId2PersonLeaveEventMap;
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehId2stopIdMap.put(event.getVehicleId(), event.getFacilityId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(this.stopIds.contains(this.vehId2stopIdMap.get(event.getVehicleId()))){
			if(this.stopId2PersonEnterEventMap.get(this.vehId2stopIdMap.get(event.getVehicleId())) == null){
				this.stopId2PersonEnterEventMap.put(this.vehId2stopIdMap.get(event.getVehicleId()), new ArrayList<PersonEntersVehicleEvent>());
			}
			this.stopId2PersonEnterEventMap.get(this.vehId2stopIdMap.get(event.getVehicleId())).add(event);
			this.log.debug("Added event to stop " + this.vehId2stopIdMap.get(event.getVehicleId()) + " event " + event);
		}		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(this.stopIds.contains(this.vehId2stopIdMap.get(event.getVehicleId()))){
			if(this.stopId2PersonLeaveEventMap.get(this.vehId2stopIdMap.get(event.getVehicleId())) == null){
				this.stopId2PersonLeaveEventMap.put(this.vehId2stopIdMap.get(event.getVehicleId()), new ArrayList<PersonLeavesVehicleEvent>());
			}
			this.stopId2PersonLeaveEventMap.get(this.vehId2stopIdMap.get(event.getVehicleId())).add(event);
			this.log.debug("Added event to stop " + this.vehId2stopIdMap.get(event.getVehicleId()) + " event " + event);
		}
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}

}
