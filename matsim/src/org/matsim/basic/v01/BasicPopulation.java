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

import org.matsim.utils.identifiers.IdI;

public class BasicPopulation <T extends BasicPerson> {
	
	protected Map<IdI, T> persons = new TreeMap<IdI, T>();

	
    /////////////////////////////////////////////////
	// Population related methods
    /////////////////////////////////////////////////

	public void addPerson(T person) throws Exception {
		// validation
		if (this.persons.containsKey(person.getId())) {
			throw new Exception("Person with id = " + person.getId() + " already exists.");
		}
		persons.put(person.getId(), person);
	}

	public final T getPerson(IdI personId) {
		return this.persons.get(personId);
	}

	public final T getPerson(String personId) {
		return this.persons.get(new Id(personId));
	}

	protected void clearPersons() {
		persons.clear();
	}
}
