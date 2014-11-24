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
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * Counts the number of operators per link
 * 
 * @author aneumann
 *
 */
final class CountPOperatorHandler implements LinkEnterEventHandler, TransitDriverStartsEventHandler{
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(CountPOperatorHandler.class);
	
	private final String pIdentifier;
	private HashMap<Id<Link>, Set<Id<Operator>>> linkId2OperatorIdsSetMap;
	private HashMap<Id<Vehicle>, Id<TransitLine>> vehId2lineIdMap;

	public CountPOperatorHandler(String pIdentifier) {
		this.pIdentifier = pIdentifier;
		this.linkId2OperatorIdsSetMap = new HashMap<>();
		this.vehId2lineIdMap = new HashMap<>();
	}

	public Set<Id<Operator>> getOperatorIdsForLinkId(Id<Link> linkId) {
		Set<Id<Operator>> operatorIds = this.linkId2OperatorIdsSetMap.get(linkId);
		if(operatorIds == null){
			return new TreeSet<>();
		} else {
			return operatorIds;
		}
	}

	@Override
	public void reset(int iteration) {
		this.linkId2OperatorIdsSetMap = new HashMap<>();
		this.vehId2lineIdMap = new HashMap<>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// add the id of the operator to the set of ids of the link
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if (this.linkId2OperatorIdsSetMap.get(event.getLinkId()) == null) {
				this.linkId2OperatorIdsSetMap.put(event.getLinkId(), new TreeSet<Id<Operator>>());
			}
			
			Id<Operator> operatorId = Id.create(this.vehId2lineIdMap.get(event.getVehicleId()), Operator.class);
			this.linkId2OperatorIdsSetMap.get(event.getLinkId()).add(operatorId);
		}		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			this.vehId2lineIdMap.put(event.getVehicleId(), event.getTransitLineId());
		}		
	}
}