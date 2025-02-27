/* *********************************************************************** *
 * project: org.matsim.*
 * AgentStuckEvent.java
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

package org.matsim.api.core.v01.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.XmlUtils;

public class PersonStuckEvent extends Event implements HasPersonId, HasLinkId {

	public static final String EVENT_TYPE = "stuckAndAbort";

	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_LEGMODE = "legMode";
	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUTE_REASON = "reason";

	private final Id<Person> personId;
	private final Id<Link> linkId;
	private final String legMode;
	private final String reason;

	public PersonStuckEvent(final double time, final Id<Person> agentId, final Id<Link> linkId, final String legMode, String reason) {
		super(time);
		this.personId = agentId;
		this.linkId = linkId;
		this.legMode = legMode;
		this.reason = reason;
	}

	public PersonStuckEvent(final double time, final Id<Person> agentId, final Id<Link> linkId, final String legMode) {
		this(time, agentId, linkId, legMode, null);
	}

	public Id<Person> getPersonId() {
		return this.personId;
	}

	public Id<Link> getLinkId() {
		return this.linkId;
	}

	public String getLegMode() {
		return this.legMode;
	}

	public String getReason() {
		return this.reason;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		// personId, linkId handled by superclass
		if (this.legMode != null) {
			attr.put(ATTRIBUTE_LEGMODE, this.legMode);
		}
		if (this.reason != null) {
			attr.put(ATTRIBUTE_REASON, this.legMode);
		}
		return attr;
	}

	@Override
	public void writeAsXML(StringBuilder out) {
		// Writes all common attributes
		writeXMLStart(out);

		if (this.legMode != null) {
			XmlUtils.writeEncodedAttributeKeyValue(out, ATTRIBUTE_LEGMODE, this.legMode);
		}

		if (this.reason != null) {
			XmlUtils.writeEncodedAttributeKeyValue(out, ATTRIBUTE_REASON, this.reason);
		}

		writeXMLEnd(out);
	}
}
