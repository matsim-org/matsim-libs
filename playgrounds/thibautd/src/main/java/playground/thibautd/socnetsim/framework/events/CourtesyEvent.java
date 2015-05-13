/* *********************************************************************** *
 * project: org.matsim.*
 * CourtesyEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.framework.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

/**
 * @author thibautd
 */
public class CourtesyEvent extends Event implements HasPersonId {
	public static enum Type {
		sayHelloEvent, sayGoodbyeEvent;
	}

	private final Type type;
	private final Id<Person> ego;
	private final Id<Person> alter;

	public CourtesyEvent(
			final double time,
			final Id<Person> ego,
			final Id<Person> alter,
			final Type type ) {
		super( time );
		this.ego = ego;
		this.alter = alter;
		this.type = type;
	}

	@Override
	public String getEventType() {
		return type.toString();
	}

	@Override
	public Id<Person> getPersonId() {
		return ego;
	}
	
	public Id<Person> getAlterId() {
		return alter;
	}

	public Type getType() {
		return type;
	}

	@Override
	public Map<String, String> getAttributes() {
		final Map<String, String> map = super.getAttributes();

		map.put( "egoId" , ego.toString() );
		map.put( "alterId" , alter.toString() );

		return map;
	}
}

