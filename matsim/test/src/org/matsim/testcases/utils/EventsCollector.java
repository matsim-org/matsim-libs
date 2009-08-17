/* *********************************************************************** *
 * project: org.matsim.*
 * EventsCollector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.testcases.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.events.BasicEvent;
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * Helper class that stores all handled events in a collection.
 *
 * @author mrieser
 */
public class EventsCollector implements BasicEventHandler {
	private final List<BasicEvent> events = new ArrayList<BasicEvent>(50);

	public void handleEvent(final BasicEvent event) {
		this.events.add(event);
	}

	public void reset(final int iteration) {
		this.events.clear();
	}
	
	public List<BasicEvent> getEvents() {
		return this.events;
	}
}
