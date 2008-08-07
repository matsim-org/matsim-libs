/* *********************************************************************** *
 * project: org.matsim.*
 * LinkTravelTimeCounter.java
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

package org.matsim.trafficmonitoring;

import java.util.HashMap;
import java.util.Map;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.LinkEnterEnter;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.Events;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;

/**
 * @author dgrether
 */
public class LinkTravelTimeCounter implements EventHandlerLinkEnterI, EventHandlerLinkLeaveI,
EventHandlerAgentArrivalI {
	/**
	 * singleton variable
	 */
	private static LinkTravelTimeCounter instance;

	final private Map<String, Double> enterEvents;
	final private Map<String, Double> travelTimes;

	private LinkTravelTimeCounter(final int numberOfLinks) {
		this.enterEvents = new HashMap<String, Double>(numberOfLinks);
		this.travelTimes = new HashMap<String, Double>(numberOfLinks);
	}

	public static void init(final Events events, final int numberOfLinks) {
		instance = new LinkTravelTimeCounter(numberOfLinks);
		events.addHandler(instance);
	}

	public void handleEvent(final LinkEnterEnter event) {
		this.enterEvents.put(event.agentId, Double.valueOf(event.time));
	}

	public void reset(final int iteration) {
		this.enterEvents.clear();
		this.travelTimes.clear();
	}

	public void handleEvent(final LinkLeaveEvent event) {
		Double startTime = this.enterEvents.get(event.agentId);
		if (startTime != null) {
			this.travelTimes.put(event.linkId, event.time - startTime.doubleValue());
		}
	}

	public void handleEvent(final AgentArrivalEvent event) {
		this.enterEvents.remove(event.agentId);
	}

	public Double getLastLinkTravelTime(final String linkId) {
		return this.travelTimes.get(linkId);
	}

	/**
	 *
	 * @return the singleton if initialized correctly
	 */
	public static synchronized LinkTravelTimeCounter getInstance() {
		if (instance == null) {
			throw new RuntimeException("LinkTravelTimeCounter not initialized! Call init(...) first!");
		}
		return instance;
	}

}
