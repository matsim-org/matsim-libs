/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package vwExamples.peoplemoverVWExample.CustomRebalancing;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.BasicEventHandler;



public class ZonalRelocationAggregatorMy implements BasicEventHandler {
	private final DrtZonalSystem zonalSystem;
	private final int binsize = 600; //Time resolution to store demand (departures)
	public Map<Double,Map<String,MutableInt>> relocations = new HashMap<>();
	public Map<Double,Map<String,MutableInt>> previousIterationRelocations = new HashMap<>();
	

	@Inject
	public ZonalRelocationAggregatorMy(EventsManager events, DrtZonalSystem zonalSystem, Config config) {
		this.zonalSystem= zonalSystem;
		events.addHandler(this);
	}
	

	public void reset(int iteration){
		previousIterationRelocations.clear();
		previousIterationRelocations.putAll(relocations);
		relocations.clear(); //Clear rejections to store new rejectedRequests of current iteration
		prepareZones(); //Initialize zones again in order to store new rejectedRequests of current iteration
	}
	
	
	private void prepareZones(){
		for (int i = 0;i<(3600/binsize)*36;i++){
			//Slot is a times slot
			//For each slot, we have a Zone map
			Map<String,MutableInt> zonesPerSlot = new HashMap<>();
			for (String zone : zonalSystem.getZones().keySet()){
				zonesPerSlot.put(zone, new MutableInt());
			}
			relocations.put(Double.valueOf(i),zonesPerSlot);

		}
	}
	
	Double getBinForTime(double time){
		
		return Math.floor(time/binsize); 
	}
	
	public Map<String,MutableInt> getExpectedRejectsForTimeBin(double time){
		Double bin = getBinForTime(time);
		return previousIterationRelocations.get(bin);
	}


	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}
	
}

