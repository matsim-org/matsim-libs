/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mrieser.svi.controller;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mrieser.svi.replanning.SviReplanner;

public class PlansAlternativeGenerator {
	
	private static void printUsage() {
		System.out.println("SviReplanning inNetwork inPopulation outPopulation zonesDescription inMatrices outMatrices");
		System.out.println("");
		System.out.println("Arguments:");
		System.out.println("  inNetwork:        an existing MATSim network file used as input");
		System.out.println("  inPopulation:     an existing MATSim plans/population file used as input");
		System.out.println("                    with exactly 1 plan per person.");
		System.out.println("  outPopulation:    filename of a not yet existing file where the modified");
		System.out.println("                    MATSim population is written to.");
	}
	
	public static void main(String[] args) {
		if (args.length < 2) {
			printUsage();
			return;
		}
		
		String inputNetworkFilename = args[0];
		String inputPopulationFilename = args[1];
		String outputPopulationFilename = args[2];

		if (inputPopulationFilename.equals(outputPopulationFilename)) {
			System.err.println("Input and Output population file must be different.");
			return;
		}

		// create scenario
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		// read network
		new MatsimNetworkReader(scenario).readFile(inputNetworkFilename);

		// stream and replan population
		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		pop.setIsStreaming(true);

		SviReplanner replanner = new SviReplanner(new Random(scenario.getConfig().global().getRandomSeed()), 1800);
		PopulationWriter writer = new PopulationWriter(pop, scenario.getNetwork());
		writer.startStreaming(outputPopulationFilename);

		pop.addAlgorithm(replanner);
		pop.addAlgorithm(writer);

		new MatsimPopulationReader(scenario).parse(inputPopulationFilename);
		
		writer.closeStreaming();
	}

}
