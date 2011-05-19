/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.Analysis.Trips.travelTime;

import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class TTAnalysisTripSetAllMode {
	
	private Map<String, TTAnalysisTripSetOneMode> mode2TripSet = new HashMap<String, TTAnalysisTripSetOneMode>();
	private boolean storeTrips;
	private Geometry zone;
	
	public TTAnalysisTripSetAllMode(boolean storeTrips, Geometry zone){
		this.storeTrips = storeTrips;
		this.zone = zone;
	}
	
	public void addTrip(AbstractTTAnalysisTrip trip){
		String mode = trip.getMode();
		
		if(this.mode2TripSet.containsKey(mode)){
			this.mode2TripSet.get(mode).addTrip(trip);
		}else{
			TTAnalysisTripSetOneMode temp = new TTAnalysisTripSetOneMode(mode, this.zone, this.storeTrips);
			temp.addTrip(trip);
			this.mode2TripSet.put(mode, temp);
		}
	}
	
	public Map<String, TTAnalysisTripSetOneMode> getTripSets(){
		return this.mode2TripSet;
	}
}
