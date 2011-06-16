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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class AgentFilter {
	// todo: import from cliques definition
	private static long[] toGet = {2190001, 3173100, 3644280};

	/**
	 * "filters" the given population file and exports the filtered pupulation.
	 */
	public static void main(String[] args) {
		String fileName = args[0];
		String netFile = args[1];
		IdImpl[] toGetIds = new IdImpl[toGet.length];

		for (int i=0; i < toGet.length; i++) {
			toGetIds[i] = new IdImpl(toGet[i]);
		}

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		(new MatsimNetworkReader(scenario)).readFile(netFile);
		(new MatsimPopulationReader(scenario)).readFile(fileName);
		Population population = scenario.getPopulation();

		Map<Id, ? extends Person> persons = population.getPersons();
		Map<Id, Person> personsToKeep = new HashMap<Id, Person>(toGet.length);

		for (Id id : toGetIds) {
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
