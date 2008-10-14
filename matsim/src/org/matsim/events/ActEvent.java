/* *********************************************************************** *
 * project: org.matsim.*
 * ActEvent.java
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

abstract class ActEvent extends PersonEvent {

	public String linkId;
	private int actId;
	public String acttype;

	public transient Link link;
	public transient Act act;

	ActEvent(final double time, final Person agent, final Link link, final Act act) {
		super(time, agent);
		this.act = act;
		this.link = link;
		this.linkId = link.getId().toString();
		this.acttype = act.getType();
	}

	ActEvent(final double time, final String agentId, final String linkId, final int actId, final String acttype) {
		super(time, agentId);
		this.linkId = linkId;
		this.actId = actId;
		this.acttype = acttype == null ? "" : acttype;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();

		attr.put("link", this.linkId);
		attr.put("activity", Integer.toString(this.actId));
		attr.put("act_type", this.acttype);
		return attr;
	}

	protected String asString() {
		return getTimeString(this.time) + this.agentId + "\t0\t"+ this.linkId + "\t0\t"; // FLAG + DESCRIPTION is missing here: concat later
	}

}
