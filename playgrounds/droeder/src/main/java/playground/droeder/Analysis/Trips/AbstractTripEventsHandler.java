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

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public abstract class AbstractTripEventsHandler implements AgentDepartureEventHandler, 
										AgentArrivalEventHandler, ActivityEndEventHandler, 
										ActivityStartEventHandler, AgentStuckEventHandler{
	
	private static final Logger log = Logger
			.getLogger(AbstractTripEventsHandler.class);
	
	protected Map<Id, LinkedList<AbstractAnalysisTrip>> id2Trips = null;
	protected Map<Id, ArrayList<PersonEvent>> id2Events = null;
	protected Map<String, AnalysisTripSetAllMode> zone2tripSet;
	private List<Id> stuckAgents;
	
	protected int nrOfprocessedTrips = 0;
	protected Map<Id, int[]> nrOfTrips = null;
	private int possibleTrips = 0;
	private boolean stuck = false;
	
	public AbstractTripEventsHandler(){
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
	
	protected abstract void processEvent(PersonEvent e);
	
	/**
	 * @param map
	 */
	public void addTrips(Map<Id, LinkedList<AbstractAnalysisTrip>> map) {
		this.id2Trips = map;
		this.init();
	}
	
	public void addZones(Map<String, Geometry> zones){
		this.zone2tripSet = new HashMap<String, AnalysisTripSetAllMode>();
		for(Entry<String, Geometry> e : zones.entrySet()){
			this.zone2tripSet.put(e.getKey(), new AnalysisTripSetAllMode(false, e.getValue()));
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
