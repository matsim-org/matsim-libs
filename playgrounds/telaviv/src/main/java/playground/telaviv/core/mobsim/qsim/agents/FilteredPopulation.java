/* *********************************************************************** *
 * project: org.matsim.*
 * FilteredPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.telaviv.core.mobsim.qsim.agents;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class FilteredPopulation implements Population {

	Map<Id<Person>, Person> persons = new LinkedHashMap<>();
	
	@Override
	public PopulationFactory getFactory() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void setName(String name) {
	}

	@Override
	public Map<Id<Person>, ? extends Person> getPersons() {
		return this.persons;
	}

	@Override
	public void addPerson(Person p) {
		this.persons.put(p.getId(), p);
	}

	@Override
	public ObjectAttributes getPersonAttributes() {
		return null;
	}
	
}