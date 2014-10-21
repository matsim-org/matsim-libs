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

package playground.boescpa.topdad.postprocessing;

import org.matsim.api.core.v01.network.Network;
import playground.boescpa.lib.tools.scenarioAnalyzer.ScenarioAnalyzer;
import playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers.AgentCounter;
import playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers.ScenarioAnalyzerEventHandler;
import playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers.TripAnalyzer;
import playground.boescpa.lib.tools.scenarioAnalyzer.spatialEventCutters.CirclePointCutter;
import playground.boescpa.lib.tools.scenarioAnalyzer.spatialEventCutters.SpatialEventCutter;
import playground.boescpa.lib.tools.networkModification.NetworkUtils;

/**
 * Analyzes events file from ToPDAd-Simulations...
 *
 * @author boescpa
 */
public class AnalyzeScenarios {

	public static void main(String[] args) {
		Network network = NetworkUtils.readNetwork(args[0]);

		// Analyze the events:
		String eventFile = args[1];
		ScenarioAnalyzerEventHandler[] handlers = {
			new AgentCounter(),
			new TripAnalyzer(network)
		};
		ScenarioAnalyzer scenarioAnalyzer = new ScenarioAnalyzer(eventFile, handlers);
		scenarioAnalyzer.analyzeScenario();

		// Return the results:
		SpatialEventCutter circlePointCutter = new CirclePointCutter(30000,683518.0,246836.0); // 30km around Zurich, Bellevue
		scenarioAnalyzer.createResults(args[1] + "_analysisResults.txt", circlePointCutter);
	}

}
