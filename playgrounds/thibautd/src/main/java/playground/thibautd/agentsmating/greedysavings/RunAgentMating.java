/* *********************************************************************** *
 * project: org.matsim.*
 * RunAgentMating.java
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
package playground.thibautd.agentsmating.greedysavings;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.charts.ChartUtil;

import playground.thibautd.analysis.possiblesharedrides.CountPossibleSharedRides;
import playground.thibautd.analysis.possiblesharedrides.EventsAccumulator;
import playground.thibautd.householdsfromcensus.CliquesWriter;

/**
 * Executable class that imports data and executes the mating algorithm
 * on it.
 *
 * @author thibautd
 */
public class RunAgentMating {
	private static final Logger log =
		Logger.getLogger(RunAgentMating.class);

	private static final double ACCEPTABLE_DISTANCE = 1000d;

	public static void main(String[] args) {
		String fakeConfig;
		
		try {
			fakeConfig = args[0];
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		}

		Controler dummyControler = new Controler(fakeConfig);
		dummyControler.setOverwriteFiles(true);
		dummyControler.run();
		Network network = dummyControler.getScenario().getNetwork();
		Population population = dummyControler.getScenario().getPopulation();

		AgentMatingAlgo algo = new AgentMatingAlgo(population, network, ACCEPTABLE_DISTANCE);
		algo.run();
		// has the side-effect of modifying the plans, that will be writen back
		// by the controler
		algo.getPopulation();

		PopulationWriter populationWriter = new PopulationWriter(population , network);
		CliquesWriter cliqueWriter = new CliquesWriter(algo.getCliques());

		String path = dummyControler.getControlerIO().getOutputPath();
		try {
			cliqueWriter.writeFile(path+"/mating_cliques.xml.gz");
			populationWriter.write(path+"/mating_plans.xml.gz");
		} catch (Exception e) {
			throw new RuntimeException("error while writing clique", e);
		}
	}
}

