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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.util.Log;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class AnalysisTripSet {

	private Geometry zone;
	private Map<String, Map<String, Double>> zoneMap;
	private Map<String, Integer> zoneCounter;
	
	private final String INSIDE = "inside";
	private final String OUTSIDE = "outside";
	private final String CROSSING = "crossing";

	/**
	 * @param zones
	 */
	public AnalysisTripSet(Geometry zone) {
		this.zone = zone;
		this.generateZoneMaps();
	}
	
	private void generateZoneMaps() {
		this.zoneMap = new HashMap<String, Map<String,Double>>();
		this.zoneCounter = new HashMap<String, Integer>();
		
		this.zoneMap.put(this.INSIDE, this.generateValueMap());
		this.zoneCounter.put(this.INSIDE, 0);
		
		if(!(this.zone == null)){
			this.zoneMap.put(this.OUTSIDE, this.generateValueMap());
			this.zoneCounter.put(this.OUTSIDE, 0);
			
			this.zoneMap.put(this.CROSSING, this.generateValueMap());
			this.zoneCounter.put(this.CROSSING, 0);
		}
	}
	
	private Map<String, Double> generateValueMap(){
		return AnalysisTrip.getEmptyValueMap();
	}
	
	public AnalysisTripSet(){
		this(null);
	}

	/**
	 * @param trip
	 */
	public void addTrip(AnalysisTrip trip) {
		String location = getTripLocation(trip);
		this.zoneCounter.put(location, this.zoneCounter.get(location) + 1);
		
		for(Entry<String, Double> e : trip.analyze().entrySet()){
			this.zoneMap.get(location).put(e.getKey(), 
					this.zoneMap.get(location).get(e.getKey()) + e.getValue());
		}
	}
	
	
	public void addTrips(List<AnalysisTrip> trips){
		int nextMsg = 1;
		int counter = 0;
		for(AnalysisTrip trip : trips){
			this.addTrip(trip);
			counter++;
			if(counter % nextMsg == 0){
				Log.info("processed " + counter + " of " + trips.size());
				nextMsg *= 2;
			}
		}
	}
	
	
	public String getTripLocation(AnalysisTrip trip){
		if(this.zone == null){
			return this.INSIDE;
		}else if(this.zone.contains(trip.getStart()) && this.zone.contains(trip.getEnd())){
			return this.INSIDE;
		}else if(!this.zone.contains(trip.getStart()) && this.zone.contains(trip.getEnd())){
			return this.CROSSING;
		}else if(this.zone.contains(trip.getStart()) && !this.zone.contains(trip.getEnd())){
			return this.CROSSING;
		}else {
			return this.OUTSIDE;
		}
	}
	//TODO getter&Setter, sortByLocationType
	
	public Map<String, Map<String, Double>> getValues(){
		Map<String, Map<String, Double>> temp = new HashMap<String, Map<String,Double>>();
		//TODO calculate values
		
		return temp;
	}
}
