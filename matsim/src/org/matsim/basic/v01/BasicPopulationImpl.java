/* *********************************************************************** *
 * project: org.matsim.*
 * BasicPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.basic.v01;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.interfaces.basic.v01.BasicPerson;
import org.matsim.interfaces.basic.v01.BasicPopulation;
import org.matsim.interfaces.basic.v01.Id;


public class BasicPopulationImpl <T extends BasicPerson> implements BasicPopulation<T> {
	
	private String name = null;
	
	private Map<Id, T> persons = new TreeMap<Id, T>();

	/**
	 * @see org.matsim.interfaces.basic.v01.BasicPopulation#addPerson(T)
	 */
	public void addPerson(T person) {
		// validation
		if (this.persons.containsKey(person.getId())) {
			throw new IllegalArgumentException("Person with id = " + person.getId() + " already exists.");
		}
		persons.put(person.getId(), person);
	}

	public final T getPerson(Id personId) {
		return this.persons.get(personId);
	}

	public final T getPerson(String personId) {
		return this.persons.get(new IdImpl(personId));
	}

	protected void clearPersons() {
		persons.clear();
	}
	
	public final String getName() {
		return this.name;
	}

	public final void setName(final String name) {
		this.name = name;
	}

	public Map<Id, T> getPersons() {
		return this.persons;
	}

}
