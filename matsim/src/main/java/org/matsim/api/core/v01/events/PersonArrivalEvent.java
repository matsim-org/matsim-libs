/* *********************************************************************** *
 * project: org.matsim.*
 * AgentArrivalEvent.java
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

public class PersonArrivalEvent extends Event implements HasPersonId, HasLinkId {

	public static final String EVENT_TYPE = "arrival";

	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_LEGMODE = "legMode";

	private final Id<Person> personId;
	private final Id<Link> linkId;
	private final String legMode;


	public PersonArrivalEvent(final double time, final Id<Person> agentId, final Id<Link> linkId, final String legMode) {
		super(time);
		this.linkId = linkId;
		this.legMode = legMode;
		this.personId = agentId;
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

	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		// linkId, personId handled by superclass
		if (this.legMode != null) {
			attr.put(ATTRIBUTE_LEGMODE, this.legMode);
		}
		return attr;
	}
}
