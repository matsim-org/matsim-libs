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

import org.matsim.population.Person;

/**
 * @author mrieser
 */
public abstract class PersonEvent extends BasicEvent {

	public Person agent;
	public final String agentId;

	public PersonEvent(final double time, final Person person) {
		super(time);
		this.agent = person;
		this.agentId = person.getId().toString();
	}
	
	public PersonEvent(final double time, final String personId)	{
		super(time);
		this.agentId = personId;
	}
	
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put("agent", this.agentId);
		return attr;
	}

}
