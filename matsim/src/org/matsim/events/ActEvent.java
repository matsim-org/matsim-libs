/* *********************************************************************** *
 * project: org.matsim.*
 * ActEvent.java
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
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.xml.sax.helpers.AttributesImpl;

abstract class ActEvent extends BasicEvent {

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

	protected AttributesImpl getAttributesImpl() {
		AttributesImpl attr = new AttributesImpl();

		long lTime = (long)this.time; // TODO [DS] output sollte auf "double" umgestellt werden
		attr.addAttribute("", "", "time", "", Long.toString(lTime));
		attr.addAttribute("", "", "agent", "", this.agentId);
		attr.addAttribute("", "", "link", "", this.linkId);
		attr.addAttribute("", "", "activity", "", Integer.toString(this.actId));
		attr.addAttribute("", "", "act_type", "", this.acttype);
		return attr;
	}

	protected String asString() {
		return getTimeString(this.time) + this.agentId + "\t0\t"+ this.linkId + "\t0\t"; // FLAG + DESCRIPTION is missing here: concat later
	}

}
