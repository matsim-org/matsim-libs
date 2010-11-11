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

package playground.andreas.bvgAna.level2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

import playground.andreas.bvgAna.level1.VehId2PersonEnterLeaveVehicleMap;

/**
 * Retrieves the agent ids of a given vehicle id at a given time
 * 
 * @author aneumann
 *
 */
public class VehId2AgentIds implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private final Logger log = Logger.getLogger(VehId2AgentIds.class);
	private final Level logLevel = Level.DEBUG;
	
	private VehId2PersonEnterLeaveVehicleMap vehId2PersonEnterLeaveVehicleMap;
	
	public VehId2AgentIds(){
		this.log.setLevel(this.logLevel);
		this.vehId2PersonEnterLeaveVehicleMap = new VehId2PersonEnterLeaveVehicleMap();
	}
	
	/**
	 * @return A set containing all agent ids traveling in a given vehicle at a given time
	 */
	public Set<Id> getAgentIdsInVehicle(Id vehId, double time){
		
		Set<Id> agentIdsInVehicle = new TreeSet<Id>();
		
		ArrayList<PersonEntersVehicleEvent> vem = this.vehId2PersonEnterLeaveVehicleMap.getVehId2PersonEnterEventMap().get(vehId);
		ArrayList<PersonLeavesVehicleEvent> vlm = this.vehId2PersonEnterLeaveVehicleMap.getVehId2PersonLeaveEventMap().get(vehId);		
		
		for (Iterator vemIterator = vem.iterator(); vemIterator.hasNext();) {
			PersonEntersVehicleEvent personEntersVehicleEvent = (PersonEntersVehicleEvent) vemIterator.next();
			
			for (Iterator vlmIterator = vlm.iterator(); vlmIterator.hasNext();) {
				PersonLeavesVehicleEvent personLeavesVehicleEvent = (PersonLeavesVehicleEvent) vlmIterator.next();
				
				while(personEntersVehicleEvent.getTime() < personLeavesVehicleEvent.getTime()){
					if(personEntersVehicleEvent.getTime() <= time){
						agentIdsInVehicle.add(personEntersVehicleEvent.getPersonId());
					}
					if(vemIterator.hasNext()){
						personEntersVehicleEvent = (PersonEntersVehicleEvent) vemIterator.next();
					} else {
						break;
					}
				}
				
				if(personLeavesVehicleEvent.getTime() <= personEntersVehicleEvent.getTime()){
					if(personLeavesVehicleEvent.getTime() <= time){
						agentIdsInVehicle.remove(personLeavesVehicleEvent.getPersonId());
					}
				}				
			}			
		}
		
		return agentIdsInVehicle;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.vehId2PersonEnterLeaveVehicleMap.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		this.vehId2PersonEnterLeaveVehicleMap.handleEvent(event);
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}
}
