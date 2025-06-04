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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.matsim.core.controler.Controler;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;


/**
 * Collects log4j LoggingEvent instances and stores them in a List.
 * @author dgrether
 *
 */
public class CollectLogMessagesAppender extends AbstractAppender {

	private Queue<LogEvent> logEvents = new ConcurrentLinkedQueue<>();

	public CollectLogMessagesAppender() {
		super("collector",
				ThresholdFilter.createFilter(Level.ALL, null, null),
				Controler.DEFAULTLOG4JLAYOUT,
				false,
				new Property[0]);
	}

	@Override
	public void append(LogEvent e) {
		this.logEvents.add(e.toImmutable());
	}

	public Queue<LogEvent> getLogEvents() {
		return this.logEvents;
	}

}
