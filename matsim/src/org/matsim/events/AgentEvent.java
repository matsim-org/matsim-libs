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
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.xml.sax.helpers.AttributesImpl;

public abstract class AgentEvent extends BasicEvent {

	//includes data about the agent and agent internalss like legnumber, etc
	public int legId;
	public transient Leg leg;

	public String linkId;
	public transient Link link;


	AgentEvent(final double time, final String agentId, final int legId,  final String linkId, final Person agent, final Leg leg, final Link link) {
		super(time, agentId, agent);
		this.legId = legId;
		this.leg = leg;
		this.linkId = linkId;
		this.link = link;
	}

	AgentEvent(final double time, final String agentId, final int legId, final String linkId) {
		super(time, agentId);
		this.legId = legId;
		this.linkId = linkId;
	}

	// Helper methods
	protected AttributesImpl getAttributesImpl() {
		AttributesImpl attr = new AttributesImpl();

		long time = (long)this.time; // TODO [DS] switch to double for times
		attr.addAttribute("", "", "time", "", Long.toString(time));
		attr.addAttribute("", "", "agent", "", this.agentId);
		attr.addAttribute("", "", "link", "", this.linkId);
		attr.addAttribute("", "", "leg", "", Integer.toString(this.legId));
		return attr;
	}

	protected String asString() {
		return getTimeString(this.time) + this.agentId + "\t"+ this.legId + "\t"+ this.linkId + "\t0\t"; // FLAG + DESCRIPTION is mising here: concat later
	}

	protected void rebuildAgentData(final Plans population, final NetworkLayer network) {
		this.agent = population.getPerson(this.agentId);
		Plan selPlan = this.agent.getSelectedPlan();
		this.leg = (Leg)selPlan.getActsLegs().get(this.legId*2+1);
	}
}
