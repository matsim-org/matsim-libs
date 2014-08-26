/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.P2.stats.gexfPStats;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;

/**
 * Counts the number of passenger of paratransit vehicles per link
 * 
 * @author aneumann
 *
 */
public class CountPPaxHandler implements LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private static final Logger log = Logger.getLogger(CountPPaxHandler.class);
	
	private String pIdentifier;
	private HashMap<Id, HashMap<String, Integer>> linkId2LineId2CountsMap;
	private HashMap<Id, Integer> vehId2CountsMap;
	private Set<String> lineIds;

	public CountPPaxHandler(String pIdentifier) {
		this.pIdentifier = pIdentifier;
		this.linkId2LineId2CountsMap = new HashMap<Id, HashMap<String, Integer>>();
		this.vehId2CountsMap =  new HashMap<Id, Integer>();
		this.lineIds = new TreeSet<String>();
	}
	
	public Set<String> getLineIds(){
		return this.lineIds;
	}

	public int getPaxCountForLinkId(Id linkId){
		int count = 0;
		if (this.linkId2LineId2CountsMap.get(linkId) != null) {
			for (Integer countEntryForLine : this.linkId2LineId2CountsMap.get(linkId).values()) {
				count += countEntryForLine.intValue();
			}
		}
		return count;
	}
	
	public int getPaxCountForLinkId(Id linkId, String lineId){
		if (this.linkId2LineId2CountsMap.get(linkId) != null) {
			if (this.linkId2LineId2CountsMap.get(linkId).get(lineId) != null) {
				return this.linkId2LineId2CountsMap.get(linkId).get(lineId).intValue();
			}
		}
		return 0;
	}

	@Override
	public void reset(int iteration) {
		this.linkId2LineId2CountsMap = new HashMap<Id, HashMap<String, Integer>>();
		for (Integer count : this.vehId2CountsMap.values()) {
			if(count != 0){
				log.warn("Should not have a count different zero " + count);
			}
		}
		this.vehId2CountsMap = new HashMap<Id, Integer>();
		this.lineIds = new TreeSet<String>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// add the number of passengers of the vehicle to the total amount of that link. ignore every non paratransit vehicle
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if(this.linkId2LineId2CountsMap.get(event.getLinkId()) == null){
				this.linkId2LineId2CountsMap.put(event.getLinkId(), new HashMap<String, Integer>());
			}
			
			String lineId = event.getVehicleId().toString().split("-")[0];
			this.lineIds.add(lineId);
			
			if (this.linkId2LineId2CountsMap.get(event.getLinkId()).get(lineId) == null) {
				this.linkId2LineId2CountsMap.get(event.getLinkId()).put(lineId, new Integer(0)); // initialize with one, implying that the link actually was served
			}
			
			if(this.vehId2CountsMap.get(event.getVehicleId()) != null){
				int oldValue = this.linkId2LineId2CountsMap.get(event.getLinkId()).get(lineId).intValue();
				int additionalValue = this.vehId2CountsMap.get(event.getVehicleId()).intValue();
				this.linkId2LineId2CountsMap.get(event.getLinkId()).put(lineId, new Integer(oldValue + additionalValue));
			}
		}		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// add a passenger to the vehicle counts data, but ignore every non paratransit vehicle and every driver
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				if(this.vehId2CountsMap.get(event.getVehicleId()) == null){
					this.vehId2CountsMap.put(event.getVehicleId(), new Integer(0));
				}
				int oldValue = this.vehId2CountsMap.get(event.getVehicleId()).intValue();
				this.vehId2CountsMap.put(event.getVehicleId(), new Integer(oldValue + 1));
			}
		}		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// subtract a passenger to the vehicle counts data, but ignore every non paratransit vehicle and every driver
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				this.vehId2CountsMap.put(event.getVehicleId(), this.vehId2CountsMap.get(event.getVehicleId()).intValue() - 1);
			}
		}		
	}
}