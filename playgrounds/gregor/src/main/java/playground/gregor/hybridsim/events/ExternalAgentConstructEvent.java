/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.hybridsim.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;

public class ExternalAgentConstructEvent extends Event{

	private Id<Person> id;
	public ExternalAgentConstructEvent(double time, Id<Person> id) {
		super(time);
		this.id = id;
	}
	public static final String EVENT_TYPE = "ExternalAgentConstructEvent";
	
	
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	public Id<Person> getId() {
		return this.id;
	}

}
