/* *********************************************************************** *
 * project: org.matsim.*
 * BasicEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.events;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.basic.v01.events.BasicEvent;
import org.matsim.core.utils.misc.Time;

public abstract class BasicEventImpl implements BasicEvent {

	public final static String ATTRIBUTE_TIME = "time";
	public final static String ATTRIBUTE_TYPE = "type";

	private final double time;

	private static String timeString = null;
	private static double timeCache = Time.UNDEFINED_TIME;

	public BasicEventImpl(final double time) {
		this.time = time;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attr = new LinkedHashMap<String, String>();
		attr.put(ATTRIBUTE_TIME, Double.toString(this.time));
		attr.put(ATTRIBUTE_TYPE, getEventType());
		return attr;
	}

	/**
	 * Returns a textual representation of this event used for writing the event to a text file.
	 * The string <em>must</em> follow the following format:
	 * <pre>time-stamp \t agentId \t legNumber \t linkId \t nodeId \t flag \t description</pre>
	 */
	public abstract String getTextRepresentation();

	/** @return a unique, descriptive name for this event type, used to identify event types in files. */
	abstract public String getEventType();

	/**
	 * Returns the passed time as Seconds, including a trailing tab-character.
	 * Internally caches the returned result to speed up writing many events with the same time.
	 * This may no longer be useful if times are switched to double.
	 *
	 * @param time
	 * @return
	 */
	protected static String getTimeString(final double time) {
		if (time != timeCache) {
			timeCache = time;
			timeString = Long.toString((long) timeCache) + "\t";
		}
		return timeString;
	}

	public double getTime() {
		return time;
	}
}


