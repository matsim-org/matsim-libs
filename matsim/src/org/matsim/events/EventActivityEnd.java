/* *********************************************************************** *
 * project: org.matsim.*
 * EventActivityEnd.java
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

import org.matsim.network.LinkImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

public class EventActivityEnd extends ActEvent {

	private static final long serialVersionUID = -4702539036126142181L;

	public EventActivityEnd(double time, String agentId, Person agent, LinkImpl link, Act act) {
		super(time, agentId, agent, link, act);
	}

	public EventActivityEnd(double time, String agentId, String linkId, int actId, String acttype) {
		super(time, agentId, linkId, actId, acttype);
	}

	@Override
	public Attributes getAttributes() {
		AttributesImpl impl = getAttributesImpl();
		//impl.addAttribute("","","Flag", "", Integer.toString(8));
		impl.addAttribute("","","type", "", "actend");
		return impl;
	}

	@Override
	public String toString() {
		return asString() + "8\tactend"+ " " + acttype;
	}

	@Override
	public void rebuild(Plans population, NetworkLayer network) {
		rebuildActData(population,network);
		
	}
}
