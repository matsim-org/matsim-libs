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

package org.matsim.contrib.minibus.stats.abtractPAnalysisModules;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;


/**
 * Count the number of passenger-meter per ptModes specified.
 * 
 * @author aneumann
 *
 */
final class CountPassengerMeterPerMode extends AbstractPAnalyisModule implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler{
	
	private final static Logger log = Logger.getLogger(CountPassengerMeterPerMode.class);
	
	private final Network network;
	private HashMap<Id<Vehicle>, String> vehId2ptModeMap;
	private HashMap<String, Double> ptMode2CountMap;
	private HashMap<Id<Vehicle>, Integer> vehId2NumberOfPassengers = new HashMap<>();

	
	public CountPassengerMeterPerMode(Network network){
		super(CountPassengerMeterPerMode.class.getSimpleName());
		this.network = network;
		log.info("enabled");
	}

	@Override
	public String getResult() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModes) {
			strB.append(", " + this.ptMode2CountMap.get(ptMode));
		}
		return strB.toString();
	}
	
	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		this.vehId2ptModeMap = new HashMap<>();
		this.ptMode2CountMap = new HashMap<>();
		this.vehId2NumberOfPassengers = new HashMap<>();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		super.handleEvent(event);
		String ptMode = this.lineIds2ptModeMap.get(event.getTransitLineId());
		if (ptMode == null) {
			log.warn("Could not find a valid pt mode for transit line " + event.getTransitLineId());
			ptMode = "no valid pt mode found";
		}
		this.vehId2ptModeMap.put(event.getVehicleId(), ptMode);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.vehId2NumberOfPassengers.get(event.getVehicleId()) == null) {
			this.vehId2NumberOfPassengers.put(event.getVehicleId(), 0);
		}
		
		if(!super.ptDriverIds.contains(event.getPersonId())){
			this.vehId2NumberOfPassengers.put(event.getVehicleId(), this.vehId2NumberOfPassengers.get(event.getVehicleId()) + 1);
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(!super.ptDriverIds.contains(event.getPersonId())){
			this.vehId2NumberOfPassengers.put(event.getVehicleId(), this.vehId2NumberOfPassengers.get(event.getVehicleId()) - 1);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		String ptMode = this.vehId2ptModeMap.get(event.getVehicleId());
		if (ptMode == null) {
			ptMode = "nonPtMode";
		}
		if (ptMode2CountMap.get(ptMode) == null) {
			ptMode2CountMap.put(ptMode, 0.0);
		}

		ptMode2CountMap.put(ptMode, ptMode2CountMap.get(ptMode) + this.network.getLinks().get(event.getLinkId()).getLength() * this.vehId2NumberOfPassengers.get(event.getVehicleId()));
	}
	
	public HashMap<String, Double> getResults(){
		return this.ptMode2CountMap;
	}

}
