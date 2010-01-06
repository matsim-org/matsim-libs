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

package playground.toronto;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;


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
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(inputNetworkFile);
		PopulationImpl population = scenario.getPopulation();
		population.setIsStreaming(true);
		NewAgentPtPlan planGenerator = new NewAgentPtPlan(population, outputPlansFile);
		population.addAlgorithm(planGenerator);
		new MatsimPopulationReader(scenario).readFile(inputPlansFile);
		population.printPlansCount();
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
