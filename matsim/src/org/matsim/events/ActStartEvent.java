/* *********************************************************************** *
 * project: org.matsim.*
 * ActStartEvent.java
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

import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;

public class ActStartEvent extends ActEvent {

	public static final String EVENT_TYPE = "actstart";

	public ActStartEvent(final double time, final Person agent, final Link link, final Act act) {
		super(time, agent, link, act);
	}

	public ActStartEvent(final double time, final String agentId, final String linkId, final String acttype) {
		super(time, agentId, linkId, acttype);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public String toString() {
		return asString() + "7\t" + EVENT_TYPE + " " + this.acttype;
	}

}
