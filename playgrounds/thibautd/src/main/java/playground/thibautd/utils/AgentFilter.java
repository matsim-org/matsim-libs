/* *********************************************************************** *
 * project: org.matsim.*
 * AgentFilter.java
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
package playground.thibautd.utils;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class AgentFilter {
	// todo: import from cliques definition
	private static long[] toGet = {2190001, 3173100, 3644280};

	/**
	 * "filters" the given population file and exports the filtered population.
	 */
	public static void main(String[] args) {
		String fileName = args[0];
		String netFile = args[1];
		Id<Person>[] toGetIds = new Id[toGet.length];

		for (int i=0; i < toGet.length; i++) {
			toGetIds[i] = Id.create(toGet[i], Person.class);
		}

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		(new MatsimNetworkReader(scenario.getNetwork())).readFile(netFile);
		(new PopulationReader(scenario)).readFile(fileName);
		Population population = scenario.getPopulation();

		Map<Id<Person>, ? extends Person> persons = population.getPersons();
		Map<Id, Person> personsToKeep = new HashMap<Id, Person>(toGet.length);

		for (Id<Person> id : toGetIds) {
			personsToKeep.put(id, persons.get(id));
		}

		persons.clear();

		for (Person person : personsToKeep.values()) {
			population.addPerson(person);
		}

		PopulationWriter writer = new PopulationWriter(population, scenario.getNetwork());
		writer.write(fileName+".filtered");
	}
}
