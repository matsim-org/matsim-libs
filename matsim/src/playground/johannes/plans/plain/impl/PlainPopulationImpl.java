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

import playground.johannes.plans.ModCount;
import playground.johannes.plans.plain.PlainPerson;
import playground.johannes.plans.plain.PlainPopulation;

/**
 * @author illenberger
 *
 */
public class PlainPopulationImpl implements PlainPopulation, ModCount {

	private Map<String, PlainPersonImpl> persons;
	
	private Map<String, PlainPersonImpl> unmodifiablePersons;
	
	private long modCount;
	
	public PlainPopulationImpl() {
		persons = new HashMap<String, PlainPersonImpl>();
		unmodifiablePersons = Collections.unmodifiableMap(persons);
	}
	
	public Map<String, PlainPersonImpl> getPersons() {
		return unmodifiablePersons;
	}

	public long getModCount() {
		return modCount;
	}

	public void addPerson(PlainPerson person) {
		persons.put("id", (PlainPersonImpl) person);
		modCount++;
	}

	public void removePerson(PlainPerson person) {
		persons.remove((PlainPersonImpl) person);
		modCount++;
	}
}
