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

package playground.andreas.P2.stats;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;

/**
 * Counts the number of passenger of paratransit vehicles per link
 * 
 * @author aneumann
 *
 */
public class CountPPaxHandler implements LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private static final Logger log = Logger.getLogger(CountPPaxHandler.class);
	
	private String pIdentifier;
	private HashMap<Id, Integer> linkId2CountsTable;
	private HashMap<Id, Integer> vehId2CountsMap;

	public CountPPaxHandler(String pIdentifier) {
		this.pIdentifier = pIdentifier;
		this.linkId2CountsTable = new HashMap<Id, Integer>();
		this.vehId2CountsMap =  new HashMap<Id, Integer>();
	}

	public int getPaxCountForLinkId(Id linkId){
		Integer count = this.linkId2CountsTable.get(linkId);
		if(count == null){
			return 0;
		} else {
			return count.intValue();
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2CountsTable = new HashMap<Id, Integer>();
		for (Integer count : this.vehId2CountsMap.values()) {
			if(count != 0){
				log.warn("Should not have a count different zero " + count);
			}
		}
		this.vehId2CountsMap = new HashMap<Id, Integer>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// add the number of passengers of the vehicle to the total amount of that link. ignore every non paratransit vehicle
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if(this.linkId2CountsTable.get(event.getLinkId()) == null){
				this.linkId2CountsTable.put(event.getLinkId(), new Integer(0));
			}
			
			if(this.vehId2CountsMap.get(event.getVehicleId()) != null){
				int oldValue = this.linkId2CountsTable.get(event.getLinkId());
				int additionalValue = this.vehId2CountsMap.get(event.getVehicleId()).intValue();
				this.linkId2CountsTable.put(event.getLinkId(), new Integer(oldValue + additionalValue));
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