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
package playground.droeder.Journal2MATSim2Journal;

import java.util.HashMap;
import java.util.LinkedHashSet;
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
public class TripDurationHandler implements AgentDepartureEventHandler, AgentArrivalEventHandler,
											ActivityEndEventHandler, ActivityStartEventHandler{

	Map<Id, Set<PersonEvent>> id2Event;
	public TripDurationHandler(){
		this.id2Event = new HashMap<Id, Set<PersonEvent>>();
	}
	
	@Override
	public void reset(int iteration) {
	}
	private void init(PersonEvent e){
		if(!this.id2Event.containsKey(e.getPersonId())){
			this.id2Event.put(e.getPersonId(), new LinkedHashSet<PersonEvent>());
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent e) {
		this.init(e);
		this.id2Event.get(e.getPersonId()).add(e);
	}
	
	@Override
	public void handleEvent(AgentDepartureEvent e) {
		this.init(e);
		this.id2Event.get(e.getPersonId()).add(e);
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent e) {
		this.init(e);
		this.id2Event.get(e.getPersonId()).add(e);
	}

	@Override
	public void handleEvent(ActivityStartEvent e) {
		this.init(e);
		this.id2Event.get(e.getPersonId()).add(e);
	}

	public Map<Id, Set<PersonEvent>> getId2Event(){
		return this.id2Event;
	}

}
