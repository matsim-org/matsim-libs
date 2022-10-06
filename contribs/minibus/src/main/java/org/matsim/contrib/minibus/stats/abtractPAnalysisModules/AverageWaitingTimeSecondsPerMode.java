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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;


/**
 * Calculates the average waiting time per trip and per ptModes specified. This is from departure event to PersonEntersVehicleEvent.
 * 
 * @author aneumann
 *
 */
final class AverageWaitingTimeSecondsPerMode extends AbstractPAnalyisModule implements PersonDepartureEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler{
	
	private final static Logger log = LogManager.getLogger(AverageWaitingTimeSecondsPerMode.class);
	
	private HashMap<Id<Vehicle>, String> vehId2ptModeMap;
	private HashMap<String, Double> ptMode2SecondsTravelledMap;
	private HashMap<String, Integer> ptMode2TripCountMap;
	private HashMap<Id<Person>, Double> agentId2AgentDepartureEventTime = new HashMap<>();
	
	public AverageWaitingTimeSecondsPerMode(){
		super(AverageWaitingTimeSecondsPerMode.class.getSimpleName());
		log.info("enabled");
	}

	@Override
	public String getResult() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModes) {
			strB.append(", " + (this.ptMode2SecondsTravelledMap.get(ptMode) / this.ptMode2TripCountMap.get(ptMode)));
		}
		return strB.toString();
	}
	
	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		this.vehId2ptModeMap = new HashMap<>();
		this.ptMode2SecondsTravelledMap = new HashMap<>();
		this.ptMode2TripCountMap = new HashMap<>();
		this.agentId2AgentDepartureEventTime = new HashMap<>();
		// avoid null-pointer in getResult() /dr
		for (String ptMode : this.ptModes) {
			this.ptMode2SecondsTravelledMap.put(ptMode, 0.);
			this.ptMode2TripCountMap.put(ptMode, 0);
		}
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
	public void handleEvent(PersonDepartureEvent event) {
		if(!super.ptDriverIds.contains(event.getPersonId())){
			this.agentId2AgentDepartureEventTime.put(event.getPersonId(), event.getTime());
		}
		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(!super.ptDriverIds.contains(event.getPersonId())){
			String ptMode = this.vehId2ptModeMap.get(event.getVehicleId());
			if (ptMode == null) {
				ptMode = "nonPtMode";
			}
			
			if (ptMode2SecondsTravelledMap.get(ptMode) == null) {
				ptMode2SecondsTravelledMap.put(ptMode, 0.0);
			}
			if (ptMode2TripCountMap.get(ptMode) == null) {
				ptMode2TripCountMap.put(ptMode, 0);
			}
			
			this.ptMode2SecondsTravelledMap.put(ptMode, this.ptMode2SecondsTravelledMap.get(ptMode) + (event.getTime() - this.agentId2AgentDepartureEventTime.get(event.getPersonId())));
			this.ptMode2TripCountMap.put(ptMode, this.ptMode2TripCountMap.get(ptMode) + 1);
		}
	}
}
