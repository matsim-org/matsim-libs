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

import org.apache.log4j.Logger;

import playground.droeder.Analysis.Trips.distance.DistAnalysisTripI;
import playground.droeder.Analysis.Trips.distance.DistanceAnalysisTripSet;
import playground.droeder.Analysis.Trips.travelTime.TTAnalysisTripI;
import playground.droeder.Analysis.Trips.travelTime.TTAnalysisTripSet;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class AnalysisTripSetStorage {
	private static final Logger log = Logger
			.getLogger(AnalysisTripSetStorage.class);
	
	private Map<String, AbstractAnalysisTripSet> mode2TripSet = new HashMap<String, AbstractAnalysisTripSet>();
	private boolean storeTrips;
	private Geometry zone;
	
	public AnalysisTripSetStorage(boolean storeTrips, Geometry zone){
		this.storeTrips = storeTrips;
		this.zone = zone;
	}
	
	public void addTrip(AbstractAnalysisTrip trip){
		String mode = trip.getMode();
		
		if(this.mode2TripSet.containsKey(mode)){
			this.mode2TripSet.get(mode).addTrip(trip);
		}else{
			
			AbstractAnalysisTripSet temp;
			if(trip instanceof DistAnalysisTripI){
				temp = new DistanceAnalysisTripSet(mode, this.zone);
			}else if(trip instanceof TTAnalysisTripI){
				temp = new TTAnalysisTripSet(mode, this.zone, this.storeTrips);
			}else{
				log.error("could not define a tripSet");
				temp = null;
			}
			temp.addTrip(trip);
			this.mode2TripSet.put(mode, temp);
		}
	}
	
	public Map<String, AbstractAnalysisTripSet> getTripSets(){
		return this.mode2TripSet;
	}
}
