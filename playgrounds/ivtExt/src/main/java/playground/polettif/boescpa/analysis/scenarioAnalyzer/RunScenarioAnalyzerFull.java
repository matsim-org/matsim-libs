/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.polettif.boescpa.analysis.scenarioAnalyzer;

import org.matsim.api.core.v01.network.Network;
import playground.polettif.boescpa.analysis.scenarioAnalyzer.eventHandlers.*;
import playground.polettif.boescpa.analysis.spatialCutters.NoCutter;
import playground.polettif.boescpa.lib.tools.NetworkUtils;

/**
 * An example how to use the scenario analyzer to
 *      fully analyze the full events file.
 *
 * @author boescpa
 */
public class RunScenarioAnalyzerFull {

	public static void main(String[] args) {
		Network network = NetworkUtils.readNetwork(args[0]);
		String path2EventFile = args[1];
		int scaleFactor = 10;

		try {
			// Analyze the events:
			ScenarioAnalyzerEventHandler[] handlers = {
					new AgentCounter(network),
					new TripAnalyzer(network),
					new TripActivityCrosscorrelator(network),
					new MFDCreator(network)
			};
			ScenarioAnalyzer scenarioAnalyzer = new ScenarioAnalyzer(path2EventFile, scaleFactor, handlers);
			scenarioAnalyzer.analyzeScenario();

			// Return the results:
			scenarioAnalyzer.createResults(path2EventFile + "_analysisResults.csv", new NoCutter());

		} catch (Exception e){
			e.printStackTrace();
		}
	}
}
