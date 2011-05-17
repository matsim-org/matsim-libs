/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationWithCliquesImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.population;

import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;


import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;

/**
 * @author thibautd
 */
public class PopulationWithCliques implements Population {
	private static final Logger log =
		Logger.getLogger(PopulationWithCliques.class);

	private Population populationDelegate;
	private PopulationOfCliques cliques=null;

	private PopulationFactory factory=null;

	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */
	public PopulationWithCliques(ScenarioImpl sc) {
		this((ScenarioWithCliques) sc);
		log.debug("PopulationWithCliques initialized by passing it a scenario");
	}

	public PopulationWithCliques(ScenarioWithCliques sc) {
		log.debug("PopulationWithCliques initialized by passing it a scenarioWithCliques");
		this.populationDelegate = new PopulationImpl((ScenarioImpl) sc);
		this.factory = new PopulationWithCliquesFactory(sc);
		this.cliques = new PopulationOfCliques(sc);
	}

	/*
	 * =========================================================================
	 * Delegate methods calls
	 * =========================================================================
	 */
	public PopulationFactory getFactory() {
		//log.debug("population factory from populationWithCliques accessed");
		return populationDelegate.getFactory();
	}

	public String getName() {
		return populationDelegate.getName();
	}

	public void setName(String name) {
		populationDelegate.setName(name);
	}

	public Map<Id, ? extends Person> getPersons() {
		//log.debug("Persons of the PopulationWithCliques accessed");
		//log.debug(populationDelegate.getPersons().size()+" persons returned");
		return populationDelegate.getPersons();
	}

	public void addPerson(Person p) {
		populationDelegate.addPerson(p);
	}

	/*
	 * =========================================================================
	 * PopulationWithCliques-specific Methods
	 * =========================================================================
	 */
	public PopulationOfCliques getCliques() {
		return this.cliques;
	}
}
