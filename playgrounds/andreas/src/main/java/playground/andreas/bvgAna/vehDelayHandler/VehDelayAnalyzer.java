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
	
	private TreeMap<Id,VehDelayAtStopContainer> stopId2DelayAtStopMap;
	
	
	public VehDelayAnalyzer(VehDelayHandler vehicleDelayHandler){
		this.log.setLevel(this.logLevel);
		this.stopId2DelayAtStopMap = vehicleDelayHandler.getStopId2DelayAtStopMap();
	}
	
	/** 
	 * @param stopId The stop
	 * @param time The time
	 * @return Returns the next planned departure time in the future for a given stop and time.
	 */
	public double getNextPlannedDepartureTime(Id stopId, double time){
		this.log.debug("Should depend on lineId and routeId as well.");
		ArrayList<Double> plannedDepartures = this.stopId2DelayAtStopMap.get(stopId).getPlannedDepartures();
		for (int i = 0; i < plannedDepartures.size(); i++) {
			if(plannedDepartures.get(i).doubleValue() > time){
				return plannedDepartures.get(i).doubleValue();
			}
		}
		this.log.warn("Could not find next planned departure time at stop " + stopId + " at " + time + "s. Returning negative Inf");
		return Double.NEGATIVE_INFINITY;
	}
	
	/** 
	 * @param stopId The stop
	 * @param time The time
	 * @return Returns the next realized departure time in the future for a given stop and time.
	 */
	public double getNextRealizedDepartureTime(Id stopId, double time){
		this.log.debug("Should depend on lineId and routeId as well.");
		ArrayList<Double> realizedDepartures = this.stopId2DelayAtStopMap.get(stopId).getRealizedDepartures();
		for (int i = 0; i < realizedDepartures.size(); i++) {
			if(realizedDepartures.get(i).doubleValue() > time){
				return realizedDepartures.get(i).doubleValue();
			}
		}
		this.log.warn("Could not find next realized departure time at stop " + stopId + " at " + time + "s. Returning negative Inf");
		return Double.NEGATIVE_INFINITY;
	}

}
