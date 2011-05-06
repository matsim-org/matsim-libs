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
package playground.droeder.Analysis.Trips.V2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;

import playground.droeder.Analysis.Trips.AnalysisTripSetAllMode;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class TripEventsHandlerV2 implements AgentDepartureEventHandler, AgentArrivalEventHandler,
									ActivityEndEventHandler, ActivityStartEventHandler, 
									AgentStuckEventHandler{
	
	private static final Logger log = Logger
			.getLogger(TripEventsHandlerV2.class);

	private Map<Id, LinkedList<AnalysisTripV2>> id2Trips = null;
	private Map<Id, ArrayList<PersonEvent>> id2Events = null;
	private Map<String, AnalysisTripSetAllMode> zone2tripSet;
	
	private int nrOfprocessedTrips = 0;
	private Map<Id, int[]> nrOfTrips = new HashMap<Id, int[]>();
	private boolean stuck = false;
	
	public TripEventsHandlerV2(){
		this.id2Events = new HashMap<Id, ArrayList<PersonEvent>>();
		this.zone2tripSet = new HashMap<String, AnalysisTripSetAllMode>();
		this.zone2tripSet.put("noZone", new AnalysisTripSetAllMode(false, null));
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(AgentStuckEvent e) {
		if(!stuck){
			log.warn("Found StuckEvent for Agent " + e.getPersonId() + "! Probably not all Trips from PlansFile are processed! Message thrown only once");
			stuck = true;
		}
	}
	
	@Override
	public void handleEvent(ActivityStartEvent e) {
		this.processEvent(e);
	}

	@Override
	public void handleEvent(ActivityEndEvent e) {
		this.processEvent(e);
	}

	@Override
	public void handleEvent(AgentArrivalEvent e) {
		this.processEvent(e);
	}

	@Override
	public void handleEvent(AgentDepartureEvent e) {
		this.processEvent(e);
	}
	
	private void processEvent(PersonEvent e){
		if(this.id2Events.containsKey(e.getPersonId())){
			this.id2Events.get(e.getPersonId()).add(e);
			if(((this.id2Trips.get(e.getPersonId()).getFirst().getNrOfElements() * 2 ) - 2) == this.id2Events.get(e.getPersonId()).size()){
				this.addEvents2Trip(e.getPersonId());
			}
		}
	}
	
	private void addEvents2Trip(Id id){
		this.nrOfprocessedTrips++;
		this.nrOfTrips.get(id)[1]++;
		AnalysisTripV2 trip = this.id2Trips.get(id).removeFirst();
		trip.addEvents(this.id2Events.get(id));
		for(String s : this.zone2tripSet.keySet()){
			this.zone2tripSet.get(s).addTrip(trip);
		}
		this.id2Events.put(id, new ArrayList<PersonEvent>());
	}
	
	

	/**
	 * @param trips
	 */
	public void addTrips(Map<Id, LinkedList<AnalysisTripV2>> trips) {
		this.id2Trips = trips;
		this.init();
	}
	
	public void addZones(Map<String, Geometry> zones){
		this.zone2tripSet = new HashMap<String, AnalysisTripSetAllMode>();
		for(Entry<String, Geometry> e : zones.entrySet()){
			this.zone2tripSet.put(e.getKey(), new AnalysisTripSetAllMode(false, e.getValue()));
		}
	}
	
	private void init(){
		for(Id id : this.id2Trips.keySet()){
			this.nrOfTrips.put(id, new int[2]);
			this.nrOfTrips.get(id)[0] = this.id2Trips.get(id).size();
			this.id2Events.put(id, new ArrayList<PersonEvent>());
		}
	}
	
	public Map<String, AnalysisTripSetAllMode> getZone2Tripset(){
		int j = 0;
		StringBuffer b = new StringBuffer();
		b.append("check following agents, because nr of PlanElements and Events does not match!");
		for(Entry<Id, int[]> i : this.nrOfTrips.entrySet()){
			j+= i.getValue()[0];
			if(i.getValue()[0] != i.getValue()[1]){
				b.append(i.getKey().toString() + "\t");
			}
		}
		log.info(this.nrOfprocessedTrips + " of " + j + " trips from plansfile are processed");
		log.warn(b.toString());
		return this.zone2tripSet;
	}
}
