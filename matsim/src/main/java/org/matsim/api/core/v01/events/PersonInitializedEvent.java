/* *********************************************************************** *
 * project: org.matsim.*
 * SimulationConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * The idea of this event is to have a list of all Persons in the beginning of the events file
 * 
 * @author tkohl / senozon
 */
public class PersonInitializedEvent extends Event implements HasPersonId, BasicLocation {

	public static final String EVENT_TYPE = "personInitialized";
	
	private final Id<Person> personId;
	private final Coord coord;
	
	public PersonInitializedEvent(double time, Id<Person> personId) {
		this(time, personId, null);
	}
	
	public PersonInitializedEvent(double time, Id<Person> personId, Coord coord) {
		super(time);
		this.personId = personId;
		this.coord = coord;
	}

	@Override
	public Id<Person> getPersonId() {
		return this.personId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Coord getCoord() {
		return this.coord;
	}

}
