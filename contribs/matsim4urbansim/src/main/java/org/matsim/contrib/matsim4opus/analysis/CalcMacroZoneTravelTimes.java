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
package org.matsim.contrib.matsim4opus.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

/**
 * @author droeder
 *
 */
public class CalcMacroZoneTravelTimes implements AgentDepartureEventHandler,
												AgentArrivalEventHandler,
												ActivityStartEventHandler,
												ActivityEndEventHandler{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(CalcMacroZoneTravelTimes.class);
	private HashMap<Id, LegStore> legStore;
	private Map<Id, ? extends ActivityFacility> allFacilities;
	private Map<Id, Id> micro2MacroZone;
	private Map<String, Matrix> mode2zoneTraveltimes;
	private Map<String, Matrix> mode2zoneTrips; 

	public CalcMacroZoneTravelTimes(Map<Id, ? extends ActivityFacility> map, Map<Id, Id> micro2MacroZone) {
		this.legStore = new HashMap<Id, LegStore>();
		this.allFacilities = map;
		this.micro2MacroZone = micro2MacroZone;
		this.mode2zoneTraveltimes = new HashMap<String, Matrix>();
		this.mode2zoneTrips = new HashMap<String, Matrix>();
	}

	@Override
	public void reset(int iteration) {
		// TODO[dr] Auto-generated method stub

	}

	/**
	 * @param outputPath
	 */
	public void writeOutput(String outputPath) {
		//write traveltimes
		for(java.util.Map.Entry<String, Matrix> e: this.mode2zoneTraveltimes.entrySet()){
			BufferedWriter writer = IOUtils.getBufferedWriter(outputPath + "/travelTimesAndTrips_" + e.getKey() + ".csv");
			
			try {
				writer.write("fromZone;toZone;totalTravelTime[s];Trips[#]; avgTravelTimes[s];");
				writer.newLine();
				for(ArrayList<Entry> ee:  e.getValue().getFromLocations().values()){
					for(Entry eee: ee){
						writer.write(eee.getFromLocation().toString() + ";" + eee.getToLocation() + ";");
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
		LegStore ls = new LegStore();
		ActivityFacility fac = allFacilities.get(event.getFacilityId());
		ls.setFromFacility(((Id) fac.getCustomAttributes().get(InternalConstants.ZONE_ID)));
		this.legStore.put(event.getPersonId(), ls);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.legStore.get(event.getPersonId()).setDepTime(event.getTime());
		this.legStore.get(event.getPersonId()).setMode(event.getLegMode());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.legStore.get(event.getPersonId()).setArrTime(event.getTime());
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
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
			this.mode2zoneTraveltimes.put(ls.getMode(), tt);
		}
		// same for the trip-matrix
		trip = this.mode2zoneTrips.get(ls.getMode());
		if(trip == null){
			trip = new Matrix(ls.getMode()+ "_trips", ls.getMode() + " trips");
			this.mode2zoneTrips.put(ls.getMode(), trip);
		}
		Id from, to;
		// get the macrozone-ids... maybe doublecheck here, if the id exists...
		from = this.micro2MacroZone.get(ls.getFromFac());
		to = this.micro2MacroZone.get(ls.getToFac());
		// update the trip-matrix-value
		Entry ee = trip.getEntry(from, to);
		if(ee == null) ee = trip.setEntry(from, to, 0);
		trip.setEntry(from, to, ee.getValue() + 1);
		// update the the tt-matrix-value
		ee = tt.getEntry(from, to);
		if(ee == null) ee = tt.setEntry(from, to, 0);
		tt.setEntry(from, to, ee.getValue() + ls.getTravelTime());
	}
	
	
}
class LegStore{
	private Id fromFac;
	private Double depTime;
	private Double arrTime;
	private Id toFacility;
	private String mode;

	public LegStore(){
		
	}
	
	public void setFromFacility(Id fromFac){
		this.fromFac = fromFac;
	}
	
	public void setDepTime(Double time){
		this.depTime = time;
	}
	
	public void setArrTime(Double time){
		this.arrTime = time;
	}
	
	public void setToFacility(Id toFac){
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
