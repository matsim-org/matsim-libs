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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingUtils;
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
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netfile);
		Network network = (Network) scenario.getNetwork();
		
//		NetworkCleaner cleaner = new NetworkCleaner();
//		cleaner.run(network);
		
		final Population plans = (Population) scenario.getPopulation();
		StreamingUtils.setIsStreaming(plans, true);
		final MatsimReader plansReader = new PopulationReader(scenario);
		final StreamingPopulationWriter plansWriter = new StreamingPopulationWriter(plans, network);
		plansWriter.startStreaming(outPopFile);
		StreamingUtils.addAlgorithm(plans, new org.matsim.core.population.algorithms.XY2Links(scenario));
		StreamingUtils.addAlgorithm(plans, plansWriter);
		plansReader.readFile(inPopFile);
		PopulationUtils.printPlansCount(plans) ;
		plansWriter.closeStreaming();

		System.out.println("done.");

	}
}
