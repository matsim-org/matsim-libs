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
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

/**
 * Collects <code>PersonEntersVehicleEvent</code> and <code>PersonLeavesVehicleEventHandler</code> for a each vehicle id.
 * 
 * @author aneumann
 *
 */
public class VehId2PersonEnterLeaveVehicleMap implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private final Logger log = Logger.getLogger(VehId2PersonEnterLeaveVehicleMap.class);
	private final Level logLevel = Level.DEBUG;
	
	private TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> vehId2PersonEnterEventMap = new TreeMap<Id, ArrayList<PersonEntersVehicleEvent>>();
	private TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> vehId2PersonLeaveEventMap = new TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>>();
	
	public VehId2PersonEnterLeaveVehicleMap(){
		this.log.setLevel(this.logLevel);
	}
	
	/**
	 * @return A map containing all <code>PersonEntersVehicleEvent</code> sorted by veh id
	 */
	public TreeMap<Id, ArrayList<PersonEntersVehicleEvent>> getVehId2PersonEnterEventMap() {
		return this.vehId2PersonEnterEventMap;
	}

	/**
	 * @return A map containing all <code>PersonLeavesVehicleEvent</code> sorted by veh id
	 */
	public TreeMap<Id, ArrayList<PersonLeavesVehicleEvent>> getVehId2PersonLeaveEventMap() {
		return this.vehId2PersonLeaveEventMap;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(this.vehId2PersonEnterEventMap.get(event.getVehicleId()) == null){
			this.vehId2PersonEnterEventMap.put(event.getVehicleId(), new ArrayList<PersonEntersVehicleEvent>());
		}
		this.vehId2PersonEnterEventMap.get(event.getVehicleId()).add(event);
		this.log.debug("Added event to veh " + event.getVehicleId() + " event " + event);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(this.vehId2PersonLeaveEventMap.get(event.getVehicleId()) == null){
			this.vehId2PersonLeaveEventMap.put(event.getVehicleId(), new ArrayList<PersonLeavesVehicleEvent>());
		}
		this.vehId2PersonLeaveEventMap.get(event.getVehicleId()).add(event);
		this.log.debug("Added event to veh " + event.getVehicleId() + " event " + event);
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}

}
