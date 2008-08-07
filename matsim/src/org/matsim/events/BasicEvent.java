/* *********************************************************************** *
 * project: org.matsim.*
 * BasicEvent.java
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

package org.matsim.events;

import org.matsim.population.Person;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;

public abstract class BasicEvent {

	public double time;
	public String agentId;
	public transient Person agent;

	private static String timeString = null;
	private static double timeCache = Time.UNDEFINED_TIME;

	// supply one c'tor with reference to person and one without
	BasicEvent(final double time, final String agentId, final Person agent) {this.time = time; this.agentId = agentId; this.agent = agent;}
	BasicEvent(final double time, final String agentId) {this.time = time; this.agentId = agentId;}

	public abstract Attributes getAttributes();
	@Override
	public abstract String toString();

	protected static String getTimeString(final double time) {
		if (time != timeCache) {
			timeCache = time;
			timeString = Long.toString((long) timeCache) + "\t";
		}
		return timeString;
	}
}


