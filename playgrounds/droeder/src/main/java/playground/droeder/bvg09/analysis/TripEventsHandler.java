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
package playground.droeder.bvg09.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

/**
 * @author droeder
 *
 */
public class TripEventsHandler implements AgentDepartureEventHandler, AgentArrivalEventHandler,
										ActivityEndEventHandler, ActivityStartEventHandler{

	private Map<Id, ArrayList<PersonEvent>> events;
	
	/**
	 * @param keySet
	 */
	public TripEventsHandler(Set<Id> keySet) {
		this.initialze(keySet);
	}

	private void initialze(Set<Id> usedIds) {
		this.events = new HashMap<Id, ArrayList<PersonEvent>>();
		for(Id id: usedIds){
			this.events.put(id, new ArrayList<PersonEvent>());
		}
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(this.events.containsKey(event.getPersonId())){
			this.events.get(event.getPersonId()).add(event);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(this.events.containsKey(event.getPersonId())){
			this.events.get(event.getPersonId()).add(event);
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if(this.events.containsKey(event.getPersonId())){
			this.events.get(event.getPersonId()).add(event);
		}	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if(this.events.containsKey(event.getPersonId())){
			this.events.get(event.getPersonId()).add(event);
		}	}

	public Map<Id, ArrayList<PersonEvent>> getEvents(){
		return this.events;
	}
}
