/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.congestion;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.events.CongestionEventsReader;
import playground.vsp.congestion.handlers.CongestionEventHandler;

/**
 * Calculated the delay caused by reading the marginal congestion events
 * @author amit
 */

public class CausedDelayAnalyzer {

	
	public CausedDelayAnalyzer(String eventsFile ) {
		this.eventsFile = eventsFile;
	}
	
	private CongestionEventsReader reader;
	private CausedDelayHandler handler = new CausedDelayHandler();
	private String eventsFile;
	
	public void run(){
		EventsManager eventsManager = EventsUtils.createEventsManager();
		reader = new CongestionEventsReader(eventsManager);
		
		eventsManager.addHandler(handler);
		reader.parse(this.eventsFile);
	}
	
	public Map<Id<Person>, Double> getPersonId2DelayCaused() {
		return handler.getPersonId2DelayCaused();
	}
	

	public class CausedDelayHandler implements CongestionEventHandler {
		private Map<Id<Person>, Double> personId2DelayCaused = new HashMap<Id<Person>, Double>();
		
		@Override
		public void reset(int iteration) {

			this.personId2DelayCaused.clear();
		}

		@Override
		public void handleEvent(CongestionEvent event) {
		
			Id<Person> causingAgentId = event.getCausingAgentId();
			if (personId2DelayCaused.containsKey(causingAgentId)){
				double delaySoFar = personId2DelayCaused.get(causingAgentId);
				personId2DelayCaused.put(causingAgentId, delaySoFar+event.getDelay());
			} else {
				personId2DelayCaused.put(causingAgentId, event.getDelay());
			}
		}

		public Map<Id<Person>, Double> getPersonId2DelayCaused() {
			return personId2DelayCaused;
		}
	
	}
	
}
