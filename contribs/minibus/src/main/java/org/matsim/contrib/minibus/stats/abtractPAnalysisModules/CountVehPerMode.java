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
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;


/**
 * Count the number of vehicles per ptModes specified.
 * 
 * @author aneumann
 *
 */
final class CountVehPerMode extends AbstractPAnalyisModule implements TransitDriverStartsEventHandler{
	
	private final static Logger log = Logger.getLogger(CountVehPerMode.class);
	
	private HashMap<String, Set<Id<Vehicle>>> ptMode2VehIdsMap;
	
	public CountVehPerMode(){
		super(CountVehPerMode.class.getSimpleName());
		log.info("enabled");
	}

	@Override
	public String getResult() {
		StringBuffer strB = new StringBuffer();
		for (String ptMode : this.ptModes) {
			strB.append(", " + this.ptMode2VehIdsMap.get(ptMode).size());
		}
		return strB.toString();
	}
	
	@Override
	public void reset(int iteration) {
		this.ptMode2VehIdsMap = new HashMap<>();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		String ptMode = this.lineIds2ptModeMap.get(event.getTransitLineId());
		if (ptMode == null) {
			log.warn("Could not find a valid pt mode for transit line " + event.getTransitLineId());
			ptMode = "no valid pt mode found";
		}
		if (this.ptMode2VehIdsMap.get(ptMode) == null) {
			this.ptMode2VehIdsMap.put(ptMode, new TreeSet<Id<Vehicle>>());
		}

		this.ptMode2VehIdsMap.get(ptMode).add(event.getVehicleId());
	}
}
