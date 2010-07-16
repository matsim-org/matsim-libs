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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;

/**
 * @author illenberger
 * 
 */
public class RemoveNonCarPlans {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(args[0]);
		loader.loadScenario();
		Scenario scenario = loader.getScenario();

		Set<Person> rmPersons = new HashSet<Person>();

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
		}

		for (Person person : rmPersons)
			scenario.getPopulation().getPersons().remove(person.getId());

		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(scenario.getConfig().getParam(
				"plans", "outputPlansFile"));
	}
}
