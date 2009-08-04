/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationImpl.java
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
package playground.johannes.plans.view.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import playground.johannes.plans.plain.impl.PlainPersonImpl;
import playground.johannes.plans.plain.impl.PlainPopulationImpl;
import playground.johannes.plans.view.Person;
import playground.johannes.plans.view.Population;

/**
 * @author illenberger
 *
 */
public class PopulationView extends AbstractView<PlainPopulationImpl> implements Population {

	private Map<String, PersonView> persons = new HashMap<String, PersonView>();
	
	private Map<String, PersonView> unmodifiablePersons;
	
	public PopulationView(PlainPopulationImpl rawPopulation) {
		super(rawPopulation);
		unmodifiablePersons = Collections.unmodifiableMap(persons);
	}
	
	public Map<String, PersonView> getPersons() {
		synchronize();
		return unmodifiablePersons;
	}
	
	@Override
	protected void update() {
		Collection<PlainPersonImpl> newPersons = synchronizeCollections(delegate.getPersons().values(), persons.values());
		
		for(PlainPersonImpl p : newPersons) {
			PersonView view = new PersonView(p);
			persons.put("id", view);
		}
	}

	public void addPerson(Person person) {
		delegate.addPerson(((PersonView) person).getDelegate());
	}

	public void removePerson(Person person) {
		delegate.removePerson(((PersonView) person).getDelegate());
	}
	
	
}
