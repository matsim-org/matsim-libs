/* *********************************************************************** *
 * project: org.matsim.*
 * AgentEvent.java
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
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.xml.sax.helpers.AttributesImpl;

public abstract class AgentEvent extends BasicEvent {

	//includes data about the agent and agent internals like legnumber, etc
	public int legId;
	public Leg leg;

	public String linkId;
	public Link link;


	AgentEvent(final double time, final Person agent, final Link link, final Leg leg) {
		super(time, agent);
		this.leg = leg;
		this.legId = leg.getNum();
		this.link = link;
		this.linkId = link.getId().toString();
	}

	AgentEvent(final double time, final String agentId, final String linkId, final int legId) {
		super(time, agentId);
		this.legId = legId;
		this.linkId = linkId;
	}

	// Helper methods
	protected AttributesImpl getAttributesImpl() {
		AttributesImpl attr = new AttributesImpl();

		long lTime = (long)this.time; // TODO [DS] switch to double for times
		attr.addAttribute("", "", "time", "", Long.toString(lTime));
		attr.addAttribute("", "", "agent", "", this.agentId);
		attr.addAttribute("", "", "link", "", this.linkId);
		attr.addAttribute("", "", "leg", "", Integer.toString(this.legId));
		return attr;
	}

	protected String asString() {
		return getTimeString(this.time) + this.agentId + "\t"+ this.legId + "\t"+ this.linkId + "\t0\t"; // FLAG + DESCRIPTION is mising here: concat later
	}

}
