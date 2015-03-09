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
package org.matsim.contrib.matsim4urbansim.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

/**
 * @author droeder
 *
 */
public class CalcMacroZoneTravelTimes implements PersonDepartureEventHandler,
												PersonArrivalEventHandler,
												ActivityStartEventHandler,
												ActivityEndEventHandler{

	private static final Logger log = Logger
			.getLogger(CalcMacroZoneTravelTimes.class);
	private HashMap<Id, LegStore> legStore;
	private Map<Id<ActivityFacility>, ? extends ActivityFacility> allFacilities;
	private Map<Id<ActivityFacility>, Id<ActivityFacility>> micro2MacroZone;
	private Map<String, Matrix> mode2zoneTraveltimes;
	private Map<String, Matrix> mode2zoneTrips;
	private int end;
	private int start; 

	/**
	 * Generates some output for validation reasons for the brussels-scenario. Note it might NOT work for other scenarios...
	 * @param map
	 * @param micro2MacroZone
	 */
	public CalcMacroZoneTravelTimes(Map<Id<ActivityFacility>, ? extends ActivityFacility> map, Map<Id<ActivityFacility>, Id<ActivityFacility>> micro2MacroZone, int startHour, int endHour) {
		this.legStore = new HashMap<Id, LegStore>();
		this.allFacilities = map;
		this.micro2MacroZone = micro2MacroZone;
		this.mode2zoneTraveltimes = new HashMap<String, Matrix>();
		this.mode2zoneTrips = new HashMap<String, Matrix>();
		this.start = startHour;
		this.end = endHour;
	}

	@Override
	public void reset(int iteration) {

	}

	/**
	 * @param outputPath
	 */
	public void writeOutput(String outputPath) {
		//write traveltimes
		for(java.util.Map.Entry<String, Matrix> e: this.mode2zoneTraveltimes.entrySet()){
			BufferedWriter writer = IOUtils.getBufferedWriter(outputPath + "/travelTimesAndTrips_" + String.valueOf(start) + "-" + String.valueOf(end) + "_"+ e.getKey() + ".csv");
			
			try {
				writer.write("fromZone;toZone;totalTravelTime[s];Trips[#]; avgTravelTimes[s];");
				writer.newLine();
				for(ArrayList<Entry> ee:  e.getValue().getFromLocations().values()){
					for(Entry eee: ee){
						writer.write(eee.getFromLocation() + ";" + eee.getToLocation() + ";");
						writer.write(eee.getValue() + ";");
						writer.write(this.mode2zoneTrips.get(e.getKey()).getEntry(eee.getFromLocation(), eee.getToLocation()).getValue() + ";");
						writer.write(eee.getValue()/this.mode2zoneTrips.get(e.getKey()).getEntry(eee.getFromLocation(), eee.getToLocation()).getValue() + ";");
						writer.newLine();
					}
				}
				writer.flush();
				writer.close();
				log.info(outputPath + "/travelTimesAndTrips_" + e.getKey() + ".csv written...");
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		//write modeshares
//		BufferedWriter writer = IOUtils.getBufferedWriter(outputPath + "/modeshares.csv");
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// handle only trips that start between 6 and 10 am...
		if((event.getTime() < (this.start*3600)) || (event.getTime() >= (this.end * 3600))) return;
		LegStore ls = new LegStore();
		ActivityFacility fac = allFacilities.get(event.getFacilityId());
		ls.setFromFacility(((Id) fac.getCustomAttributes().get(InternalConstants.ZONE_ID)));
		this.legStore.put(event.getPersonId(), ls);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// check if agents exists...
		if(!this.legStore.containsKey(event.getPersonId())) return;
		this.legStore.get(event.getPersonId()).setDepTime(event.getTime());
		this.legStore.get(event.getPersonId()).setMode(event.getLegMode());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// check if agents exists...
		if(!this.legStore.containsKey(event.getPersonId())) return;
		this.legStore.get(event.getPersonId()).setArrTime(event.getTime());
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		// check if agents exists...
		if(!this.legStore.containsKey(event.getPersonId())) return;
		LegStore ls = this.legStore.remove(event.getPersonId());
		ActivityFacility fac = allFacilities.get(event.getFacilityId());
		ls.setToFacility(((Id) fac.getCustomAttributes().get(InternalConstants.ZONE_ID)));
		computeLeg(ls);
	}

	/**
	 * @param ls
	 */
	private void computeLeg(LegStore ls) {
		Matrix tt, trip;
		// get the correct tt-matrix or create a new one if necessary
		tt = this.mode2zoneTraveltimes.get(ls.getMode());
		if(tt == null){
			tt = new Matrix(ls.getMode()+ "_traveltimes", ls.getMode() + " traveltimes");
			init(tt);
			this.mode2zoneTraveltimes.put(ls.getMode(), tt);
		}
		// same for the trip-matrix
		trip = this.mode2zoneTrips.get(ls.getMode());
		if(trip == null){
			trip = new Matrix(ls.getMode()+ "_trips", ls.getMode() + " trips");
			init(trip);
			this.mode2zoneTrips.put(ls.getMode(), trip);
		}
		String from, to;
		// get the macrozone-ids... maybe doublecheck here, if the id exists...
		from = this.micro2MacroZone.get(ls.getFromFac()).toString();
		to = this.micro2MacroZone.get(ls.getToFac()).toString();
		// update the trip-matrix-value
		Entry ee = trip.getEntry(from, to);
		if(ee == null) ee = trip.setEntry(from, to, 0);
		trip.setEntry(from, to, ee.getValue() + 1);
		// update the the tt-matrix-value
		ee = tt.getEntry(from, to);
		if(ee == null) ee = tt.setEntry(from, to, 0);
		tt.setEntry(from, to, ee.getValue() + ls.getTravelTime());
	}

	/**
	 * initialize the matrix to get it sorted in a proper way...
	 * @param matrix
	 */
	private void init(Matrix matrix) {
		for(int i = 1; i < 8; i++){
			for(int j = 1; j < 8; j++){
				matrix.createEntry(Integer.toString(i), Integer.toString(j), 0.);
			}
		}
	}
	
	
}
class LegStore{
	private Id<ActivityFacility> fromFac;
	private Double depTime;
	private Double arrTime;
	private Id<ActivityFacility> toFacility;
	private String mode;

	public LegStore(){
		
	}
	
	public void setFromFacility(Id<ActivityFacility> fromFac){
		this.fromFac = fromFac;
	}
	
	public void setDepTime(Double time){
		this.depTime = time;
	}
	
	public void setArrTime(Double time){
		this.arrTime = time;
	}
	
	public void setToFacility(Id<ActivityFacility> toFac){
		this.toFacility = toFac;
	}
	
	public void setMode(String mode){
		this.mode = mode;
	}
	
	public Double getTravelTime(){
		return (this.arrTime - this.depTime);
	}
	
	public Id getToFac(){
		return this.toFacility;
	}
	
	public Id getFromFac(){
		return this.fromFac;
	}
	
	public String getMode(){
		return this.mode;
	}
}
