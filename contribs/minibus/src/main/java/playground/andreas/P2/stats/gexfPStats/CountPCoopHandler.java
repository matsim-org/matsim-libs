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
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;

/**
 * Counts the number of cooperatives per link
 * 
 * @author aneumann
 *
 */
public class CountPCoopHandler implements LinkEnterEventHandler, TransitDriverStartsEventHandler{
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(CountPCoopHandler.class);
	
	private String pIdentifier;
	private HashMap<Id, Set<Id>> linkId2CoopIdsSetMap;
	private HashMap<Id, Id> vehId2lineIdMap;

	public CountPCoopHandler(String pIdentifier) {
		this.pIdentifier = pIdentifier;
		this.linkId2CoopIdsSetMap = new HashMap<Id, Set<Id>>();
		this.vehId2lineIdMap = new HashMap<Id, Id>();
	}

	public Set<Id> getCoopsForLinkId(Id linkId) {
		Set<Id> lineIds = this.linkId2CoopIdsSetMap.get(linkId);
		if(lineIds == null){
			return new TreeSet<Id>();
		} else {
			return lineIds;
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2CoopIdsSetMap = new HashMap<Id, Set<Id>>();
		this.vehId2lineIdMap = new HashMap<Id, Id>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// add the id of the cooperative to the set of ids of the link
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if (this.linkId2CoopIdsSetMap.get(event.getLinkId()) == null) {
				this.linkId2CoopIdsSetMap.put(event.getLinkId(), new TreeSet<Id>());
			}
			
			this.linkId2CoopIdsSetMap.get(event.getLinkId()).add(this.vehId2lineIdMap.get(event.getVehicleId()));
		}		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			this.vehId2lineIdMap.put(event.getVehicleId(), event.getTransitLineId());
		}		
	}
}