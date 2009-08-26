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

package org.matsim.core.trafficmonitoring;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.core.events.EventsImpl;

/**
 * @author dgrether
 */
public class LinkTravelTimeCounter implements BasicLinkEnterEventHandler, BasicLinkLeaveEventHandler, BasicAgentArrivalEventHandler {
	/**
	 * singleton variable
	 */
	private static LinkTravelTimeCounter instance;

	final private Map<Id, Double> enterEvents;
	final private Map<Id, Double> travelTimes;

	private LinkTravelTimeCounter(final int numberOfLinks) {
		this.enterEvents = new HashMap<Id, Double>(numberOfLinks);
		this.travelTimes = new HashMap<Id, Double>(numberOfLinks);
	}

	public static void init(final EventsImpl events, final int numberOfLinks) {
		instance = new LinkTravelTimeCounter(numberOfLinks);
		events.addHandler(instance);
	}

	public void handleEvent(final BasicLinkEnterEvent event) {
		this.enterEvents.put(event.getPersonId(), Double.valueOf(event.getTime()));
	}

	public void reset(final int iteration) {
		this.enterEvents.clear();
		this.travelTimes.clear();
	}

	public void handleEvent(final BasicLinkLeaveEvent event) {
		Double startTime = this.enterEvents.get(event.getPersonId());
		if (startTime != null) {
			this.travelTimes.put(event.getLinkId(), event.getTime() - startTime.doubleValue());
		}
	}

	public void handleEvent(final BasicAgentArrivalEvent event) {
		this.enterEvents.remove(event.getPersonId());
	}

	public Double getLastLinkTravelTime(final Id linkId) {
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
