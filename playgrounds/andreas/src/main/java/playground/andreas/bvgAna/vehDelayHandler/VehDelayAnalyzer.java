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

package playground.andreas.bvgAna.vehDelayHandler;

import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

/**
 * Searches the next planned and next realized departures for a given stop and time.
 * 
 * @author aneumann
 *
 */
public class VehDelayAnalyzer {
	
	private final Logger log = Logger.getLogger(VehDelayAnalyzer.class);
	private final Level logLevel = Level.DEBUG;
	
	private TreeMap<Id, TreeMap<Id, VehDelayAtStopContainer>> stopId2Route2DelayAtStopMap;
	
	
	public VehDelayAnalyzer(VehDelayHandler vehicleDelayHandler){
		this.log.setLevel(this.logLevel);
		this.stopId2Route2DelayAtStopMap = vehicleDelayHandler.getStopId2Route2DelayAtStopMap();
	}
	
	/** 
	 * @param stopId The stop
	 * @param time The time
	 * @param lineId
	 * @param routeId
	 * @return Returns the next planned departure time in the future for a given stop and time.
	 */
	public double getNextPlannedDepartureTime(Id stopId, double time, Id lineId, Id routeId){
		this.log.debug("TransitAgent.getEnterTransitRoute only checks for line id and stops to come ignoring route id. Agent probably too fast in simulation compared to plan.");
		TreeMap<Id, VehDelayAtStopContainer> possibleRoutes = this.stopId2Route2DelayAtStopMap.get(stopId);		
		double closestDepartureTime = Double.POSITIVE_INFINITY;
		
		for (VehDelayAtStopContainer delayContainer : possibleRoutes.values()) {
			
			if(delayContainer.getLineId().toString().equalsIgnoreCase(lineId.toString()) && delayContainer.getRouteId().toString().equalsIgnoreCase(routeId.toString())){
				ArrayList<Double> plannedDepartures = delayContainer.getPlannedDepartures();
				for (int i = 0; i < plannedDepartures.size(); i++) {
					if(plannedDepartures.get(i).doubleValue() > time){
						if(plannedDepartures.get(i).doubleValue() < closestDepartureTime){
							closestDepartureTime = plannedDepartures.get(i).doubleValue();
						}					
					}
				}
			}
			
		}
		
		if(closestDepartureTime == Double.POSITIVE_INFINITY){
			this.log.warn("Could not find next planned departure time for stop " + stopId + ", line " + lineId + " route " + routeId + " and time " + time + "s. Returning positive Inf");
		}
		return closestDepartureTime;
	}
	
	/** 
	 * @param stopId The stop
	 * @param time The time
	 * @param lineId
	 * @param routeId
	 * @return Returns the next realized departure time in the future for a given stop and time.
	 */
	public double getNextRealizedDepartureTime(Id stopId, double time, Id lineId, Id routeId){
		this.log.debug("TransitAgent.getEnterTransitRoute only checks for line id and stops to come ignoring route id. Agent probably too fast in simulation compared to plan.");
		TreeMap<Id, VehDelayAtStopContainer> possibleRoutes = this.stopId2Route2DelayAtStopMap.get(stopId);		
		double closestDepartureTime = Double.POSITIVE_INFINITY;
		
		for (VehDelayAtStopContainer delayContainer : possibleRoutes.values()) {
			
			if(delayContainer.getLineId().toString().equalsIgnoreCase(lineId.toString()) && delayContainer.getRouteId().toString().equalsIgnoreCase(routeId.toString())){
				ArrayList<Double> realizedDepartures = delayContainer.getRealizedDepartures();
				for (int i = 0; i < realizedDepartures.size(); i++) {
					if(realizedDepartures.get(i).doubleValue() > time){
						if(realizedDepartures.get(i).doubleValue() < closestDepartureTime){
							closestDepartureTime = realizedDepartures.get(i).doubleValue();
						}					
					}
				}
			}
		}
		
		if(closestDepartureTime == Double.POSITIVE_INFINITY){
			this.log.warn("Could not find next realized departure time for stop " + stopId + ", line " + lineId + " route " + routeId + " and time " + time + "s. Returning positive Inf");
		}
		return closestDepartureTime;
	}
}
