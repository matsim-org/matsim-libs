/* *********************************************************************** *
 * project: org.matsim.*
 * LinkEvent.java
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
import org.matsim.api.basic.v01.events.BasicLinkEvent;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Person;

public abstract class LinkEvent extends PersonEvent implements BasicLinkEvent {

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_LEG = "leg";

	private final Id linkId;
	private transient Link link;

	LinkEvent(final double time, final Person agent, final Link link) {
		super(time, agent);
		this.setLink(link);
		this.linkId = link.getId();
	}

	LinkEvent(final double time, final Id agentId, final Id linkId) {
		super(time, agentId);
		this.linkId = linkId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		return attr;
	}

	protected String asString() {
		return getTimeString(this.getTime()) + this.getPersonId() + "\t\t" + this.getLinkId().toString() + "\t0\t"; // FLAG + DESCRIPTION is missing here: concatenate later
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public void setLink(Link link) {
		this.link = link;
	}

	public Link getLink() {
		return link;
	}

}
