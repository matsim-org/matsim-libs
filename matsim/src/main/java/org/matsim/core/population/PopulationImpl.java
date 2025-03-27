/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.core.population;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.scenario.Lockable;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

/**
 * @author nagel
 *
 */
/* deliberately package */ class PopulationImpl implements Population, Lockable {
	private static final Logger log = LogManager.getLogger(PopulationImpl.class);

	private final Attributes attributes = new AttributesImpl();
	private String name;
	private Map<Id<Person>, Person> persons = new LinkedHashMap<>();
	private final PopulationFactory populationFactory;
	private long counter = 0;
	private long nextMsg = 1;

	PopulationImpl(PopulationFactory populationFactory, Double scale) {
		this.populationFactory = populationFactory ;
		if(scale != null) {
			ScenarioUtils.putScale(this, scale);
		}
	}

	@Override
	public void addPerson(final Person p) {
		// validation
		if (this.getPersons().containsKey(p.getId())) {
			throw new IllegalArgumentException("Person with id = " + p.getId() + " already exists.");
		}
		if ( p instanceof Lockable ) {
			((Lockable) p).setLocked();
		}

		// show counter
		this.counter++;
		if (this.counter % this.nextMsg == 0) {
			this.nextMsg *= 4;
			printPlansCount();
		}

		this.persons.put( p.getId(), p ) ;
	}

	@Override
	public Person removePerson(Id<Person> personId) {
		return this.persons.remove(personId) ;
	}

	@Override
	public final Map<Id<Person>, ? extends Person> getPersons() {
		return persons ;
	}

	@Override
	public PopulationFactory getFactory() {
		return this.populationFactory;
	}

	@Override
	public String getName() {
		return this.name ;
	}

	@Override
	public void setName(String name) {
		this.name = name ;
	}

	@Override
	public final void setLocked() {
		for ( Person person : this.persons.values() ) {
			if ( person instanceof Lockable ) {
				((Lockable)person).setLocked() ;
			}
		}
	}

	public void printPlansCount() {
		log.info(" person # " + this.counter);
	}

	/**
	 * Attributes of the population itself. Can be used as metadata, for instance to store source of data,
	 * date of conversion, authors, sampling rate, or whatever is felt useful
	 * @return
	 */
	@Override
	public Attributes getAttributes() {
		return attributes;
	}
}
