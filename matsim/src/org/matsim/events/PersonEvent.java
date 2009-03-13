/* *********************************************************************** *
 * project: org.matsim.*
 * PersonEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import org.matsim.interfaces.core.v01.Person;

/**
 * @author mrieser
 */
public abstract class PersonEvent extends BasicEvent {

	public static final String ATTRIBUTE_AGENT = "agent";

	private Person person;
	public final String agentId;
	private final Id personId;

	public PersonEvent(final double time, final Person person) {
		super(time);
		this.person = person;
		this.personId = person.getId();
		this.agentId = person.getId().toString();
	}

	public PersonEvent(final double time, final Id personId)	{
		super(time);
		this.personId = personId;
		this.agentId = personId.toString();
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_AGENT, this.personId.toString());
		return attr;
	}

	@Deprecated // should be set via Constructor...
	public void setAgent(final Person agent) {
		this.person = agent;
	}

	public Person getAgent() { // TODO [MR] rename to getPerson
		return this.person;
	}

	public Id getPersonId() {
		return this.personId;
	}
}
