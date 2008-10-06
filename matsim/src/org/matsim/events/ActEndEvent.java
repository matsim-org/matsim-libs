/* *********************************************************************** *
 * project: org.matsim.*
 * ActEndEvent.java
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

import java.util.Map;

import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.Person;

public class ActEndEvent extends ActEvent {

	public ActEndEvent(double time, Person agent, Link link, Act act) {
		super(time, agent, link, act);
	}

	public ActEndEvent(double time, String agentId, String linkId, int actId, String acttype) {
		super(time, agentId, linkId, actId, acttype);
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(BasicEvent.ATTRIBUTE_TYPE, "actend");
		return attr;
	}

	@Override
	public String toString() {
		return asString() + "8\tactend"+ " " + this.acttype;
	}

}
