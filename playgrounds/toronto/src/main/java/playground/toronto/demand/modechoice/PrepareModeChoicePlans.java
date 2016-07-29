/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareModeChoicePlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.toronto.demand.modechoice;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * Prepares a plans file with only a single plan per person to be used with
 * the simple mode choice implementation currently available in MATSim. It
 * takes the existing plan, sets all leg-modes to "car" and then makes a copy
 * of it and sets all leg-modes in the copy to "pt".
 *
 * @author mrieser
 * @author ychen
 */
public class PrepareModeChoicePlans {

	public static void run(final String inputPlansFile, final String inputNetworkFile, final String outputPlansFile) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputNetworkFile);
		Population population = (Population) scenario.getPopulation();
		StreamingUtils.setIsStreaming(population, true);
		NewAgentPtPlan planGenerator = new NewAgentPtPlan(network, population, outputPlansFile);
		final PersonAlgorithm algo = planGenerator;
		StreamingUtils.addAlgorithm(population, algo);
		new PopulationReader(scenario).readFile(inputPlansFile);
		PopulationUtils.printPlansCount(population) ;
		planGenerator.writeEndPlans();
		System.out.println("done.");
	}

	/**
	 * @param args input-population-file, network-file, output-population-file
	 */
	public static void main(final String[] args) {
		if (args.length == 3) {
			PrepareModeChoicePlans.run(args[0], args[1], args[2]);
		} else {
			System.err.println("This program expected 3 arguments:");
			System.err.println(" - input-population-file");
			System.err.println(" - input-network-file");
			System.err.println(" - output-population-file");
		}
	}

}
