/* *********************************************************************** *
 * project: org.matsim.*
 * EventAgentNoRoute.java
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

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

public class EventAgentNoRoute extends AgentEvent {

	private static final long serialVersionUID = -4288945431030953252L;

	public EventAgentNoRoute(double time, String agentId, int legId, String linkId, Person agent, Leg leg, Link link) {
		super(time, agentId, legId, linkId, agent, leg, link);
	}

	public EventAgentNoRoute(double time, String agentId, int legId, String linkId) {
		super(time, agentId, legId, linkId);
	}

	public Attributes getAttributes() {
		AttributesImpl impl = getAttributesImpl();
		//impl.addAttribute("","","Flag", "", Integer.toString(1024));
		impl.addAttribute("","","type", "", "no_route");
		return impl;
	}

	public String toString() {
		return asString() + "1024\tno_route";
	}

	public void rebuild(Plans population, NetworkLayer network) {
		rebuildAgentData(population,network);
	}

}
