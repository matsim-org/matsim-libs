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

package org.matsim.core.events;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicActEvent;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;

abstract class ActEvent extends PersonEvent implements BasicActEvent {

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_ACTTYPE = "actType";

	private final Id linkId;
	private final String acttype;

	private transient Link link;
	private transient Activity act;

	ActEvent(final double time, final Person agent, final Link link, final Activity act) {
		super(time, agent);
		this.act = act;
		this.link = link;
		this.linkId = link.getId();
		this.acttype = act.getType();
	}

	ActEvent(final double time, final Id agentId, final Id linkId, final String acttype) {
		super(time, agentId);
		this.linkId = linkId;
		this.acttype = acttype == null ? "" : acttype;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();

		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		attr.put(ATTRIBUTE_ACTTYPE, this.acttype);
		return attr;
	}

	protected String asString() {
		return getTimeString(this.getTime()) + this.getPersonId() + "\t\t"+ this.linkId.toString() + "\t0\t"; // FLAG + DESCRIPTION is missing here: concatenate later
	}

	public String getActType() {
		return this.acttype;
	}

	@Deprecated // should be set in Constructor
	public void setLink(final Link link) {
		this.link = link;
	}

	public Link getLink() {
		return this.link;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public Activity getAct() {
		return this.act;
	}

}
