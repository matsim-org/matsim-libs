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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;

public final class StreamingPopulation implements Population {
	// more than 500 compile errors if one makes this non-public. kai, feb'14

	private static final Logger log = Logger.getLogger(StreamingPopulation.class);

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private boolean isStreaming = false;
	
	private final Population delegate ;
	
	// algorithms over plans
	private final ArrayList<PersonAlgorithm> personAlgos = new ArrayList<PersonAlgorithm>();

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	StreamingPopulation(PopulationFactory populationFactory) {
		delegate = new PopulationImpl( populationFactory ) ;
	}

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final void addPerson(final Person p) {
		
		if (!this.isStreaming) {
			// streaming is off, just add the person to our list
			delegate.addPerson(p);
		} else {
			// streaming is on, run algorithm on the person and write it to file.

			/* Add Person to map, for algorithms might reference to the person
			 * with "agent = population.getPersons().get(personId);"
			 * remove it after running the algorithms! */
			delegate.addPerson(p);

			// run algos
			for (PersonAlgorithm algo : this.personAlgos) {
				algo.run(p);
			}

			// remove again as we are streaming
			delegate.removePerson(p.getId());
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


	public final boolean isStreaming() {
		return this.isStreaming;
	}
  
	public final void setIsStreaming(final boolean isStreaming) {
		this.isStreaming = isStreaming;
	}

	

	@Override
	public final String toString() {
		return "[name=" + this.getName() + "]" +
				"[is_streaming=" + this.isStreaming + "]" +
				"[nof_persons=" + this.getPersons().size() + "]" +
				"[nof_plansalgos=" + this.personAlgos.size() + "]";
	}
	
	//////////////////////////////////////////////////////////////////////
	//	delegated methods:
	//////////////////////////////////////////////////////////////////////
	

	@Override
	public PopulationFactory getFactory() {
		return this.delegate.getFactory();
	}

	@Override
	public String getName() {
		return this.delegate.getName();
	}

	@Override
	public void setName(String name) {
		this.delegate.setName(name);
	}

	@Override
	public Map<Id<Person>, ? extends Person> getPersons() {
		return this.delegate.getPersons();
	}

	@Override
	public Person removePerson(Id<Person> personId) {
		return this.delegate.removePerson(personId);
	}

	@Override
	public ObjectAttributes getPersonAttributes() {
		return this.delegate.getPersonAttributes();
	}

}
