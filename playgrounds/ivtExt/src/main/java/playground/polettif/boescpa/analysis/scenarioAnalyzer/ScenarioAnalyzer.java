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

package playground.polettif.boescpa.analysis.scenarioAnalyzer;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import playground.polettif.boescpa.analysis.scenarioAnalyzer.eventHandlers.ScenarioAnalyzerEventHandler;
import playground.polettif.boescpa.analysis.spatialCutters.SpatialCutter;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Analyzes a given events file for given handlers and lets you read out the handlers for several areas.
 *
 * @author boescpa
 */
public class ScenarioAnalyzer {
	private static Logger log = Logger.getLogger(ScenarioAnalyzer.class);

	public static String NL = "\n";
	public static String DEL = "; ";

	private final String eventsFile;
	private final int scaleFactor;
	private final ScenarioAnalyzerEventHandler[] scenarioAnalyzerEventHandlers;

	public ScenarioAnalyzer(String eventsFile, int scaleFactor, ScenarioAnalyzerEventHandler[] scenarioAnalyzerEventHandlers) {
		this.eventsFile = eventsFile;
		this.scaleFactor = scaleFactor;
		this.scenarioAnalyzerEventHandlers = scenarioAnalyzerEventHandlers;
	}

	public void analyzeScenario() {
		EventsManager eventsManager= EventsUtils.createEventsManager();

		// Add all handlers:
		for (ScenarioAnalyzerEventHandler handler : scenarioAnalyzerEventHandlers) {
			handler.reset(0);
			eventsManager.addHandler(handler);
		}

		// Read the events file:
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFile);
	}

	public void createResults(String pathToResultsFile, SpatialCutter spatialEventCutter) {
		String results = getInitialResultsString(spatialEventCutter) + NL;

		// Ask handlers for the results:
		for (ScenarioAnalyzerEventHandler handler : scenarioAnalyzerEventHandlers) {
			results += handler.createResults(spatialEventCutter, scaleFactor) + NL;
		}

		showResultsOnTerminal(results);
		writeResultsToFile(pathToResultsFile, results);
	}

	private String getInitialResultsString(SpatialCutter spatialEventCutter) {
		return spatialEventCutter.toString() + NL;
	}

	private void showResultsOnTerminal(String results) {
		log.info(results);
	}

	private void writeResultsToFile(String pathToResultsfile, String results) {
		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(pathToResultsfile);
			out.write(results);
			out.close();
			log.info("Analysis results written to " + pathToResultsfile);
		} catch (IOException e) {
			log.warn("IOException. Could not write analysis results to file.");
		}
	}
}
