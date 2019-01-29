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
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;


/**
 * Counts the number of departure per ptModes specified.
 * 
 * @author aneumann
 *
 */
final class CountDeparturesPerMode extends AbstractPAnalyisModule implements TransitDriverStartsEventHandler, VehicleDepartsAtFacilityEventHandler{
	
	private final static Logger log = Logger.getLogger(CountDeparturesPerMode.class);
	
	private HashMap<Id<Vehicle>, String> vehId2ptModeMap;
	private HashMap<String, Integer> ptMode2nOfDepartures = new HashMap<>();
	
	public CountDeparturesPerMode(){
		super(CountDeparturesPerMode.class.getSimpleName());
		log.info("enabled");
	}

	@Override
	public String getResult() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModes) {
			strB.append(", " + (this.ptMode2nOfDepartures.get(ptMode)));
		}
		return strB.toString();
	}
	
	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		this.vehId2ptModeMap = new HashMap<>();
		this.ptMode2nOfDepartures = new HashMap<>();
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
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		String ptMode = this.vehId2ptModeMap.get(event.getVehicleId());
		if (ptMode == null) {
			ptMode = "nonPtMode";
		}
		if (this.ptMode2nOfDepartures.get(ptMode) == null) {
			this.ptMode2nOfDepartures.put(ptMode, 0);
		}
		
		int oldValue = this.ptMode2nOfDepartures.get(ptMode);
		this.ptMode2nOfDepartures.put(ptMode, oldValue + 1);
	}
}
