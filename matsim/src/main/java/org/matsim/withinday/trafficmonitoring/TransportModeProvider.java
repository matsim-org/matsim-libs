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
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;

public class TransportModeProvider implements AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler {

	private final Map<Id, String> transportModes = new ConcurrentHashMap<Id, String>();
	
	public String getTransportMode(Id agentId) {
		return this.transportModes.get(agentId);
	}
	
	@Override
	public void reset(int iteration) {
		this.transportModes.clear();
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		this.transportModes.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.transportModes.put(event.getPersonId(), event.getLegMode());
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.transportModes.remove(event.getPersonId());
	}

}