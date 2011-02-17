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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;

/**
 * @author yu
 * 
 */
public class InsertExtraneousSelectedPlans extends NewPopulation {
	private Population[] extraneousPopulations;

	public InsertExtraneousSelectedPlans(Network network,
			Population population, Population[] extraneousPopulations,
			String outputPopfilename) {
		super(network, population, outputPopfilename);
		this.extraneousPopulations = extraneousPopulations;
	}

	public void beforeWritePersonHook(Person person) {
		Id personId = person.getId();
		for (Population extraneousPopulation : extraneousPopulations) {
			Person extraneousPerson = extraneousPopulation.getPersons().get(
					personId);

			if (extraneousPerson != null) {
				person.addPlan(extraneousPerson.getSelectedPlan());
			} else {
				Logger
						.getLogger("INSERT_EXTRANEOUS_PLAN")
						.warning(
								"Person\t"
										+ person.getId()
										+ "\tdoes NOT exist in the extraneous population!");
			}
		}
	}

	public static void onePlusN(String[] args) {
		String netFilename, populationFilename, extraneousPopulationFilenames[], outputPopulationFilename;
		int extraneousPopSize = 2;
		// if (args.length > 4) {
		netFilename = args[0];
		populationFilename = args[1];
		extraneousPopSize = args.length - 2 - 2 + 1;
		extraneousPopulationFilenames = new String[extraneousPopSize];
		for (int i = 0; i < extraneousPopSize; i++) {
			extraneousPopulationFilenames[i] = args[2 + i];
		}
		outputPopulationFilename = args[args.length - 1];
		// } else {
		// netFilename = "../data/schweiz/input/ch.xml";
		// populationFilename = "../data/schweiz/input/459.100.plans.xml.gz";
		// extraneousPopulationFilenames = new String[] { "dummy", "dummy" };
		// outputPopulationFilename = "dummy";
		// }

		Scenario scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(netFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		Population[] extraneousPopulations = new Population[extraneousPopSize];
		int idx = 0;
		for (String extraneousPopulationFilename : extraneousPopulationFilenames) {
			Scenario extraneousScenario = new ScenarioImpl();
			((ScenarioImpl) extraneousScenario)
					.setNetwork((NetworkImpl) network);
			new MatsimPopulationReader(extraneousScenario)
					.readFile(extraneousPopulationFilename);
			extraneousPopulations[idx] = extraneousScenario.getPopulation();
			idx++;
		}
		InsertExtraneousSelectedPlans iesp = new InsertExtraneousSelectedPlans(
				network, population, extraneousPopulations,
				outputPopulationFilename);

		iesp.run(population);

		iesp.writeEndPlans();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		onePlusN(args);
	}
}
