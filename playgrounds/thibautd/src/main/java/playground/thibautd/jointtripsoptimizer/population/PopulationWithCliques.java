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
 * Encapsulate a {@link Population} Object and a {@link PopulationWithCliques}
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
	public PopulationWithCliques(final ScenarioImpl sc) {
		this((ScenarioWithCliques) sc);
		log.debug("PopulationWithCliques initialized by passing it a scenario");
	}

	/**
	 * Construct the internal {@link Population} and {@link PopulationOfCliques}.
	 */
	public PopulationWithCliques(final ScenarioWithCliques sc) {
		log.debug("PopulationWithCliques initialized by passing it a scenarioWithCliques");
		this.populationDelegate = new PopulationImpl(sc);
		this.factory = new PopulationWithCliquesFactory(sc);
		this.cliques = new PopulationOfCliques(sc);
	}

	/*
	 * =========================================================================
	 * Delegate methods calls
	 * =========================================================================
	 */
	@Override
	public PopulationFactory getFactory() {
		//log.debug("population factory from populationWithCliques accessed");
		return populationDelegate.getFactory();
	}

	@Override
	public String getName() {
		return populationDelegate.getName();
	}

	@Override
	public void setName(final String name) {
		populationDelegate.setName(name);
	}

	@Override
	public Map<Id, ? extends Person> getPersons() {
		//log.debug("Persons of the PopulationWithCliques accessed");
		//log.debug(populationDelegate.getPersons().size()+" persons returned");
		return populationDelegate.getPersons();
	}

	@Override
	public void addPerson(final Person p) {
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
