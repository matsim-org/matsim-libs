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
package playground.johannes.socialnetworks.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

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
//		String input = "/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.001.xml";
		String output = args[1];
//		String output = "/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.n500.xml";
		int numSamples = Integer.parseInt(args[2]);
//		int numSamples = 500;
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(input);

		Population pop = scenario.getPopulation();
		List<Person> persons = new LinkedList<Person>(scenario.getPopulation().getPersons().values());
		
		int numDelete = persons.size() - numSamples;
		if(numDelete > -1) {
			logger.info(String.format("Deleting %1$s persons...", numDelete));
			
			Random random = new Random();
			for(int i = 0; i < numDelete; i++) {
				int idx = random.nextInt(persons.size());
				
				Person person = persons.remove(idx);
				pop.getPersons().remove(person.getId());				
			}
		
			logger.info(String.format("New population size: %1$s.", pop.getPersons().size()));
			
			new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(output);
		} else {
			logger.warn(String.format("Sample size (%1$s) greater than population size (%2$s)!", numSamples, pop.getPersons().size()));
		}
	}

}
