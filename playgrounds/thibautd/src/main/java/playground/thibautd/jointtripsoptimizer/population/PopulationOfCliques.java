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
 * Container aimed at handling the population of cliques from within the default
 * classes of MATSim (as the StrategyManager) with few modifications.
 * <BR>
 * It thus consists of two types of methods:
 * <ul>
 * <li>methods with meaningful names
 * <li>methods inherited from the population interface, which redirect toward the
 *  "real" ones.
 *  </ul>
 *
 * Instances of this class are created together with the individual population
 * while constructing a {@link PopulationWithCliques}.
 *
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
	/**
	 * Constructs an instance.
	 *
	 * Do not call: use {@link PopulationWithCliques} to constuct simultaneously
	 * population and cliques.
	 */
	PopulationOfCliques(ScenarioWithCliques sc) {
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

	/**
	 * binds to {@link PopulationOfCliques#getCliques()}
	 */
	@Override
	public Map<Id,? extends Person> getPersons() {
		//log.debug("method getPersons() used to retrieve cliques from PopulationOfCliques");
		return this.getCliques();
	}

	/**
	 * binds to {@link PopulationOfCliques#addClique(Clique)}
	 *
	 * @throws IllegalArgumentException if the argument is not a
	 * {@link Clique}
	 */
	@Override
	public void addPerson(final Person p) {
		try {
			this.addClique((Clique) p);
		} catch(ClassCastException e) {
			throw new IllegalArgumentException(
					"Failed to add agent "+p+": is not a Clique!",
					e);
		}
	}
}

