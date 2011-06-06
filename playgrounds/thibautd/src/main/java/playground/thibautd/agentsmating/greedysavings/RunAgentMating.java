/* *********************************************************************** *
 * project: org.matsim.*
 * RunAgentMating.java
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

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.thibautd.householdsfromcensus.CliquesWriter;

/**
 * Executable class that imports data and executes the mating algorithm
 * on it.
 *
 * @author thibautd
 */
public class RunAgentMating {
	private static final Logger log =
		Logger.getLogger(RunAgentMating.class);

	private static final double ACCEPTABLE_DISTANCE = 3000d;

	/**
	 * usage: RunAgentMating pop net out
	 */
	public static void main(String[] args) {
		String populationFile = args[0];
		String networkFile = args[1];
		String outputPath = args[2];

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		log.info("reading network");
		(new MatsimNetworkReader(scenario)).readFile(networkFile);

		log.info("trying to read facilities");
		ActivityFacilitiesImpl facilities = null;
		try {
			String facilityFile = args[3];
			(new MatsimFacilitiesReader((ScenarioImpl) scenario)).readFile(facilityFile);
			facilities =  ((ScenarioImpl) scenario).getActivityFacilities();
		} catch (ArrayIndexOutOfBoundsException e) {
			log.info("no facility file given, nothing loaded");
		}

		log.info("reading population");
		(new MatsimPopulationReader(scenario)).readFile(populationFile);


		Population population = scenario.getPopulation();
		Network network = scenario.getNetwork();

		AgentMatingAlgo algo = new AgentMatingAlgo(
				population,
				network,
				ACCEPTABLE_DISTANCE,
				facilities);
		algo.run();
		// modifies the population: no need for storing it.
		algo.getPopulation();

		PopulationWriter populationWriter = new PopulationWriter(population , network);
		CliquesWriter cliqueWriter = new CliquesWriter(algo.getCliques());
		if (facilities != null) {
			(new FacilitiesWriter(facilities)).write(outputPath+"mating_facilities.xml.gz");
		}

		try {
			cliqueWriter.writeFile(outputPath+"mating_cliques.xml.gz");
			populationWriter.write(outputPath+"mating_plans.xml.gz");
		} catch (Exception e) {
			throw new RuntimeException("error while writing clique", e);
		}
	}
}

