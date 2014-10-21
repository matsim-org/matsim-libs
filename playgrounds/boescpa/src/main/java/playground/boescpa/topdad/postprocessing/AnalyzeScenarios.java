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
		int scaleFactor = 10;

		for (int i = 1; i < args.length; i++) {
			try {
				String path2EventFile = args[i];

				// Analyze the events:
				ScenarioAnalyzerEventHandler[] handlers = {
						new AgentCounter(network),
						new TripAnalyzer(network)
				};
				ScenarioAnalyzer scenarioAnalyzer = new ScenarioAnalyzer(path2EventFile, scaleFactor, handlers);
				scenarioAnalyzer.analyzeScenario();

				// Return the results:
				//	Zurich
				SpatialEventCutter circlePointCutter = new CirclePointCutter(30000, 683518.0, 246836.0); // 30km around Zurich, Bellevue
				scenarioAnalyzer.createResults(path2EventFile + "_analysisResults_Zurich.csv", circlePointCutter);
				/*
				//	Geneva
				SpatialEventCutter circlePointCutter = new CirclePointCutter(30000, 683518.0, 246836.0); // 30km around Zurich, Bellevue
				scenarioAnalyzer.createResults(path2EventFile + "_analysisResults_Geneva.csv", circlePointCutter);
				//	Lausanne
				SpatialEventCutter circlePointCutter = new CirclePointCutter(30000, 683518.0, 246836.0); // 30km around Zurich, Bellevue
				scenarioAnalyzer.createResults(path2EventFile + "_analysisResults_Lausanne.csv", circlePointCutter);
				//	Bern
				SpatialEventCutter circlePointCutter = new CirclePointCutter(30000, 683518.0, 246836.0); // 30km around Zurich, Bellevue
				scenarioAnalyzer.createResults(path2EventFile + "_analysisResults_Bern.csv", circlePointCutter);
				//	Basel
				SpatialEventCutter circlePointCutter = new CirclePointCutter(30000, 683518.0, 246836.0); // 30km around Zurich, Bellevue
				scenarioAnalyzer.createResults(path2EventFile + "_analysisResults_Basel.csv", circlePointCutter);
				//	StGallen
				SpatialEventCutter circlePointCutter = new CirclePointCutter(30000, 683518.0, 246836.0); // 30km around Zurich, Bellevue
				scenarioAnalyzer.createResults(path2EventFile + "_analysisResults_StGallen.csv", circlePointCutter);
				//	Luzern
				SpatialEventCutter circlePointCutter = new CirclePointCutter(30000, 683518.0, 246836.0); // 30km around Zurich, Bellevue
				scenarioAnalyzer.createResults(path2EventFile + "_analysisResults_Luzern.csv", circlePointCutter);
				//	Lugano
				SpatialEventCutter circlePointCutter = new CirclePointCutter(30000, 683518.0, 246836.0); // 30km around Zurich, Bellevue
				scenarioAnalyzer.createResults(path2EventFile + "_analysisResults_Lugano.csv", circlePointCutter);
				*/
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}

}
