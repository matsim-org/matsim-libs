/* *********************************************************************** *
 * project: org.matsim.*
 * TransportModeProvider.java
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

package org.matsim.withinday.trafficmonitoring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * Returns an agent's current transport mode or null if the agent is performing an activity. 
 * 
 * @author cdobler
 */
public class TransportModeProvider implements PersonArrivalEventHandler, PersonDepartureEventHandler, PersonStuckEventHandler {

	private final Map<Id<Person>, String> transportModes = new ConcurrentHashMap<Id<Person>, String>();
	
	public String getTransportMode(Id<Person> agentId) {
		return this.transportModes.get(agentId);
	}
	
	@Override
	public void reset(int iteration) {
		this.transportModes.clear();
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.transportModes.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.transportModes.put(event.getPersonId(), event.getLegMode());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.transportModes.remove(event.getPersonId());
	}

}