/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.testcases.utils;

import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * A helper class for developing tests, writes out all events to the log.
 *
 * @author mrieser
 */
public class EventsLogger implements BasicEventHandler {

	private final static Logger log = LogManager.getLogger(EventsLogger.class);
	private final Level level;

	public EventsLogger() {
		this(Level.DEBUG);
	}

	public EventsLogger(final Level level) {
		this.level = level;
	}

	@Override
	public void handleEvent(final Event event) {
		StringBuilder eventXML = new StringBuilder("\t<event ");
		Map<String, String> attr = event.getAttributes();
		for (Map.Entry<String, String> entry : attr.entrySet()) {
			eventXML.append(entry.getKey());
			eventXML.append("=\"");
			eventXML.append(entry.getValue());
			eventXML.append("\" ");
		}
		eventXML.append(" />");
		log.log(this.level, eventXML.toString());
	}

	@Override
	public void reset(int iteration) {
		log.log(this.level, "EventHandler reset, iteration = " + iteration);
	}
}
