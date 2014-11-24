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

package org.matsim.contrib.minibus.stats;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Counts the number of paratransit vehicles per link
 * 
 * @author aneumann
 *
 */
final class CountPVehHandler implements LinkEnterEventHandler{
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(CountPVehHandler.class);
	
	private final String pIdentifier;
	private HashMap<Id<Link>, HashMap<String, Integer>> linkId2LineId2CountsMap;
	// TODO [AN] Check if this can be replaced by Set<Id<TransitLine/Operator>> lineIds
	private Set<String> lineIds;

	public CountPVehHandler(String pIdentifier) {
		this.pIdentifier = pIdentifier;
		this.linkId2LineId2CountsMap = new HashMap<>();
		this.lineIds = new TreeSet<>();
	}
	
	public Set<String> getLineIds(){
		return this.lineIds;
	}

	public int getVehCountForLinkId(Id<Link> linkId){
		int count = 0;
		if (this.linkId2LineId2CountsMap.get(linkId) != null) {
			for (Integer countEntryForLine : this.linkId2LineId2CountsMap.get(linkId).values()) {
				count += countEntryForLine;
			}
		}
		return count;
	}
	
	public int getVehCountForLinkId(Id<Link> linkId, String lineId){
		if (this.linkId2LineId2CountsMap.get(linkId) != null) {
			if (this.linkId2LineId2CountsMap.get(linkId).get(lineId) != null) {
				return this.linkId2LineId2CountsMap.get(linkId).get(lineId);
			}
		}
		return 0;
	}

	@Override
	public void reset(int iteration) {
		this.linkId2LineId2CountsMap = new HashMap<>();
		this.lineIds = new TreeSet<>();
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
				this.linkId2LineId2CountsMap.get(event.getLinkId()).put(lineId, 0); // initialize with one, implying that the link actually was served
			}
			
			int oldValue = this.linkId2LineId2CountsMap.get(event.getLinkId()).get(lineId);
			int additionalValue = 1;
			this.linkId2LineId2CountsMap.get(event.getLinkId()).put(lineId, oldValue + additionalValue);
		}		
	}
}