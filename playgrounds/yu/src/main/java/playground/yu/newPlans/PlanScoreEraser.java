/* *********************************************************************** *
 * project: org.matsim.*
 * PlanScoreEraser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.newPlans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * erases the scores of all the {@code Plan}s in one {@code Population}
 * 
 * @author yu
 * 
 */
public class PlanScoreEraser extends NewPopulation implements PlanAlgorithm {

	public static void main(String[] args) {

		String networkFilename, populationFilename, outputPopulationFilename;
		if (args.length < 3) {
			networkFilename = "test/input/2car1ptRoutes/net2.xml";
			populationFilename = "test/input/2car1ptRoutes/preparePop/pop100.xml";
			outputPopulationFilename = "test/input/2car1ptRoutes/preparePop/pop100.erased.xml";
		} else/* length >= 3 */{
			networkFilename = args[0];
			populationFilename = args[1];
			outputPopulationFilename = args[2];
		}
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		Population population = scenario.getPopulation();

		PlanScoreEraser pe = new PlanScoreEraser(scenario.getNetwork(),
				population, outputPopulationFilename);
		pe.run(population);
		pe.writeEndPlans();
	}

	public PlanScoreEraser(Network network, Population population,
			String outputPopulationFilename) {
		super(network, population, outputPopulationFilename);
	}

	@Override
	protected void beforeWritePersonHook(Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

	@Override
	public void run(Plan plan) {
		plan.setScore(null);
	}
}
