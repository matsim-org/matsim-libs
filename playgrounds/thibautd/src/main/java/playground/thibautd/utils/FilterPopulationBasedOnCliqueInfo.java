/* *********************************************************************** *
 * project: org.matsim.*
 * FilterPopulationBasedOnCliqueInfo.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.PopulationOfCliques;
import playground.thibautd.jointtripsoptimizer.population.ScenarioWithCliques;
import playground.thibautd.jointtripsoptimizer.run.JointControler;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;

/**
 * creates a sample of a population containing only the agents listed in
 * a clique file.
 * <br>
 * Used to generate the "individual" population corresponding to a "joint"
 * population.
 *
 * @author thibautd
 */
public class FilterPopulationBasedOnCliqueInfo {
	private static final Log log =
		LogFactory.getLog(FilterPopulationBasedOnCliqueInfo.class);

	/**
	 * arg: a config defining population, cliques and output directory.
	 */
	public static void main(final String[] args) {
		String configFile = args[0];

		JointControler controler = (JointControler) JointControlerUtils.createControler(configFile);
		ScenarioWithCliques scenario = (ScenarioWithCliques) controler.getScenario();
		PopulationOfCliques cliques = scenario.getCliques();
		Population population = scenario.getPopulation();

		log.debug(cliques.getCliques().size()+" cliques");
		List<Id> personsToKeep = new ArrayList<Id>();
		for (Clique clique : cliques.getCliques().values()) {
			personsToKeep.addAll(clique.getMembers().keySet());
		}

		log.debug(personsToKeep.size()+" persons to keep");

		Map<Id, ? extends Person> persons = population.getPersons();
		population = new PopulationImpl(scenario);

		Person person;
		for (Id id : personsToKeep) {
			person = persons.get(id);
			population.addPerson(person);
		}

		String popFile = controler.getConfig().controler().getOutputDirectory() + "individual_population.xml.gz";
		(new PopulationWriter(population, scenario.getNetwork(), scenario.getKnowledges())).write(popFile);
	}
}

