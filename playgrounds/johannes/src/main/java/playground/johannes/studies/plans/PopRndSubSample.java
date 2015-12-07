/* *********************************************************************** *
 * project: org.matsim.*
 * PopRndSubSample.java
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
package playground.johannes.studies.plans;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author illenberger
 *
 */
public class PopRndSubSample {

	private static final Logger logger = Logger.getLogger(PopRndSubSample.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String input = args[0];
//		String input = "/home/johannes/sge/prj/matsim/run/106/output/plans.routed.xml.gz";
		String output = args[1];
//		String output = "/home/johannes/gsv/ger/data/plans.routed.sub.xml.gz";
		int numSamples = Integer.parseInt(args[2]);
//		int numSamples = 10000;
		
		Config config = ConfigUtils.createConfig();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(input);

		Population pop = scenario.getPopulation();
		List<Person> persons = new ArrayList<Person>(scenario.getPopulation().getPersons().values());
		logger.info("Shuffling persons...");
		Collections.shuffle(persons);
		
		Population newPop = PopulationUtils.createPopulation(config);
		int numDelete = persons.size() - numSamples;
		if(numDelete > -1) {
			logger.info(String.format("Extracting %1$s persons...", numDelete));
			ProgressLogger.init(numSamples, 1, 10);
			
			for(int i = 0; i < numSamples; i++) {
				newPop.addPerson(persons.get(i));
				ProgressLogger.step();
			}
			ProgressLogger.terminate();
			logger.info(String.format("New population size: %1$s.", pop.getPersons().size()));
			
			new PopulationWriter(newPop, scenario.getNetwork()).write(output);
		} else {
			logger.warn(String.format("Sample size (%1$s) greater than population size (%2$s)!", numSamples, pop.getPersons().size()));
		}
	}

}
