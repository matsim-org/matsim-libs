/* *********************************************************************** *
 * project: org.matsim.*
 * AgentEvent.java
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
import org.matsim.population.Leg;
import org.matsim.population.Person;

public abstract class AgentEvent extends PersonEvent {

	public Link link;
	public Leg leg;

	public String linkId;
	public int legId;

	AgentEvent(final double time, final Person agent, final Link link, final Leg leg) {
		super(time, agent);
		this.link = link;
		this.linkId = link.getId().toString();
		this.leg = leg;
		this.legId = leg.getNum();
	}

	AgentEvent(final double time, final String agentId, final String linkId, final int legId) {
		super(time, agentId);
		this.legId = legId;
		this.linkId = linkId;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put("link", this.linkId);
		attr.put("leg", Integer.toString(this.legId));
		return attr;
	}

	protected String asString() {
		return getTimeString(this.time) + this.agentId + "\t"+ this.legId + "\t"+ this.linkId + "\t0\t"; // FLAG + DESCRIPTION is mising here: concat later
	}

}
