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
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.PersonAlgorithm;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.PopulationBuilder;
import org.matsim.core.basic.v01.BasicPopulationImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Counter;

/**
 * Root class of the population description (previously also called "plans file")
 */
public class PopulationImpl extends BasicPopulationImpl<Person> implements Population {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private long counter = 0;
	private long nextMsg = 1;
	private boolean isStreaming = false;

	// algorithms over plans
	private final ArrayList<PersonAlgorithm> personAlgos = new ArrayList<PersonAlgorithm>();

	private static final Logger log = Logger.getLogger(PopulationImpl.class);

	private final PopulationBuilder pb = new PopulationBuilderImpl((NetworkLayer) Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE), this, (ActivityFacilities) Gbl.getWorld().getLayer(ActivityFacilities.LAYER_TYPE));

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
			this.getPersons().put(p.getId(), p);
		} else {
			// streaming is on, run algorithm on the person and write it to file.

			/* Add Person to map, for algorithms might reference to the person
			 * with "agent = population.getPersons().get(personId);"
			 * remove it after running the algorithms! */
			this.getPersons().put(p.getId(), p);

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
			for (int i=0; i<this.personAlgos.size(); i++) {
				PersonAlgorithm algo = this.personAlgos.get(i);
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
	// clear methods
	//////////////////////////////////////////////////////////////////////

	public final void clearAlgorithms() {
		this.personAlgos.clear();
	}

	public boolean removeAlgorithm(final PersonAlgorithm algo) {
		return this.personAlgos.remove(algo);
	}

	//////////////////////////////////////////////////////////////////////
	// set/add methods
	//////////////////////////////////////////////////////////////////////


	public final void addAlgorithm(final PersonAlgorithm algo) {
		this.personAlgos.add(algo);
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////


	@Override
	public final Map<Id, Person> getPersons() {
		return super.getPersons();
	}

	public final boolean isStreaming() {
		return this.isStreaming;
	}
  
  public final void setIsStreaming(final boolean isStreaming) {
  	this.isStreaming = isStreaming;
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
	public PopulationBuilder getPopulationBuilder() {
		return this.pb;
	}

}
