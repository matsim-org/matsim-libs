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
package playground.vsp.analysis.modules.ptTripAnalysis.traveltime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;

import playground.vsp.analysis.modules.ptTripAnalysis.AbstractAnalysisTrip;
import playground.vsp.analysis.modules.ptTripAnalysis.AnalysisTripSetStorage;

/**
 * @author droeder
 *
 */
public class TTtripEventsHandler  implements PersonDepartureEventHandler, 
										PersonArrivalEventHandler, ActivityEndEventHandler, 
										ActivityStartEventHandler, PersonStuckEventHandler, 
										PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private static final Logger log = Logger
			.getLogger(TTtripEventsHandler.class);
	
	protected Map<Id, LinkedList<AbstractAnalysisTrip>> id2Trips = null;
	protected Map<Id, ArrayList<Event>> id2Events = null;
	protected Map<String, AnalysisTripSetStorage> zone2tripSet;
	private List<Id> stuckAgents;
	
	protected int nrOfprocessedTrips = 0;
	protected Map<Id, int[]> nrOfTrips = null;
	private int possibleTrips = 0;
	private boolean stuck = false;

	private Collection<String> ptModes;
	
	/**
	 * @param ptModes
	 */
	public TTtripEventsHandler(Collection<String> ptModes) {
		this.id2Events = new HashMap<Id, ArrayList<Event>>();
		this.zone2tripSet = new HashMap<String, AnalysisTripSetStorage>();
		this.zone2tripSet.put("noZone", new AnalysisTripSetStorage(false, null, ptModes));
		this.stuckAgents = new ArrayList<Id>();
		this.ptModes = ptModes;
	}
	
	@Override
	public void reset(int iteration) {
		
	}
	
	@Override
	public void handleEvent(PersonStuckEvent e) {
		if(!stuck){
			log.warn("Found StuckEvent for Agent " + e.getPersonId() + "! Probably not all Trips from PlansFile are processed! Message thrown only once");
			stuck = true;
		}
		this.stuckAgents.add(e.getPersonId());
	}

	@Override
	public void handleEvent(ActivityStartEvent e) {
		if(this.id2Trips.containsKey(e.getPersonId())){
			if(((TTAnalysisTrip) this.id2Trips.get(e.getPersonId()).getFirst()).handleEvent(e)){
				this.addTrip2TripSet(e.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent e) {
		if(this.id2Trips.containsKey(e.getPersonId())){
			if(((TTAnalysisTrip) this.id2Trips.get(e.getPersonId()).getFirst()).handleEvent(e)){
				this.addTrip2TripSet(e.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent e) {
		if(this.id2Trips.containsKey(e.getPersonId())){
			if(((TTAnalysisTrip) this.id2Trips.get(e.getPersonId()).getFirst()).handleEvent(e)){
				this.addTrip2TripSet(e.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent e) {
		if(this.id2Trips.containsKey(e.getPersonId())){
			if(((TTAnalysisTrip) this.id2Trips.get(e.getPersonId()).getFirst()).handleEvent(e)){
				this.addTrip2TripSet(e.getPersonId());
			}
		}
	}
	
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent e) {
		if(this.id2Trips.containsKey(e.getPersonId())){
			if(((TTAnalysisTrip) this.id2Trips.get(e.getPersonId()).getFirst()).handleEvent(e)){
				this.addTrip2TripSet(e.getPersonId());
			}
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent e) {
		if(this.id2Trips.containsKey(e.getPersonId())){
			if(((TTAnalysisTrip) this.id2Trips.get(e.getPersonId()).getFirst()).handleEvent(e)){
				this.addTrip2TripSet(e.getPersonId());
			}
		}
	}
	
	private void addTrip2TripSet(Id id){
		// store number of processed Trips
		this.nrOfprocessedTrips++;
		
		// store this for getUncompletedPlans()
		this.nrOfTrips.get(id)[1]++;
		
		//get and remove the first Trip of this agent
		TTAnalysisTrip trip = (TTAnalysisTrip ) this.id2Trips.get(id).removeFirst();
		
		//add for all zones
		for(String s : this.zone2tripSet.keySet()){
			this.zone2tripSet.get(s).addTrip(trip);
		}
	}
	
	public void addTrips(Map<Id, LinkedList<AbstractAnalysisTrip>> map) {
		this.id2Trips = map;
		this.init();
		this.id2Events = null;
	}

	
	public void addZones(Map<String, Geometry> zones){
		this.zone2tripSet = new HashMap<String, AnalysisTripSetStorage>();
		for(Entry<String, Geometry> e : zones.entrySet()){
			this.zone2tripSet.put(e.getKey(), new AnalysisTripSetStorage(false, e.getValue(), this.ptModes));
		}
	}
	
	//init fields
	private void init(){
		this.nrOfTrips = new HashMap<Id, int[]>();
		int i = 0;
		for(Id id : this.id2Trips.keySet()){
			this.nrOfTrips.put(id, new int[2]);
			i = this.id2Trips.get(id).size();
			this.possibleTrips += i;
			this.nrOfTrips.get(id)[0] = i;
			this.id2Events.put(id, new ArrayList<Event>());
		}
	}
	
	/**
	 * returns a csv-<code>String</code> of all agents with a not corresponding number of PlanElements and Events
	 * @return
	 */
	public String getUncompletedPlans(){
		StringBuffer b = new StringBuffer();
		b.append("check following agents, because nr of PlanElements and Events does not match, so not all trips are processed\n" );
		b.append("id;possible Trips; processed Trips \n");
		for(Entry<Id, int[]> i : this.nrOfTrips.entrySet()){
			if(i.getValue()[0] != i.getValue()[1]){
				b.append(i.getKey().toString() + ";" + i.getValue()[0] + ";" + i.getValue()[1] + "\n");
			}
		}
		return b.toString();
	}
	
	/**
	 * returns all agents got an StuckEvent in a csv-String
	 * @return
	 */
	public String getStuckAgents(){
		StringBuffer b = new StringBuffer();
		for(Id id: this.stuckAgents){
			b.append(id.toString() + "\n");
		}
		return b.toString();
	}
	
	/**
	 * returns all produced <code>AnalysisTripSetAllMode</code>-objects, separated by zones
	 * @return
	 */
	public Map<String, AnalysisTripSetStorage> getZone2Tripset(){
		log.info(this.nrOfprocessedTrips + " of " + this.possibleTrips + " trips from plansfile are processed");
		return this.zone2tripSet;
	}
	
}


