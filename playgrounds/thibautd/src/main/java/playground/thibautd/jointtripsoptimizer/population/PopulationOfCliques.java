/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationOfCliques.java
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

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;

/**
 * Interface aimed at handling the population of cliques from within the default
 * classes of MATSim (as the StrategyManager) with few modifications.
 * It thus consists of two types of methods:
 * -methods with meaningful names
 * -methods inherited from the population interface, which redirect toward the
 *  "real" ones.
 * @author thibautd
 */
public class PopulationOfCliques implements Population {

	private static final Logger log = Logger.getLogger(PopulationOfCliques.class);
	
	private String name = null;

	private Map<Id, Clique> cliques = new TreeMap<Id, Clique>();
	private final PopulationFactory factory;

	/*
	 * =========================================================================
	 * constructors
	 * =========================================================================
	 */
	public PopulationOfCliques(ScenarioWithCliques sc) {
		this.factory = new PopulationOfCliquesFactory(sc);
		//this.cliques = this.extractCliques(sc.getConfig());
	}

	/*
	 * =========================================================================
	 * private
	 * =========================================================================
	 */
	//private Map<Id, Clique> extractCliques(Config conf) {
	//	//TODO
	//}

	/*
	 * =========================================================================
	 * miscelaneous
	 * =========================================================================
	 */
	public Map<Id,? extends Clique> getCliques() {
		return this.cliques;
	}

	public void addClique(final Clique c) {
		this.cliques.put(c.getId(), c);
	}

	/*
	 * =========================================================================
	 * population methods
	 * =========================================================================
	 */
	@Override
	public PopulationFactory getFactory() {
		return this.factory;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Map<Id,? extends Person> getPersons() {
		log.debug("method getPersons() used to retrieve cliques from PopulationOfCliques");
		return this.getCliques();
	}

	@Override
	public void addPerson(Person p) {
		try {
			this.addClique((Clique) p);
		} catch(java.lang.ClassCastException e) {
			//TODO: treat exception
			log.error("Failed to add agent "+p+": is not a Clique!");
		}
	}
}

