/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.mid.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author johannes
 *
 */
public class XY2Links {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netfile = args[1];
		String inPopFile = args[0];
		String outPopFile = args[2];
		
		Config config = ConfigUtils.createConfig();
		MatsimRandom.reset();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scenario).readFile(netfile);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
//		NetworkCleaner cleaner = new NetworkCleaner();
//		cleaner.run(network);
		
		final PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		final PopulationReader plansReader = new MatsimPopulationReader(scenario);
		final PopulationWriter plansWriter = new PopulationWriter(plans, network);
		plansWriter.startStreaming(outPopFile);
		plans.addAlgorithm(new org.matsim.population.algorithms.XY2Links(scenario));
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(inPopFile);
		plans.printPlansCount();
		plansWriter.closeStreaming();

		System.out.println("done.");

	}
}
