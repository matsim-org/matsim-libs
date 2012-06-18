/* *********************************************************************** *
 * project: org.matsim.*
 * CollectLogMessagesAppender
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.utils.io;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;


/**
 * Collects log4j LoggingEvent instances and stores them in a List.
 * @author dgrether
 *
 */
public class CollectLogMessagesAppender extends AppenderSkeleton {

	private List<LoggingEvent> logEvents = new LinkedList<LoggingEvent>();

	/**
	 * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
	 */
	@Override
	protected void append(LoggingEvent e) {
		logEvents.add(e);
	}

	/**
	 * @see org.apache.log4j.Appender#close()
	 */
	@Override
	public void close() {
		this.logEvents.clear();
		this.closed = true;
	}

	/**
	 * @see org.apache.log4j.Appender#requiresLayout()
	 */
	@Override
	public boolean requiresLayout() {
		return false;
	}

	public List<LoggingEvent> getLogEvents() {
		return this.logEvents;
	}

}
