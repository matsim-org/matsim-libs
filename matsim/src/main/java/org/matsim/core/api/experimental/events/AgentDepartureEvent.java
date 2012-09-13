/* *********************************************************************** *
 * project: org.matsim.*
 * AgentDepartureEvent.java
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

package org.matsim.core.api.experimental.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.EventImpl;

public class AgentDepartureEvent extends EventImpl {

	public static final String EVENT_TYPE = "departure";

	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_LEGMODE = "legMode";

	private final Id linkId;
	private final String legMode;

	private final Id personId;

	public AgentDepartureEvent(final double time, final Id agentId, final Id linkId, final String legMode) {
		super(time);
		this.linkId = linkId;
		this.legMode = legMode;
		this.personId = agentId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_LINK, (this.linkId == null ? null : this.linkId.toString()));
		if (this.legMode != null) {
			attr.put(ATTRIBUTE_LEGMODE, this.legMode);
		}
		return attr;
	}

	public Id getPersonId() {
		return this.personId;
	}

	public String getLegMode() {
		return this.legMode;
	}

	public Id getLinkId() {
		return this.linkId;
	}

	public String getEventType() {
		return EVENT_TYPE;
	}

}
