/* *********************************************************************** *
 * project: org.matsim.*
 * SizeFilter.java
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
package playground.thibautd.agentsmating.ptv2matsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

import playground.thibautd.householdsfromcensus.CliquesWriter;
import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.PopulationOfCliques;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithJointTripsWriterHandler;
import playground.thibautd.jointtripsoptimizer.population.ScenarioWithCliques;
import playground.thibautd.jointtripsoptimizer.run.JointControler;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;

/**
 * Very simple utility, which creates a population without the cliques of more than 10 individuals.
 * @author thibautd
 */
public class SizeFilter {
	private static final Log log =
		LogFactory.getLog(SizeFilter.class);

	private final static int MAX_SIZE = 5;
	public static void main(final String[] args) {
		String configFile = args[0];
		
		Config config = JointControlerUtils.createConfig(configFile);
		ScenarioWithCliques scenario = JointControlerUtils.createScenario(config);
		PopulationOfCliques populationOfCliques = scenario.getCliques();
		Population population = scenario.getPopulation();

		Map<Id, ? extends Clique> cliques = populationOfCliques.getCliques();
		population = new PopulationWithCliques(scenario);

		Map<Id, List<Id>> cliquesToWrite = new HashMap<Id, List<Id>>();
		int count = 0;
		for (Clique clique : cliques.values()) {
			if (clique.getMembers().size() <= MAX_SIZE) {
				cliquesToWrite.put(clique.getId(), new ArrayList<Id>(clique.getMembers().keySet()));

				for (Person person : clique.getMembers().values()) {
					population.addPerson(person);
				}
			}
			else {
				count++;
			}
		}
		log.info(count+" cliques removed");

		String popFile = config.controler().getOutputDirectory() + "individuals-less-than-10.xml.gz";
		PopulationWriter writer = (new PopulationWriter(population, scenario.getNetwork(), scenario.getKnowledges())) ;
		writer.setWriterHandler(new PopulationWithJointTripsWriterHandler(scenario.getNetwork(), scenario.getKnowledges()));
		writer.write(popFile);

		String cliqueFile = config.controler().getOutputDirectory() + "cliques-less-than-10.xml.gz";
		(new CliquesWriter(cliquesToWrite)).writeFile(cliqueFile);
	}
}

