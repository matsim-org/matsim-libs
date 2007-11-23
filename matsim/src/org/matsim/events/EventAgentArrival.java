/* *********************************************************************** *
 * project: org.matsim.*
 * EventAgentArrival.java
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

public class EventAgentArrival extends AgentEvent {

	private static final long serialVersionUID = -975513797122449530L;

	public EventAgentArrival(double time, String agentId, int legId, String linkId, Person agent, Leg leg, Link link) {
		super(time, agentId, legId, linkId, agent, leg, link);
	}

	public EventAgentArrival(double time, String agentId, int legId, String linkId) {
		super(time, agentId, legId, linkId);
	}

	public Attributes getAttributes() {
		AttributesImpl impl = getAttributesImpl();
		//impl.addAttribute("","","Flag", "", Integer.toString(0));
		impl.addAttribute("","","type", "", "arrival");
		return impl;
	}

	public String toString() {
		return asString() + "0\tarrival";
	}

	public void rebuild(Plans population, NetworkLayer network) {
		rebuildAgentData(population,network);
	}

}
