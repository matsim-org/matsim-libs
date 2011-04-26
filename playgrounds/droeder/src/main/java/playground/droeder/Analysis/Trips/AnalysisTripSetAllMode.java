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
package playground.droeder.Analysis.Trips;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class AnalysisTripSetAllMode {
	
	private Map<String, AnalysisTripSetOneMode> mode2TripSet = new HashMap<String, AnalysisTripSetOneMode>();
	private boolean storeTrips;
	private Geometry zone;
	
	public AnalysisTripSetAllMode(boolean storeTrips, Geometry zone){
		this.storeTrips = storeTrips;
		this.zone = zone;
	}
	
	public void addTrip(AnalysisTrip trip){
		String mode = trip.getMode();
		
		if(this.mode2TripSet.containsKey(mode)){
			this.mode2TripSet.get(mode).addTrip(trip);
		}else{
			AnalysisTripSetOneMode temp = new AnalysisTripSetOneMode(mode, this.zone, this.storeTrips);
			temp.addTrip(trip);
			this.mode2TripSet.put(mode, temp);
		}
	}
	
	@Override
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		boolean header = true;
		
		for(Entry<String, AnalysisTripSetOneMode> e : this.mode2TripSet.entrySet()){
			buffer.append(e.getValue().toString(header));
			header = false;
		}
		
		return buffer.toString();
	}
	
	public Map<String, AnalysisTripSetOneMode> getTripSets(){
		return this.mode2TripSet;
	}
}
