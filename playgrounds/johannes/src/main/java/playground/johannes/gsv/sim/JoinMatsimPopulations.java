/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author johannes
 *
 */
public class JoinMatsimPopulations {

	private static final Logger logger = Logger.getLogger(JoinMatsimPopulations.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile(args[0]);
		
		logger.info(String.format("Loaded %s persons.", scenario.getPopulation().getPersons().size()));
		
//		Population newPop = PopulationUtils.createPopulation(config);
//		
//		logger.info("Copying persons...");
//		for(Person person : scenario.getPopulation().getPersons().values()) {
//			newPop.addPerson(person);
//		}
		
		reader.readFile(args[1]);
		
		logger.info(String.format("Loaded %s persons.", scenario.getPopulation().getPersons().size()));
		
//		logger.info("Copying persons...");
//		for(Person person : scenario.getPopulation().getPersons().values()) {
//			newPop.addPerson(person);
//		}
		
		
//		PopulationWriter writer = new PopulationWriter(newPop);
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		writer.write(args[2]);
		logger.info("Done.");
	}

}
