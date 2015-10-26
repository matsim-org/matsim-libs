/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.lib.tools.scenarioAnalyzer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers.*;

/**
 * @author boescpa
 */
public class TestScenarioAnalyzer {

	private ScenarioAnalyzer scenarioAnalyzer;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepareTests() {
		// Run Scenario "equil"
		final Config config = ConfigUtils.loadConfig(utils.getClassInputDirectory() + "config.xml");
		config.setParam("controler", "outputDirectory", utils.getOutputDirectory());
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);
		controler.run();

		// Get network and events file
		Network network = scenario.getNetwork();
		String eventFile = this.utils.getOutputDirectory() + "ITERS/it.10/10.events.xml.gz";

		// Analyze the events:
		ScenarioAnalyzerEventHandler[] handlers = {
				new AgentCounter(network),
				new TripAnalyzer(network),
				new TripActivityCrosscorrelator(network),
				new MFDCreator(network)
		};
		scenarioAnalyzer = new ScenarioAnalyzer(eventFile, 1, handlers);
		scenarioAnalyzer.analyzeScenario();
	}

	@Test
	public void testAnalyzer() {
		// Return the results:
		scenarioAnalyzer.createResults(utils.getOutputDirectory() + "scenarioAnalyzerResults.txt", null);
	}
}
