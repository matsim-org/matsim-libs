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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.LinkEvent;

public abstract class LinkEventImpl extends PersonEventImpl implements LinkEvent {

	public static final String ATTRIBUTE_LINK = "link";

	private final Id linkId;
	private transient Link link;

	LinkEventImpl(final double time, final Person agent, final Link link) {
		super(time, agent);
		this.link = link;
		this.linkId = link.getId();
	}

	LinkEventImpl(final double time, final Id agentId, final Id linkId) {
		super(time, agentId);
		this.linkId = linkId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		return attr;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	/** @deprecated use getLinkId() */
	@Deprecated
	public Link getLink() {
		return this.link;
	}

}
