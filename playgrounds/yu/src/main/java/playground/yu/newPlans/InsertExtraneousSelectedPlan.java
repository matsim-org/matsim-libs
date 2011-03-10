/* *********************************************************************** *
 * project: org.matsim.*
 * InsertExtraneousSelectedPlan.java
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

/**
 * 
 */
package playground.yu.newPlans;

import java.util.logging.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author yu
 * 
 */
public class InsertExtraneousSelectedPlan extends NewPopulation {
	private Population extraneousPopulation;

	public InsertExtraneousSelectedPlan(Network network, Population population,
			Population extraneousPopulation, String outputPopfilename) {
		super(network, population, outputPopfilename);
		this.extraneousPopulation = extraneousPopulation;
	}

	public void beforeWritePersonHook(Person person) {
		Id personId = person.getId();
		Person extraneousPerson = extraneousPopulation.getPersons().get(
				personId);

		if (extraneousPerson != null) {
			person.addPlan(extraneousPerson.getSelectedPlan());
		} else {
			Logger.getLogger("INSERT_EXTRANEOUS_PLAN").warning(
					"Person\t" + person.getId()
							+ "\tdoes NOT exist in the extraneous population!");
		}
	}

	/**
	 * @param args
	 */
	public static void onePlus1(String[] args) {
		String netFilename, populationFilename, extraneousPopulationFilename, outputPopulationFilename;
		if (args.length == 4) {
			netFilename = args[0];
			populationFilename = args[1];
			extraneousPopulationFilename = args[2];
			outputPopulationFilename = args[3];
		} else {
			netFilename = "../data/schweiz/input/ch.xml";
			populationFilename = "../data/schweiz/input/459.100.plans.xml.gz";
			extraneousPopulationFilename = "dummy";
			outputPopulationFilename = "dummy";
		}

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		Scenario extraneousScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		((ScenarioImpl) extraneousScenario).setNetwork((NetworkImpl) network);
		new MatsimPopulationReader(extraneousScenario)
				.readFile(extraneousPopulationFilename);

		InsertExtraneousSelectedPlan iesp = new InsertExtraneousSelectedPlan(
				network, population, extraneousScenario.getPopulation(),
				outputPopulationFilename);
		iesp.run(population);
		iesp.writeEndPlans();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		onePlus1(args);
	}
}
