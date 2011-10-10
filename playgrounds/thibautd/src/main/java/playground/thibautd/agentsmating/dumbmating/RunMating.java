/* *********************************************************************** *
 * project: org.matsim.*
 * RunMating.java
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
package playground.thibautd.agentsmating.dumbmating;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * Runs the mating algorithm defined in {@link Mater}.
 *
 * @author thibautd
 */
public class RunMating {
	private static final Logger log =
		Logger.getLogger(RunMating.class);

	private static final Mater.TripChaining chainingMode =
		Mater.TripChaining.ALL_TOGETHER;
		//Mater.TripChaining.ONE_BY_ONE;
	private static final String popFile = "../../trunk/examples/equil/plans2000.xml.gz";
	private static final String netFile = "../../trunk/examples/equil/network.xml";
	private static final String outputPrefix = "testcases/matings/3-50-together";
	private static final int minCliqueSize = 3;
	private static final int maxCliqueSize = 50;
	private static final double pNoCar = 0.7d;

	public static void main(String[] args) {
		//String popFile = args[0];
		//String outputPath = args[2];
		//int cliqueSize = Integer.parseInt(args[1]);

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		log.info("reading network");
		(new MatsimNetworkReader(scenario)).readFile(netFile);

		log.info("reading population");
		(new MatsimPopulationReader(scenario)).readFile(popFile);

		log.info("mating");
		Mater matingAlgo = new Mater(scenario, chainingMode, minCliqueSize, maxCliqueSize, pNoCar);
		matingAlgo.run();

		log.info("writing output");
		matingAlgo.write(outputPrefix);
	}
}

