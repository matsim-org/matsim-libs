/* *********************************************************************** *
 * project: org.matsim.*
 * InteractionHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.sim.interaction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;

/**
 * @author illenberger
 * 
 */
public class InteractionHandler implements ActivityStartEventHandler, ActivityEndEventHandler {

	private static final Logger logger = Logger.getLogger(InteractionHandler.class);
	
	private InteractionSelector selector;

	private Interactor interactor;

	private Map<Id, Map<Id, ActivityStartEvent>> facilities;

	public InteractionHandler(InteractionSelector selector, Interactor interactor) {
		this.selector = selector;
		this.interactor = interactor;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Map<Id, ActivityStartEvent> visitors = facilities.get(event.getFacilityId());
		if (visitors == null) {
			visitors = new HashMap<Id, ActivityStartEvent>();
			facilities.put(event.getFacilityId(), visitors);
		}

		Event old = visitors.put(event.getPersonId(), event); 
		if(old != null)
			logger.warn(String.format("Overwriting start event for person %1$s, time %2$s.", event.getPersonId(), event.getTime()));
	}

	@Override
	public void reset(int iteration) {
		facilities = new HashMap<Id, Map<Id, ActivityStartEvent>>();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Map<Id, ActivityStartEvent> visitors = facilities.get(event.getFacilityId());
		if (visitors != null) {
			ActivityStartEvent startEvent = visitors.remove(event.getPersonId());
			
			if (startEvent != null) {
				Collection<Id> targets = selector.select(startEvent.getPersonId(), visitors.keySet());
				for (Id person : targets) {
					double startTime = Math.max(startEvent.getTime(), visitors.get(person).getTime());
					interactor.interact(startEvent.getPersonId(), person, startTime, event.getTime(), event.getActType());
				}
			} else {
				logger.warn(String.format("No activity start event found. Facility = %1$s, Person = %2$s, Time = %3$s.", event.getFacilityId(), event.getPersonId(), event.getTime()));
			}
		} else {
			logger.warn(String.format("No facility with id %1$s found. Time = %2$s.", event.getFacilityId(), event.getTime()));
		}
	}

}
