/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningEventImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.withinday.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;

/**
 * @author cdobler
 */
public class ReplanningEvent extends Event {

	public static final String EVENT_TYPE = "replanning";
	public static final String ATTRIBUTE_REPLANNERTYPE = "replanner";
	
	public static final String ATTRIBUTE_PERSON = "person";
	
	private final Id<Person> personId;
	
	private final String replannerType;
	
	public ReplanningEvent(final double time, final Id<Person> personId, final String replannerType) {
		super(time);
		this.personId = personId;
		this.replannerType = replannerType;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();

		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_REPLANNERTYPE, this.replannerType);

		return attr;
	}

	public String getReplannerType() {
		return this.replannerType;
	}
	
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	public Id<Person> getPersonId() {
		return this.personId;
	}

}
