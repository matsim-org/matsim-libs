/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveNonCarPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.util.ProgressLogger;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**
 * @author illenberger
 * 
 */
public class RemoveNonCarPlans {

	private static final Logger logger = Logger.getLogger(RemoveNonCarPlans.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ScenarioLoaderImpl loader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(args[0]);
		loader.loadScenario();
		Scenario scenario = loader.getScenario();

		Set<Person> rmPersons = new HashSet<Person>();

		logger.info("Parsing plans...");
		ProgressLogger.init(scenario.getPopulation().getPersons().size(), 1, 5);
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Set<Plan> remove = new HashSet<Plan>();
			for (Plan plan : person.getPlans()) {

				if (plan.getPlanElements().size() > 1) {
					for (int i = 1; i < plan.getPlanElements().size(); i += 2) {
						if (!((Leg) plan.getPlanElements().get(i)).getMode().equalsIgnoreCase("car")) {
							remove.add(plan);
							break;
						}
					}
				}
			}
		
			for (Plan plan : remove)
				person.getPlans().remove(plan);

			if (person.getPlans().isEmpty())
				rmPersons.add(person);
			
			ProgressLogger.step();
		}
		ProgressLogger.termiante();

		logger.info(String.format("Removing %1$s persons with zero plans...", rmPersons.size()));
		for (Person person : rmPersons)
			scenario.getPopulation().getPersons().remove(person.getId());

		logger.info(String.format("%1$s persons left.", scenario.getPopulation().getPersons().size()));
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(scenario.getConfig().getParam(
				"popfilter", "outputPlansFile"));
	}
}
