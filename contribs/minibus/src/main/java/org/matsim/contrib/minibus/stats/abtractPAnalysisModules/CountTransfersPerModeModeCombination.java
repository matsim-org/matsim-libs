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
import java.util.LinkedList;


/**
 * Counts the number of transfers in each combination of the ptModes specified.
 * 
 * @author aneumann
 *
 */
final class CountTransfersPerModeModeCombination extends AbstractPAnalyisModule implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, ActivityStartEventHandler{
	
	private final static Logger log = Logger.getLogger(CountTransfersPerModeModeCombination.class);
	
	private LinkedList<String> ptModeCombinations = null;
	private HashMap<Id<Vehicle>, String> vehId2ptModeMap;
	private HashMap<String, Integer> ptModeCombination2TripCountMap;
	private HashMap<Id<Person>, String> agentId2LastPtModeUsed = new HashMap<>();
	
	public CountTransfersPerModeModeCombination(){
		super(CountTransfersPerModeModeCombination.class.getSimpleName());
		log.info("enabled");
	}
	
	@Override
	public String getHeader() {
		// need to write an own header
		
		if (this.ptModeCombinations == null) {
			// calculate all pt mode combinations
			this.ptModeCombinations = new LinkedList<>();
			for (String ptModeFrom : this.ptModes) {
				for (String ptModeTo : this.ptModes) {
					this.ptModeCombinations.add(ptModeFrom + "-" + ptModeTo);
				}
			}
		}
		
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModeCombinations) {
			strB.append(", " + ptMode);
		}
		return strB.toString();
	}

	@Override
	public String getResult() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModeCombinations) {
			if (this.ptModeCombination2TripCountMap.get(ptMode) == null) {
				strB.append(", " + 0);
			} else {
				strB.append(", " + this.ptModeCombination2TripCountMap.get(ptMode));
			}
		}
		return strB.toString();
	}
	
	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		this.vehId2ptModeMap = new HashMap<>();
		this.ptModeCombination2TripCountMap = new HashMap<>();
		this.agentId2LastPtModeUsed = new HashMap<>();
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
			if (ptModeCombination2TripCountMap.get(ptMode) == null) {
				ptModeCombination2TripCountMap.put(ptMode, 0);
			}
			
			if (this.agentId2LastPtModeUsed.get(event.getPersonId()) != null) {
				// it's a transfer
				String tripCombination = this.agentId2LastPtModeUsed.get(event.getPersonId()) + "-" + ptMode;
				if (this.ptModeCombination2TripCountMap.get(tripCombination) == null) {
					this.ptModeCombination2TripCountMap.put(tripCombination, 0);
				}
				this.ptModeCombination2TripCountMap.put(tripCombination, this.ptModeCombination2TripCountMap.get(tripCombination) + 1);
			}
			this.agentId2LastPtModeUsed.put(event.getPersonId(), ptMode);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!event.getActType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			// reset the last used pt mode
			this.agentId2LastPtModeUsed.remove(event.getPersonId());
		}
	}
}
