/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package example.lsp;

import lsp.controler.LSPModule;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

/**
 * This is an academic example for the 2-echelon problem.
 * It uses the 9x9-grid network from the matsim-examples.
 *
 * The depot is located at the outer border of the network, while the jobs are located in the middle area.
 * The {@link lsp.LSP} has two different {@link lsp.LSPPlan}s:
 * 1) direct delivery from the depot
 * 2) Using a TransshipmentHub: All goods were brought from the depot to the hub, reloaded and then brought from the hub to the customers
 *
 * The decision which of these plans is chosen should be made via the Score of the plans.
 * We will modify the costs of the vehicles and/or for using(having) the Transshipment hub. Depending on this setting,
 * the plan selection should be done accordingly.
 *
 * Please note: This example is in part on existing examples, but I start from the scratch for a) see, if this works and b) have a "clean" class :)
 *
 * @author Kai Martins-Turner (kturner)
 */
final class ExampleTwoEchelonGrid {

	private static final Logger log = Logger.getLogger(ExampleTwoEchelonGrid.class);

	public static void main(String[] args) {
		log.info("Prepare Config");
		Config config = prepareConfig();

		log.info("Prepare scenario");
		Scenario scenario = prepareScenario(config);

		log.info("Prepare Controler");
		Controler controler = new Controler(scenario);
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				install( new LSPModule() );
			}
		} );

		log.info("Run MATSim");
		controler.run();

		log.info("Done.");
	}

	private static Config prepareConfig() {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(String.valueOf(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9" ), "grid9x9.xml")));

		return config;
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.createScenario(config);

		//Change speed on all links to 30 km/h (8.33333 m/s) for easier computation --> Freeflow TT per link is 2min
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setFreespeed(30/3.6);
		}

		return scenario;
	}

}
