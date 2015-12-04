/* *********************************************************************** *
 * project: org.matsim.*
 * Plans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.utils.misc.Counter;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * Root class of the population description (previously also called "plans file")
 */
public final class PopulationImpl implements Population {
	// more than 500 compile errors if one makes this non-public. kai, feb'14

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private long counter = 0;
	private long nextMsg = 1;
	private boolean isStreaming = false;
	
	private String name ;
	private boolean locked = false ;

	private Map<Id<Person>, Person> persons = new LinkedHashMap<Id<Person>, Person>();

	// algorithms over plans
	private final ArrayList<PersonAlgorithm> personAlgos = new ArrayList<PersonAlgorithm>();

	private static final Logger log = Logger.getLogger(PopulationImpl.class);

	private final PopulationFactory populationFactory;
	
	private final ObjectAttributes personAttributes = new ObjectAttributes();

    PopulationImpl(PopulationFactory populationFactory) {
        this.populationFactory = populationFactory;
    }

    //////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final void addPerson(final Person p) {
		// validation
		if (this.getPersons().containsKey(p.getId())) {
			throw new IllegalArgumentException("Person with id = " + p.getId() + " already exists.");
		}

		// show counter
		this.counter++;
		if (this.counter % this.nextMsg == 0) {
			this.nextMsg *= 2;
			printPlansCount();
		}

		if (!this.isStreaming) {
			// streaming is off, just add the person to our list
			this.persons.put(p.getId(), p);
		} else {
			// streaming is on, run algorithm on the person and write it to file.

			/* Add Person to map, for algorithms might reference to the person
			 * with "agent = population.getPersons().get(personId);"
			 * remove it after running the algorithms! */
			this.persons.put(p.getId(), p);

			// run algos
			for (PersonAlgorithm algo : this.personAlgos) {
				algo.run(p);
			}

			// remove again as we are streaming
			this.getPersons().remove(p.getId());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public final void runAlgorithms() {
		if (!this.isStreaming) {
            for (PersonAlgorithm algo : this.personAlgos) {
                log.info("running algorithm " + algo.getClass().getName());
                Counter cntr = new Counter(" person # ");
                for (Person person : this.getPersons().values()) {
                    cntr.incCounter();
                    algo.run(person);
                }
                cntr.incCounter();
                log.info("done running algorithm.");
            }
		} else {
			log.info("Plans-Streaming is on. Algos were run during parsing");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// algorithms
	//////////////////////////////////////////////////////////////////////

	public final void clearAlgorithms() {
		this.personAlgos.clear();
	}

    public final void addAlgorithm(final PersonAlgorithm algo) {
		this.personAlgos.add(algo);
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////


	@Override
	public final Map<Id<Person>, ? extends Person> getPersons() {
		return persons ;
	}

	public final boolean isStreaming() {
		return this.isStreaming;
	}
  
	public final void setIsStreaming(final boolean isStreaming) {
		this.isStreaming = isStreaming;
	}

	@Override
	public ObjectAttributes getPersonAttributes() {
		return this.personAttributes;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[name=" + this.getName() + "]" +
				"[is_streaming=" + this.isStreaming + "]" +
				"[nof_persons=" + this.getPersons().size() + "]" +
				"[nof_plansalgos=" + this.personAlgos.size() + "]";
	}

	public void printPlansCount() {
		log.info(" person # " + this.counter);
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

	public final void setLocked() {
		this.locked = true ;
		for ( Person person : this.persons.values() ) {
			if ( person instanceof PersonImpl ) {
				((PersonImpl)person).setLocked() ;
			}
		}
	}

}
