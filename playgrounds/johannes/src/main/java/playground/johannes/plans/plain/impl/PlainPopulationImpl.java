/* *********************************************************************** *
 * project: org.matsim.*
 * RawPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.plans.plain.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.johannes.plans.plain.PlainPerson;
import playground.johannes.plans.plain.PlainPopulation;

/**
 * @author illenberger
 *
 */
public class PlainPopulationImpl extends AbstractModifiable implements PlainPopulation {

	private Map<Id, PlainPersonImpl> persons;
	
	private Map<Id, PlainPersonImpl> unmodifiablePersons;
	
	public PlainPopulationImpl() {
		persons = new HashMap<Id, PlainPersonImpl>();
		unmodifiablePersons = Collections.unmodifiableMap(persons);
	}
	
	public Map<Id, ? extends PlainPersonImpl> getPersons() {
		return unmodifiablePersons;
	}

	public void addPerson(PlainPerson person) {
		persons.put(person.getId(), (PlainPersonImpl) person);
		modified();
	}

	public void removePerson(PlainPerson person) {
		persons.remove(person.getId());
		modified();
	}
	
	public void removePerson(Id id) {
		persons.remove(id);
		modified();
	}
}
