/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.ptCircuityAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.vehicles.Vehicles;

/**
 * 
 * @author gleich
 *
 */
public class PtDistanceHandler implements ActivityStartEventHandler,
	PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, 
	LinkEnterEventHandler, VehicleArrivesAtFacilityEventHandler,
	TransitDriverStartsEventHandler, PersonDepartureEventHandler {
	
	//TODO: Find Coord for each vehicle before first departure:
	/*
	 * 1. Transit Driver Starts
	 * 2. Person enters Vehicle -> transit driver only, pt stop coords unknown
	 * 3. Vehicle arrives at facility -> pt stop coords known
	 * 4. Person enters vehicle -> passengers can board
	 * 5. Vehicle departs at facility
	 * -> Exclude Transit Driver
	 */
	
	private Scenario scenario;
	/* <Id: Vehicle id, <Double: time of arrival at facility (= stop), Double: distance from last to current stop */
	private Map<Id, SortedMap<Double, Double>> distancePerArrivalAndVehicle =
			new HashMap<Id, SortedMap<Double, Double>>();
	private Map<Id, Double> distanceSinceLastDeparture = new HashMap<Id, Double>(); // distance travelled by the pt vehicle since the last stop
	private Map<Id, Coord> currentPtFacilityCoord = new HashMap<Id, Coord>();
	private Map<Id, Coord> lastTransitWalkBeginCoord = new HashMap<Id, Coord>();
	private Map<Id, Double> lastVehicleEnterEvent = new HashMap<Id, Double>();
	private Map<Id, Double> lastActivityToActivityDistance = new HashMap<Id, Double>();
	private Map<Id, List<Double>> ptDistancesBetweenActivities = new HashMap<Id, List<Double>>(); //including transitWalk to, from and between pt stops
	private Map<Id, List<Double>> transitWalkDistancesBetweenActivities = new HashMap<Id, List<Double>>(); // transit walk only
	private Set<Id> transitDrivers = new HashSet<Id>();
	private Map<Id, Queue<Coord>> actCoords;
	private Map<Id, String> legMode = new HashMap<Id, String>();
	
	public PtDistanceHandler(Scenario scenario, Vehicles vehicles, Map<Id, Queue<Coord>> actCoords) {
		this.scenario = scenario;
		this.actCoords = actCoords;
		/* 
		 * Initialize lastTransitWalkBeginCoord with first Activity Coord and
		 * initialize lastActivityToActivityDistance with 0 
		 */
		for(Id id: actCoords.keySet()) {
			lastTransitWalkBeginCoord.put(id, actCoords.get(id).remove());
			lastActivityToActivityDistance.put(id, 0.0);
		}
		/* Initialize Maps with pt vehicle Ids */
		for(Id id: vehicles.getVehicles().keySet()) {
			distancePerArrivalAndVehicle.put(id, new TreeMap<Double, Double>());
			distanceSinceLastDeparture.put(id, 0.0);
		}
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	/*
	 * Trip distance calculation:
	 *  Bus stops are situated at the end of a link 
	 *  -> get links from LinkEnterEvents 
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		/* Ensure, that only pt vehicles are analyzed */
		if(distancePerArrivalAndVehicle.containsKey(event.getVehicleId())) {
			double length = scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			distanceSinceLastDeparture.put(
					event.getVehicleId(), distanceSinceLastDeparture.get(event.getVehicleId()) + length);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		/* Exclude transit Drivers and all modes except transit walk and pt */
		if((!transitDrivers.contains(event.getPersonId())) && 
				(legMode.get(event.getPersonId()).equalsIgnoreCase("pt") || legMode.get(event.getPersonId()).equalsIgnoreCase("transit_walk"))) {
			lastTransitWalkBeginCoord.put(event.getPersonId(), currentPtFacilityCoord.get(event.getVehicleId()));

			double distance = lastActivityToActivityDistance.get(event.getPersonId());
			/* Add all stop to stop distances travelled inside the pt vehicle between boarding and alighting time */
			for(Double d: distancePerArrivalAndVehicle.get(event.getVehicleId()).
					subMap(lastVehicleEnterEvent.get(event.getPersonId()), event.getTime()).values()) {
				distance += d;
			}
			lastActivityToActivityDistance.put(event.getPersonId(), distance);
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		/* Exclude transit Drivers and all modes except transit walk and pt */
		if((!transitDrivers.contains(event.getPersonId())) && 
				(legMode.get(event.getPersonId()).equalsIgnoreCase("pt") || legMode.get(event.getPersonId()).equalsIgnoreCase("transit_walk"))) {
			/* Calculate transit_walk distance */
			Double distance = lastActivityToActivityDistance.get(event.getPersonId());
			Coord startCoord = lastTransitWalkBeginCoord.get(event.getPersonId());
			Coord endCoord = currentPtFacilityCoord.get(event.getVehicleId());
			distance += Math.sqrt(
					Math.pow((endCoord.getX() - startCoord.getX()), 2)
					+ Math.pow((endCoord.getY() - startCoord.getY()), 2));
			lastActivityToActivityDistance.put(event.getPersonId(), distance);

			lastVehicleEnterEvent.put(event.getPersonId(), event.getTime());
		}
	}

	/* Adds the distance travelled since the last ActivityStartEvent to the distances list */
	@Override
	public void handleEvent(ActivityStartEvent event) {
			/* Exclude "pt interaction" pseudo-activities */
			if(event.getActType().equalsIgnoreCase("pt interaction")) {
				return;
			} else {
				/* Get Distance travelled until arrival at the last pt stop */
				Double distance = lastActivityToActivityDistance.get(event.getPersonId());
				/* Calculate transit walk distance from the last pt stop */
				Coord startCoord = lastTransitWalkBeginCoord.get(event.getPersonId());
				Coord endCoord = actCoords.get(event.getPersonId()).remove();
				lastTransitWalkBeginCoord.put(event.getPersonId(), endCoord);
				distance += Math.sqrt(
				Math.pow((endCoord.getX() - startCoord.getX()), 2)
				+ Math.pow((endCoord.getY() - startCoord.getY()), 2));
				if(legMode.get(event.getPersonId()).equalsIgnoreCase("pt")) {
					if(!ptDistancesBetweenActivities.containsKey(event.getPersonId())){
						ptDistancesBetweenActivities.put(event.getPersonId(), new ArrayList<Double>());
					}
					ptDistancesBetweenActivities.get(event.getPersonId()).add(distance);
				} else if(legMode.get(event.getPersonId()).equalsIgnoreCase("transit_walk")) {
					if(!transitWalkDistancesBetweenActivities.containsKey(event.getPersonId())){
						transitWalkDistancesBetweenActivities.put(event.getPersonId(), new ArrayList<Double>());
					}
					transitWalkDistancesBetweenActivities.get(event.getPersonId()).add(distance);
				}

			}
		lastActivityToActivityDistance.put(event.getPersonId(), 0.0);
		
		// AN Had to replace this line
		legMode.remove(event.getPersonId());
//		legMode.put(event.getDriverId(), null);
		// END of replacement
	}
	
	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		currentPtFacilityCoord.put(event.getVehicleId(), scenario.getTransitSchedule().getFacilities().get(event.getFacilityId()).getCoord());
		distancePerArrivalAndVehicle.get(event.getVehicleId()).put(
				event.getTime(), distanceSinceLastDeparture.get(event.getVehicleId()));
		distanceSinceLastDeparture.put(event.getVehicleId(), 0.0); //reset for the leg to the next stop
		
	}
	
	public Map<Id, List<Double>> getPtDistances() {
		return ptDistancesBetweenActivities;
	}
	
	public Map<Id, List<Double>> getTransitWalkDistances() {
		return transitWalkDistancesBetweenActivities;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		transitDrivers.add(event.getDriverId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(!transitDrivers.contains(event.getPersonId())) {
			if(legMode.containsKey(event.getPersonId())) {
				// ignore changes from pt to transit walk in order to separate transit_walk only legs
				if(legMode.get(event.getPersonId()).equalsIgnoreCase("pt")) {
					return;
				}
			} 
			legMode.put(event.getPersonId(), event.getLegMode());
		}
	}
	
	

}
