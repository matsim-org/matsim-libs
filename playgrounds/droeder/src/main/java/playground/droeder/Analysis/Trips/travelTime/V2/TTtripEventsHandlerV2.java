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
package playground.droeder.Analysis.Trips.travelTime.V2;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEvent;

import playground.droeder.Analysis.Trips.travelTime.AbstractTTtripEventsHandler;

/**
 * @author droeder
 *
 */
public class TTtripEventsHandlerV2 extends AbstractTTtripEventsHandler{
	
	
	public TTtripEventsHandlerV2(){
		super();
	}

	
	protected void processEvent(PersonEvent e){
		//process only if this agent has a plan in PlansFile
		if(this.id2Events.containsKey(e.getPersonId())){
			
			//add Event
			this.id2Events.get(e.getPersonId()).add(e);
			
			//if number of elements of the first trip and number of events match, add events
			if(((((TTAnalysisTripV2) this.id2Trips.get(e.getPersonId()).getFirst()).getNrOfElements() * 2 ) - 2) == this.id2Events.get(e.getPersonId()).size()){
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
		TTAnalysisTripV2 trip = (TTAnalysisTripV2) this.id2Trips.get(id).removeFirst();
		trip.addEvents(this.id2Events.get(id));
		
		//add for all zones
		for(String s : this.zone2tripSet.keySet()){
			this.zone2tripSet.get(s).addTrip(trip);
		}
		
		//put a new List for AgentEvents to store events for next trip
		this.id2Events.put(id, new ArrayList<PersonEvent>());
	}
}
