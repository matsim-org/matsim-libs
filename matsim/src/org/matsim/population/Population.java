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

package org.matsim.population;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicPopulationImpl;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.population.algorithms.PersonAlgorithmI;
import org.matsim.utils.misc.Counter;

/**
 * root class of the population description (plans file)
 *
 * @todo well... 'plans' is a really ugly word for that. it should be
 *   'population' because it is the description of the population. inside each
 *   person there are the infos of the plans (also called the demand) a person
 *   has. At one point this 'plans' should be changed, which then means also
 *   the dtd must change and all the other things which are dependent on that
 *   (i.e. PlansHandlerImplXXX to PopulationHandlerImplXXX, and so on).
 *   Therefore everybody must agree with that!
 */
public class Population extends BasicPopulationImpl<Person> implements Iterable<Person> {

	public static final boolean USE_STREAMING = true;
	public static final boolean NO_STREAMING = false;

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private long counter = 0;
	private long nextMsg = 1;
	private boolean isStreaming;

	// algorithms over plans
	private final ArrayList<PersonAlgorithmI> personAlgos = new ArrayList<PersonAlgorithmI>();

	private static final Logger log = Logger.getLogger(Population.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Population() {
		this(!Gbl.getConfig().plans().switchOffPlansStreaming());
	}

	public Population(final boolean isStreaming) {
		this.isStreaming = isStreaming;
	}

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final void addPerson(final Person p)throws Exception {
		// validation
		if (this.persons.containsKey(p.getId())) {
			throw new Exception("Person with id = " + p.getId() + " already exists.");
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
			for (PersonAlgorithmI algo : this.personAlgos) {
				algo.run(p);
			}

			// remove again as we are streaming
			this.persons.remove(p.getId());
		}
	}

	@Override
	protected final void clearPersons() {
		this.persons.clear();
		this.counter = 0;
		this.nextMsg = 1;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public final void runAlgorithms() {
		if (!this.isStreaming) {
			for (int i=0; i<this.personAlgos.size(); i++) {
				PersonAlgorithmI algo = this.personAlgos.get(i);
				log.info("running algorithm " + algo.getClass().getName());
				Counter counter = new Counter(" person # ");
				for (Person person : this.persons.values()) {
					counter.incCounter();
					algo.run(person);
				}
				counter.incCounter();
				log.info("done running algorithm.");
			}
		} else {
			log.info("Plans-Streaming is on. Algos were run during parsing");
		}
	}

	//////////////////////////////////////////////////////////////////////
	// clear methods
	//////////////////////////////////////////////////////////////////////

	public final void clearAlgorithms() {
		this.personAlgos.clear();
	}

	public boolean removeAlgorithm(final PersonAlgorithmI algo) {
		return this.personAlgos.remove(algo);
	}

	//////////////////////////////////////////////////////////////////////
	// set/add methods
	//////////////////////////////////////////////////////////////////////


	public final void addAlgorithm(final PersonAlgorithmI algo) {
		this.personAlgos.add(algo);
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////


	public final Map<Id, Person> getPersons() {
		return this.persons;
	}

	public final boolean isStreaming() {
		return this.isStreaming;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[name=" + this.getName() + "]" +
				"[is_streaming=" + this.isStreaming + "]" +
				"[nof_persons=" + this.persons.size() + "]" +
				"[nof_plansalgos=" + this.personAlgos.size() + "]";
	}

	public void printPlansCount() {
		log.info(" person # " + this.counter);
	}

	public Iterator<Person> iterator() {
		return this.persons.values().iterator();
	}

}
