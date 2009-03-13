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

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;

public abstract class AgentEvent extends PersonEvent {

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_LEG = "leg";

	private Link link;
	private Leg leg;

	public final String linkId;
	private final Id linkId_;

	AgentEvent(final double time, final Person agent, final Link link, final Leg leg) {
		super(time, agent);
		this.link = link;
		this.linkId_ = link.getId();
		this.linkId = link.getId().toString();
		this.leg = leg;
	}

	AgentEvent(final double time, final Id agentId, final Id linkId) {
		super(time, agentId);
		this.linkId = linkId.toString();
		this.linkId_ = linkId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_LINK, this.linkId_.toString());
		return attr;
	}

	protected String asString() {
		return getTimeString(this.getTime()) + this.getPersonId() + "\t\t"+ this.linkId + "\t0\t"; // FLAG + DESCRIPTION is missing here: concatenate later
	}

	public Link getLink() {
		return this.link;
	}

	public Leg getLeg() {
		return this.leg;
	}
	
	public Id getLinkId() {
		return this.linkId_;
	}

}
