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
package playground.droeder.Analysis.Trips.V3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;

import playground.droeder.Analysis.Trips.AnalysisTripSetAllMode;
import playground.droeder.Analysis.Trips.V2.TripEventsHandlerV2;

/**
 * @author droeder
 *
 */
public class TripEventsHandlerV3 implements AgentDepartureEventHandler, AgentArrivalEventHandler,
											ActivityEndEventHandler, ActivityStartEventHandler, 
											PersonEntersVehicleEventHandler, AgentStuckEventHandler{
	
		private static final Logger log = Logger
		.getLogger(TripEventsHandlerV2.class);
	
	private Map<Id, LinkedList<AnalysisTripV3>> id2Trips = null;
	private Map<Id, ArrayList<PersonEvent>> id2Events = null;
	private Map<String, AnalysisTripSetAllMode> zone2tripSet;
	private List<Id> stuckAgents;
	
	private int nrOfprocessedTrips = 0;
	private int possibleTrips = 0;
	private Map<Id, int[]> nrOfTrips = new HashMap<Id, int[]>();
	private boolean stuck = false;
	
	public TripEventsHandlerV3(){
		this.id2Events = new HashMap<Id, ArrayList<PersonEvent>>();
		this.zone2tripSet = new HashMap<String, AnalysisTripSetAllMode>();
		this.zone2tripSet.put("noZone", new AnalysisTripSetAllMode(false, null));
		this.stuckAgents = new ArrayList<Id>();
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
		this.stuckAgents.add(e.getPersonId());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
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
		//process only if this agent has a plan in PlansFile
		if(this.id2Events.containsKey(e.getPersonId())){
			
			//add Event
			this.id2Events.get(e.getPersonId()).add(e);
			
			//if number of elements of the first trip and number of events match, add events
			if(this.id2Trips.get(e.getPersonId()).getFirst().getNumberOfExpectedEvents() == this.id2Events.get(e.getPersonId()).size()){
				this.addEvents2Trip(e.getPersonId());
			}
		}
	}
	
	private void addEvents2Trip(Id id){
		// store number of processed Trips
		this.nrOfprocessedTrips++;
		
		// store this for getUncompletedPlans()
		this.nrOfTrips.get(id)[1]++;
		
		//get and remove the first Trip of this agent and add Events
		AnalysisTripV3 trip = this.id2Trips.get(id).removeFirst();
		trip.addEvents(this.id2Events.get(id));
		
		//add for all zones
		for(String s : this.zone2tripSet.keySet()){
			this.zone2tripSet.get(s).addTrip(trip);
		}
		
		//put a new List for AgentEvents to store events for next trip
		this.id2Events.put(id, new ArrayList<PersonEvent>());
	}
	
	//init fields
	private void init(){
		int i = 0;
		for(Id id : this.id2Trips.keySet()){
			this.nrOfTrips.put(id, new int[2]);
			i = this.id2Trips.get(id).size();
			this.possibleTrips += i;
			this.nrOfTrips.get(id)[0] = i;
			this.id2Events.put(id, new ArrayList<PersonEvent>());
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
	public Map<String, AnalysisTripSetAllMode> getZone2Tripset(){
		log.info(this.nrOfprocessedTrips + " of " + this.possibleTrips + " trips from plansfile are processed");
		return this.zone2tripSet;
	}
}

