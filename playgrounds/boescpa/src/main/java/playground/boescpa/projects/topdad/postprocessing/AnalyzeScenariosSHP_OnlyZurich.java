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

package playground.boescpa.projects.topdad.postprocessing;

import org.matsim.api.core.v01.network.Network;
import playground.boescpa.lib.tools.NetworkUtils;
import playground.boescpa.analysis.scenarioAnalyzer.ScenarioAnalyzer;
import playground.boescpa.analysis.scenarioAnalyzer.eventHandlers.AgentCounter;
import playground.boescpa.analysis.scenarioAnalyzer.eventHandlers.ScenarioAnalyzerEventHandler;
import playground.boescpa.analysis.scenarioAnalyzer.eventHandlers.TripActivityCrosscorrelator;
import playground.boescpa.analysis.scenarioAnalyzer.eventHandlers.TripAnalyzer;
import playground.boescpa.analysis.spatialCutters.SHPFileCutter;
import playground.boescpa.analysis.spatialCutters.SpatialCutter;

/**
 * Analyzes events file from ToPDAd-Simulations...
 *
 * @author boescpa
 */
public class AnalyzeScenariosSHP_OnlyZurich {

	public static void main(String[] args) {

		int scaleFactor = 10;

		Network network = NetworkUtils.readNetwork(args[0]);

		String shpZurichPath = args[1];
		SpatialCutter zurichCutter = new SHPFileCutter(shpZurichPath);

		for (int i = 2; i < args.length; i++) {
			try {
				String path2EventFile = args[i];

				// Analyze the events:
				ScenarioAnalyzerEventHandler[] handlers = {
						new AgentCounter(network),
						new TripAnalyzer(network),
						new TripActivityCrosscorrelator(network)
				};
				ScenarioAnalyzer scenarioAnalyzer = new ScenarioAnalyzer(path2EventFile, scaleFactor, handlers);
				scenarioAnalyzer.analyzeScenario();

				// Return the results:
				//	Zurich
				scenarioAnalyzer.createResults(path2EventFile + "_analysisResults_Zurich.csv", zurichCutter);

			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
}
