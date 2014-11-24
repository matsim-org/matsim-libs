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
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.PtConstants;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;


/**
 * Counts the number of trips per ptMode-chains.
 * 
 * @author aneumann
 *
 */
final class CountTripsPerPtModeCombination extends AbstractPAnalyisModule implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, ActivityStartEventHandler{
	
	private final static Logger log = Logger.getLogger(CountTripsPerPtModeCombination.class);
	
	private HashMap<Id<Vehicle>, String> vehId2ptModeMap;
	private HashMap<String, Integer> ptModeCombination2TripCountMap;
	private HashMap<Id<Person>, String> agentId2TripCombination = new HashMap<>();
	
	public CountTripsPerPtModeCombination(){
		super(CountTripsPerPtModeCombination.class.getSimpleName());
		log.info("enabled");
	}
	
	@Override
	public String getHeader() {
		// need to write an own header
		return ", There is no header. Please refer to each iterations results separately.";
	}

	@Override
	public String getResult() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModeCombination2TripCountMap.keySet()) {
			strB.append(", " + ptMode + ": " + this.ptModeCombination2TripCountMap.get(ptMode));
		}
		return strB.toString();
	}
	
	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		this.vehId2ptModeMap = new HashMap<>();
		this.ptModeCombination2TripCountMap = new HashMap<>();
		this.agentId2TripCombination = new HashMap<>();
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
		if(!super.ptDriverIds.contains(event.getPersonId())){
			String ptMode = this.vehId2ptModeMap.get(event.getVehicleId());
			if (ptMode == null) {
				ptMode = "nonPtMode";
			}
			if (this.agentId2TripCombination.get(event.getPersonId()) == null) {
				this.agentId2TripCombination.put(event.getPersonId(), ptMode);
			} else {
				// it's a transfer - extend the agent's trip combination
				String tripCombination = this.agentId2TripCombination.get(event.getPersonId()) + "-" + ptMode;
				this.agentId2TripCombination.put(event.getPersonId(), tripCombination);
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(!super.ptDriverIds.contains(event.getPersonId())){
			if (!event.getActType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				// trip finished
				String tripCombination = this.agentId2TripCombination.get(event.getPersonId());
				if (tripCombination != null) {
					if (this.ptModeCombination2TripCountMap.get(tripCombination) == null) {
						this.ptModeCombination2TripCountMap.put(tripCombination, 0);
					}
					this.ptModeCombination2TripCountMap.put(tripCombination, this.ptModeCombination2TripCountMap.get(tripCombination) + 1);
				} 
				// reset the last used pt mode
				this.agentId2TripCombination.remove(event.getPersonId());
			}
		}
	}
}
