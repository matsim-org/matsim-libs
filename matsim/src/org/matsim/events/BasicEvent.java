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

package org.matsim.events;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.population.Person;
import org.matsim.utils.misc.Time;

public abstract class BasicEvent {

	public final static String ATTRIBUTE_TYPE = "type";
	
	public final double time;
	public final String agentId;
	public Person agent = null;

	private static String timeString = null;
	private static double timeCache = Time.UNDEFINED_TIME;

	protected BasicEvent(final double time, final Person agent) {
		this.time = time;
		this.agent = agent;
		this.agentId = agent.getId().toString();
	}

	BasicEvent(final double time, final String agentId) {
		this.time = time;
		this.agentId = agentId;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attr = new LinkedHashMap<String, String>();
		attr.put("time", getTimeString(this.time));
		attr.put("agent", this.agentId);
		return attr;
	}

	/**
	 * Returns a textual representation of this event used for writing the event to a text file.
	 * The string <em>must</em> follow the following format:
	 * <pre>time-stamp \t agentId \t legNumber \t linkId \t nodeId \t flag \t description</pre>
	 */
	@Override
	public abstract String toString();

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
}


