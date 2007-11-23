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

import java.io.Serializable;

import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.xml.sax.Attributes;

public abstract class BasicEvent implements Serializable{

	public double time;
	public String agentId;
	public transient Person agent;

	private static String timeString = null;
	private static double timeCache = Gbl.UNDEFINED_TIME;
	
	// supply one c'tor with reference to person and one without
	BasicEvent(double time, String agentId, Person agent) {this.time = time; this.agentId = agentId; this.agent = agent;}
	BasicEvent(double time, String agentId) {this.time = time; this.agentId = agentId;}
	
	public abstract Attributes getAttributes();
	public abstract String toString();

	public abstract void rebuild(Plans population, NetworkLayer network);
	
	protected static String getTimeString(double time) {
		if (time != timeCache) {
			timeCache = time;
			timeString = Long.toString((long) timeCache) + "\t";
		}
		return timeString;
	}
}


