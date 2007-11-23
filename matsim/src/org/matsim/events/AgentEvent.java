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

import java.util.List;

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.xml.sax.helpers.AttributesImpl;

abstract class AgentEvent extends BasicEvent {

	//includes data about the agent and agent internas like legnumber, etc
	private int legId;
	public transient Leg leg;
	
	public String linkId;
	public transient Link link;
		

	AgentEvent(double time, String agentId, int legId,  String linkId, Person agent, Leg leg, Link link) {
		super(time, agentId, agent);
		this.legId = legId;
		this.leg = leg;
		this.linkId = linkId;
		this.link = link;
	}

	AgentEvent(double time, String agentId, int legId, String linkId) {
		super(time, agentId);
		this.legId = legId;
		this.linkId = linkId;
	}


	// Helper methods
	protected AttributesImpl getAttributesImpl() {
		AttributesImpl attr = new AttributesImpl();

		long time = (long)this.time; // TODO [DS] output sollte auf "double" umgestellt werden
		attr.addAttribute("","","time", "", Long.toString(time));
		attr.addAttribute("","","agent", "", agentId);
		attr.addAttribute("","","link", "", linkId);
		attr.addAttribute("","","leg", "", Integer.toString(legId));
		return attr;
	}

	protected String asString() {
		return getTimeString(this.time) + agentId + "\t"+ legId + "\t"+ linkId + "\t0\t"; // FLAG + DESCRIPTION is mising here: concat later
	}


	protected void rebuildAgentData(Plans population, NetworkLayer network) {
		agent = population.getPerson(agentId);
		List<Plan> plans = agent.getPlans();
		Plan selPlan = null;
		for (Plan plan : plans) if (plan.isSelected())selPlan = plan;
		leg = (Leg)selPlan.getActsLegs().get(legId*2+1);
	}
}
