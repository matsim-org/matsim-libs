/* *********************************************************************** *
 * project: org.matsim.*
 * FlightSimStuckedAnalysis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.BoardingDeniedEventHandler;



/**
 * @author dgrether
 *
 */
public class CollectBoardingDeniedStuckEventHandler implements AgentDepartureEventHandler, AgentStuckEventHandler, BoardingDeniedEventHandler{
	
	private static final Logger log = Logger.getLogger(CollectBoardingDeniedStuckEventHandler.class);
	
	final static class PersonEvents {
		List<BoardingDeniedEvent> boardingDeniedEvents = new ArrayList<BoardingDeniedEvent>();
		AgentStuckEvent stuckEvent = null;
	}

	
	private Map<Id, AgentDepartureEvent> agent2DepartureEventMap;
	private Map<Id, PersonEvents> personStats;
	
	public CollectBoardingDeniedStuckEventHandler(){
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.personStats = new HashMap<Id, PersonEvents>();
		this.agent2DepartureEventMap = new HashMap<Id, AgentDepartureEvent>();
	}

	
	@Override
	public void handleEvent(BoardingDeniedEvent e) {
		if (! this.personStats.containsKey(e.getPersonId())){
			this.personStats.put(e.getPersonId(), new PersonEvents());
		}
		this.personStats.get(e.getPersonId()).boardingDeniedEvents.add(e);
		
	}
	
	@Override
	public void handleEvent(AgentStuckEvent event) {
		if (event.getLegMode().compareToIgnoreCase("pt") == 0 && !event.getPersonId().toString().startsWith("pt_")){
			if (! this.personStats.containsKey(event.getPersonId())){
				this.personStats.put(event.getPersonId(), new PersonEvents());
			}
			this.personStats.get(event.getPersonId()).stuckEvent = event;
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getLegMode().compareToIgnoreCase("pt") == 0 && !event.getPersonId().toString().startsWith("pt_")){
			this.agent2DepartureEventMap.put(event.getPersonId(), event);
		}
	}
	
	public Map<Id, PersonEvents> getBoardingDeniedStuckEventsByPersonId() {
		return this.personStats;
	}
	

	




}
