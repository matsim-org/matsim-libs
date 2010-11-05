/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.level2;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

import playground.andreas.bvgAna.level1.StopId2RouteId2DelayAtStopMap;
import playground.andreas.bvgAna.level1.StopId2RouteId2DelayAtStopMapData;

/**
 * Calculates the average delay per line for each stop
 * 
 * @author aneumann
 *
 */
public class StopId2DelayOfLine24hMap implements VehicleDepartsAtFacilityEventHandler, TransitDriverStartsEventHandler{
	
	private final Logger log = Logger.getLogger(StopId2DelayOfLine24hMap.class);
	private final Level logLevel = Level.DEBUG;
	
	private final StopId2RouteId2DelayAtStopMap stopId2RouteId2DelayAtStopMap = new StopId2RouteId2DelayAtStopMap();
	private TreeMap<Id, StopId2DelayOfLine24hMapData> stopId2DelayOfLine24hMap = null;
	
	public StopId2DelayOfLine24hMap(){
		this.log.setLevel(this.logLevel);
	}
	
	/**
	 * @return Returns a map containing the average delay per line for each stop
	 */
	public TreeMap<Id, StopId2DelayOfLine24hMapData> getStopId2DelayOfLine24hMap(){
		if(this.stopId2DelayOfLine24hMap == null){
			calculateStopId2DelayOfLine24hMap();
		}
		return this.stopId2DelayOfLine24hMap;
	}

	private void calculateStopId2DelayOfLine24hMap() {
		TreeMap<Id, TreeMap<Id, StopId2RouteId2DelayAtStopMapData>> results = this.stopId2RouteId2DelayAtStopMap.getStopId2RouteId2DelayAtStopMap();
		this.stopId2DelayOfLine24hMap = new TreeMap<Id, StopId2DelayOfLine24hMapData>();
		
		for (Entry<Id, TreeMap<Id, StopId2RouteId2DelayAtStopMapData>> stopEntry : results.entrySet()) {
			StopId2DelayOfLine24hMapData stopId2DelayOfLine24hMapData = new StopId2DelayOfLine24hMapData(stopEntry.getKey());
			this.stopId2DelayOfLine24hMap.put(stopEntry.getKey(), stopId2DelayOfLine24hMapData);
			
			for (StopId2RouteId2DelayAtStopMapData routeData : stopEntry.getValue().values()) {
				Id lineId = routeData.getLineId();
				
				ArrayList<Double> plannedDepartures = routeData.getPlannedDepartures();
				ArrayList<Double> realizedDepartures = routeData.getRealizedDepartures();
				
				if(plannedDepartures.size() != realizedDepartures.size()){
					this.log.error("Number of planned and realized deparutes NOT the same.");
				}
				
				for (int i = 0; i < plannedDepartures.size(); i++) {
					stopId2DelayOfLine24hMapData.addDelayForLine(lineId, realizedDepartures.get(i).doubleValue() - plannedDepartures.get(i).doubleValue());
				}				
			}			
		}
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.stopId2RouteId2DelayAtStopMap.handleEvent(event);		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.stopId2RouteId2DelayAtStopMap.handleEvent(event);		
	}
	
	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");		
	}
}
