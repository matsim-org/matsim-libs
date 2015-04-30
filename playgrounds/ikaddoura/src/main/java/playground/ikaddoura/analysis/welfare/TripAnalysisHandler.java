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

/**
 * 
 */
package playground.ikaddoura.analysis.welfare;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * @author ikaddoura
 *
 */
public class TripAnalysisHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler{
	
	private final static Logger log = Logger.getLogger(TripAnalysisHandler.class);

	private Map<Id<Person>, Double> personId2departureTime = new HashMap<Id<Person>, Double>();
	
	private double totalTravelTimeAllModes = 0.;
	private double totalTravelTimeCarMode = 0.;
	private int agentStuckEvents = 0;
	private int carLegs = 0;
	private int ptLegs = 0;
	private int walkLegs = 0;

	private Set<Id<Person>> stuckingAgents;
	
	public TripAnalysisHandler(Set<Id<Person>> invalidPersonIDs) {
		this.stuckingAgents = invalidPersonIDs;
		log.info("Providing the person Ids of stucking agents. These agents will be excluded from this analysis.");
	}

	public TripAnalysisHandler() {
		stuckingAgents = null;
		log.info("Considering all persons even though they may stuck in the base case or policy case.");
	}

	@Override
	public void reset(int iteration) {
		this.personId2departureTime.clear();
		this.totalTravelTimeAllModes = 0.;
		this.totalTravelTimeCarMode = 0.;
		this.agentStuckEvents = 0;
		this.carLegs = 0;
		this.ptLegs = 0;
		this.walkLegs = 0;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
				
		if (isStucking(event.getPersonId())) {
			// ignore this person
			
		} else {
			double travelTime = event.getTime() - this.personId2departureTime.get(event.getPersonId());
			
			totalTravelTimeAllModes = totalTravelTimeAllModes + travelTime;
			
			if (event.getLegMode().toString().equals(TransportMode.car)) {
				totalTravelTimeCarMode = totalTravelTimeCarMode + travelTime;
				this.carLegs++;
				
			} else if (event.getLegMode().toString().equals(TransportMode.pt)) {
				this.ptLegs++;
		
			} else if (event.getLegMode().toString().equals(TransportMode.walk)) {
				this.walkLegs++;
			
			} else if (event.getLegMode().toString().equals(TransportMode.transit_walk)) {
				log.warn("For the simulated public transport, e.g. 'transit_walk' this analysis has to be revised.");
			
			} else {
				// ...
			}
		}
	
	}

	private boolean isStucking(Id<Person> personId) {
		if (this.stuckingAgents == null) {
			return false;
		} else if (this.stuckingAgents.contains(personId)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (isStucking(event.getPersonId())) {
			// ignore this person
			
		} else {
			this.personId2departureTime.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		agentStuckEvents++;
	}

	public double getTotalTravelTimeAllModes() {
		return totalTravelTimeAllModes;
	}

	public double getTotalTravelTimeCarMode() {
		return totalTravelTimeCarMode;
	}
	
	public int getAgentStuckEvents() {
		return agentStuckEvents;
	}

	public int getCarLegs() {
		return carLegs;
	}

	public int getPtLegs() {
		return ptLegs;
	}

	public int getWalkLegs() {
		return walkLegs;
	}
	
}
