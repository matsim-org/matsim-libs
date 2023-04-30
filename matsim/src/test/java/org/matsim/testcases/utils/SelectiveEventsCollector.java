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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * Helper class that stores events of one or more specified types in a collection.
 *
 * @author mrieser
 */
public class SelectiveEventsCollector implements BasicEventHandler {

	private final static Logger log = LogManager.getLogger(SelectiveEventsCollector.class);

	private final List<Event> events = new ArrayList<Event>(50);
	private final Set<Class<?>> classes = new HashSet<Class<?>>();

	public SelectiveEventsCollector(final Class<?>... classes) {
        Collections.addAll(this.classes, classes);
	}

	@Override
	public void handleEvent(final Event event) {
		Class<?> eC = event.getClass();
		for (Class<?> klass : this.classes) {
			if (klass.isAssignableFrom(eC)) {
				this.events.add(event);
				return;
			}
		}
	}

	@Override
	public void reset(final int iteration) {
		this.events.clear();
	}

	public List<Event> getEvents() {
		return this.events;
	}

	public void printEvents() {
		for (Event e : this.events) {
			log.info(e.toString());
		}
	}
}
