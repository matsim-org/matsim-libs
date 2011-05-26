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
package playground.droeder.Analysis.Trips.distance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;

import playground.droeder.Analysis.Trips.AbstractAnalysisTrip;
import playground.droeder.Analysis.Trips.AnalysisTripSetStorage;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class DistAnalysisHandler implements LinkEnterEventHandler, TransitDriverStartsEventHandler,
												PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
												AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler{
	
	private static final Logger log = Logger
			.getLogger(DistAnalysisHandler.class);
	
	private Map<Id, DistAnalysisAgent> persons;
	private Map<Id, DistAnalysisPtDriver> drivers;
	private Map<Id, DistAnalysisTransitRoute> routes;
	private Map<Id, DistAnalysisVehicle> vehicles;
	private List<Id> stuckAgents;
	private Map<String, AnalysisTripSetStorage> tripSets;
	
	private Map<Id, Link> links;
	
	public DistAnalysisHandler(){
		this.persons = new HashMap<Id, DistAnalysisAgent>();
		this.drivers = new HashMap<Id, DistAnalysisPtDriver>();
		this.routes = new HashMap<Id, DistAnalysisTransitRoute>();
		this.vehicles = new HashMap<Id, DistAnalysisVehicle>();
		this.tripSets = new HashMap<String, AnalysisTripSetStorage>();
		this.tripSets.put("noZone", new AnalysisTripSetStorage(false, null));
		this.stuckAgents = new ArrayList<Id>();
	}
	
	
	public void addLinks(Map<Id, Link> map){
		this.links = map;
	}
	public void addZones(Map<String, Geometry> zones){
		this.tripSets = new HashMap<String, AnalysisTripSetStorage>();
		for(Entry<String, Geometry> e : zones.entrySet()){
			this.tripSets.put(e.getKey(), new AnalysisTripSetStorage(false, e.getValue()));
		}
	}
	
	public void addPerson(DistAnalysisAgent person){
		this.persons.put(person.getId(), person);
	}

	@Override
	public void reset(int iteration) {
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent e) {
		if(!this.drivers.containsKey(e.getDriverId())){
			this.drivers.put(e.getDriverId(), new DistAnalysisPtDriver(e.getDriverId()));
		}
		if(!this.vehicles.containsKey(e.getVehicleId())){
			this.vehicles.put(e.getVehicleId(), new DistAnalysisVehicle(e.getVehicleId()));
		}
		if(!this.routes.containsKey(e.getTransitRouteId())){
			this.routes.put(e.getTransitRouteId(), new DistAnalysisTransitRoute(e.getTransitRouteId()));
		}
		
		this.drivers.get(e.getDriverId()).registerVehicle(this.vehicles.get(e.getVehicleId()));
		this.vehicles.get(e.getVehicleId()).registerRoute(this.routes.get(e.getTransitRouteId()));
	}

	@Override
	public void handleEvent(AgentDepartureEvent e) {
		if(this.persons.containsKey(e.getPersonId())){
			if(this.persons.get(e.getPersonId()).processAgentEvent(e)){
				this.addTrip2TripSetsAndRemoveFromPerson(e.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent e) {
		if(this.persons.containsKey(e.getPersonId())){
			if(this.persons.get(e.getPersonId()).processAgentEvent(e)){
				this.addTrip2TripSetsAndRemoveFromPerson(e.getPersonId());
			}
		}
	}
	
	private void addTrip2TripSetsAndRemoveFromPerson(Id id){
		AbstractAnalysisTrip t = this.persons.get(id).removeFinishedTrip();
		for(AnalysisTripSetStorage s: this.tripSets.values()){
			s.addTrip(t);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent e) {
		if(this.persons.containsKey(e.getPersonId())){
			if(!this.vehicles.get(e.getVehicleId()).leaveVehicle(this.persons.get(e.getPersonId()))){
				log.error("agent " + e.getPersonId() + " try to leave vehicle " + e.getVehicleId() + " but isn't in there");
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent e) {
		if(this.persons.containsKey(e.getPersonId())){
			this.vehicles.get(e.getVehicleId()).enterVehicle(this.persons.get(e.getPersonId()));
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent e) {
		if(this.persons.containsKey(e.getPersonId())){
			this.persons.get(e.getPersonId()).processLinkEnterEvent(this.links.get(e.getLinkId()).getLength());
		}else if(this.drivers.containsKey(e.getPersonId())){
			this.drivers.get(e.getPersonId()).processLinkEnterEvent(this.links.get(e.getLinkId()).getLength());
		}
	}
	
	
	private boolean stuck = false;
	@Override
	public void handleEvent(AgentStuckEvent e) {
		if(!stuck){
			this.stuck = true;
			log.error("Message thrown only once!!! StuckEvent for Agent: " + e.getPersonId());
		}
		this.stuckAgents.add(e.getPersonId());
		this.persons.remove(e.getPersonId());
	}

	public Map<String, AnalysisTripSetStorage> getAnalysisTripSetStorage(){
		return this.tripSets;
	}
	
	public Collection<DistAnalysisTransitRoute> getRoutes(){
		return this.routes.values();
	}
	
	public Collection<DistAnalysisVehicle> getVehicles(){
		return this.vehicles.values();
	}
	
	public List<Id> getStuckAgents(){
		return this.stuckAgents;
	}
}
