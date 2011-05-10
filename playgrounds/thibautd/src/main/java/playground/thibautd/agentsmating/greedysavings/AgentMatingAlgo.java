/* *********************************************************************** *
 * project: org.matsim.*
 * AgentMatingAlgo.java
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
package playground.thibautd.agentsmating.greedysavings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;

/**
 * 
 * @author thibautd
 */
public class AgentMatingAlgo {

	private final AgentTopology agentTopology;
	private final Population population;
	private final List<Person> unaffectedAgents;
	private final Comparator<Tuple<? extends Object, Double>> comparator =
		new PassengerComparator();
	private final List<Tuple<Person, Person>> carPoolingAffectations =
		new ArrayList<Tuple<Person, Person>>(1000);

	/*
	 * =========================================================================
	 * constructors
	 * =========================================================================
	 */
	public AgentMatingAlgo(
			final Population population,
			final Network network,
			final double acceptableDistance) {
		this.agentTopology = 
			new AgentTopology(network, population, acceptableDistance);
		this.population = population;
		this.unaffectedAgents = new ArrayList<Person>(population.getPersons().values());
	}

	/*
	 * =========================================================================
	 * core method
	 * =========================================================================
	 */
	/**
	 * computes all information necessary to use getters.
	 */
	public void run() {
		List<Tuple<Person, Double>> neighbors = null;
		Tuple<Person, Double> passenger;

		for (Person driver : this.population.getPersons().values()) {
			try {
				neighbors = this.agentTopology.getNeighbors(driver);
			} catch (AgentTopology.UnknownPersonException e) {
				//the person has been affected: jump to the next
				continue;
			}

			if (neighbors.size() == 0) {
				continue;
			}

			passenger = Collections.max(neighbors, this.comparator);

			// Do not mate if negative savings (ie if it increases the overall
			// travelled distance)
			if (passenger.getSecond() > 0) {
				affectAndRemoveFromTopology(driver, passenger.getFirst());
			}
		}
	}

	private void affectAndRemoveFromTopology(
			final Person driver,
			final Person passenger) {
		this.carPoolingAffectations.add(
				new Tuple<Person, Person>(driver, passenger));

		this.agentTopology.remove(driver);
		this.agentTopology.remove(passenger);

		this.unaffectedAgents.remove(driver);
		this.unaffectedAgents.remove(passenger);
	}

	/*
	 * =========================================================================
	 * getters
	 * =========================================================================
	 */
	/**
	 * @return a population were the plans define joint plans for the cliques
	 * returned by getCliques.
	 *
	 * Should be encapsulated in a PopulationWithCliques, but there is currently
	 * no XML writer for such a data structure.
	 *
	 * TODO
	 */
	public Population getPopulation() {
		return null;
	}

	/**
	 * @return clique pertenancy information, in a format compatible with the
	 * XML clique writer (could change)
	 */
	public Map<Id, List<Id>> getCliques() {
		Map<Id, List<Id>> output = new HashMap<Id, List<Id>>();
		IdFactory factory = new IdFactory();
		List<Id> currentClique;

		for (Tuple<Person, Person> couple : this.carPoolingAffectations) {
			currentClique = new ArrayList<Id>(2);
			currentClique.add(couple.getFirst().getId());
			currentClique.add(couple.getSecond().getId());

			output.put(factory.createId(), currentClique);
		}

		for (Person person : this.unaffectedAgents) {
			currentClique = new ArrayList<Id>(1);
			currentClique.add(person.getId());

			output.put(factory.createId(), currentClique);
		}

		return output;
	}

	/*
	 * =========================================================================
	 * classes
	 * =========================================================================
	 */
	/**
	 * Comparator aimed at classing passenger in ascending order according to
	 * their savings value.
	 * That means that this comparator considers a passenger A "greater than" 
	 * another passenger B if A's saving value is greater than the one of B.
	 */
	private class PassengerComparator 
			implements Comparator<Tuple<? extends Object, Double>> {

		public PassengerComparator() {}

		@Override
		public int compare(
				final Tuple<? extends Object, Double> arg0,
				final Tuple<? extends Object, Double> arg1) {
			double val1 = arg0.getSecond();
			double val2 = arg1.getSecond();

			return Double.compare(val1, val2);
		}
	}

	/**
	 * Creates a series of unique Ids.
	 */
	private class IdFactory {
		private long lastId = 0;

		public Id createId() {
			lastId++;
			return new IdImpl(lastId);
		}
	}
}

