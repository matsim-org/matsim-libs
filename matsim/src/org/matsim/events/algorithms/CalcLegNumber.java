/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegNumbers.java.java
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

package org.matsim.events.algorithms;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentEvent;
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.AgentStuckEvent;
import org.matsim.events.AgentWait2LinkEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.AgentStuckEventHandler;
import org.matsim.events.handler.AgentWait2LinkEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.interfaces.basic.v01.Id;

/**
 * Sets the correct leg-number in events. For each agent, a counter is increased with every departure event, starting with 0 at the first departure event.
 *
 * @author mrieser
 */
public class CalcLegNumber implements AgentDepartureEventHandler, AgentArrivalEventHandler, AgentWait2LinkEventHandler,
		AgentStuckEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

	/**
	 * Map containing <agent-id, legCount>.
	 */
	final private Map<Id, Integer> legCounters = new TreeMap<Id, Integer>();

	final private static Logger log = Logger.getLogger(CalcLegNumber.class);

	public void reset(final int iteration) {
		this.legCounters.clear();
	}

	public void handleEvent(final AgentDepartureEvent event) {
		IdImpl id = new IdImpl(event.agentId);
		Integer counter = this.legCounters.get(id);
		if (counter == null) {
			event.legId = 0;
		} else {
			event.legId = counter.intValue() + 1;
		}
		this.legCounters.put(id, Integer.valueOf(event.legId));
	}

	public void handleEvent(final AgentArrivalEvent event) {
		setLegNumber(event);
	}

	public void handleEvent(final AgentWait2LinkEvent event) {
		setLegNumber(event);
	}

	public void handleEvent(final AgentStuckEvent event) {
		setLegNumber(event);
	}

	public void handleEvent(final LinkEnterEvent event) {
		Integer counter = this.legCounters.get(new IdImpl(event.agentId));
		if (counter == null) {
			log.warn("Cannot find leg counter for agent " + event.agentId + " for event at time " + event.time + ". Most likely, a departure-event is missing for this agent.");
			return;
		}
		event.legId = counter.intValue();
	}

	public void handleEvent(final LinkLeaveEvent event) {
		Integer counter = this.legCounters.get(new IdImpl(event.agentId));
		if (counter == null) {
			log.warn("Cannot find leg counter for agent " + event.agentId + " for event at time " + event.time + ". Most likely, a departure-event is missing for this agent.");
			return;
		}
		event.legId = counter.intValue();
	}

	private void setLegNumber(final AgentEvent event) {
		Integer counter = this.legCounters.get(new IdImpl(event.agentId));
		if (counter == null) {
			log.warn("Cannot find leg counter for agent " + event.agentId + " for event at time " + event.time + ". Most likely, a departure-event is missing for this agent.");
			return;
		}
		event.legId = counter.intValue();
	}
}
