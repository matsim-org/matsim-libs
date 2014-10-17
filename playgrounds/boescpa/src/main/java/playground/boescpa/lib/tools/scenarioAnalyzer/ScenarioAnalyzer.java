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

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers.ScenarioAnalyzerEventHandler;
import playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers.TripAnalyzer;
import playground.boescpa.lib.tools.scenarioAnalyzer.spatialEventCutters.SpatialEventCutter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Analyzes a given events file for given handlers and lets you read out the handlers for several areas.
 *
 * @author boescpa
 */
public class ScenarioAnalyzer {
	private static Logger log = Logger.getLogger(ScenarioAnalyzer.class);

	private final String eventsFile;
	private final ScenarioAnalyzerEventHandler[] scenarioAnalyzerEventHandlers;

	public ScenarioAnalyzer(String eventsFile, ScenarioAnalyzerEventHandler[] scenarioAnalyzerEventHandlers) {
		this.eventsFile = eventsFile;
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

	public void createResults(String pathToResultsFile, SpatialEventCutter spatialEventCutter) {
		Results results = new Results(spatialEventCutter);

		// Ask handlers for the results:
		for (ScenarioAnalyzerEventHandler handler : scenarioAnalyzerEventHandlers) {
			handler.createResults(results, spatialEventCutter);
		}

		showResultsOnTerminal(results);
		writeResultsToFile(pathToResultsFile, results);
	}

	private void showResultsOnTerminal(Results results) {
		log.info(results.toString());
	}

	private void writeResultsToFile(String pathToResultsfile, Results results) {
		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(pathToResultsfile);
			out.write(results.toString());
			out.close();
			log.info("Analysis results written to " + pathToResultsfile);
		} catch (IOException e) {
			log.warn("IOException. Could not write analysis results to file.");
		}
	}

	/**
	 -	For every mode:
	 o	Number of trips []
	 o	Total distance travelled [km]
	 o	Mean and variance of distances travelled [km]
	 o	Total duration [min]
	 o	Mean and variance of durations [min]
	 -	For every activity type:
	 o	Number of times executed []
	 o	Total duration [min]
	 o	Mean and variance of durations [min]
	 -	Number of active agents in the area []
	 */
	public class Results {
		private Map<String, Double[]> modes = new HashMap<>();
		private Map<String, Double[]> activities = new HashMap<>();
		private int numberOfAgents = 0;
		private String analyzedArea;

		public Results(SpatialEventCutter spatialEventCutter) {
			this.analyzedArea = spatialEventCutter.toString();
		}

		public void setNumberOfAgents(int numberOfAgents) {
			this.numberOfAgents = numberOfAgents;
		}

		public void setMode_NumberOfTrips(String mode, double numberOfTrips) {
			Double[] modeVals = getMode(mode);
			modeVals[0] = numberOfTrips;
		}

		/**
		 * All in [km]!
		 *
		 * @param mode
		 * @param totalDistance
		 * @param meanDistance
		 * @param varianceDistance
		 */
		public void setMode_Distance(String mode, double totalDistance, double meanDistance, double varianceDistance) {
			Double[] modeVals = getMode(mode);
			modeVals[1] = totalDistance;
			modeVals[2] = meanDistance;
			modeVals[3] = varianceDistance;
		}

		/**
		 * All in [min]!
		 *
		 * @param mode
		 * @param totalDuration
		 * @param meanDuration
		 * @param varianceDuration
		 */
		public void setMode_Duration(String mode, double totalDuration, double meanDuration, double varianceDuration) {
			Double[] modeVals = getMode(mode);
			modeVals[4] = totalDuration;
			modeVals[5] = meanDuration;
			modeVals[6] = varianceDuration;
		}

		private Double[] getMode(String mode) {
			Double[] modeVals = this.modes.get(mode);
			if (modeVals == null) {
				modeVals = new Double[7];
				this.modes.put(mode, modeVals);
			}
			return modeVals;
		}

		public void setActivities_NumberOfExecutions(String activity, double numberOfExecutions) {
			Double[] actVals = getActivity(activity);
			actVals[0] = numberOfExecutions;
		}

		/**
		 * All in [min]!
		 *
		 * @param activity
		 * @param totalDuration
		 * @param meanDuration
		 * @param varianceDuration
		 */
		public void setActivities_Duration(String activity, double totalDuration, double meanDuration, double varianceDuration) {
			Double[] actVals = getActivity(activity);
			actVals[1] = totalDuration;
			actVals[2] = meanDuration;
			actVals[3] = varianceDuration;
		}

		private Double[] getActivity(String activity) {
			Double[] actVals = this.activities.get(activity);
			if (actVals == null) {
				actVals = new Double[4];
				this.activities.put(activity, actVals);
			}
			return actVals;
		}

		@Override
		public String toString() {
			String nl = "\n";
			String del = "; ";
			String results;

			// Area:
			results = analyzedArea + nl;
			results += nl;

			// Number of Agents:
			results += "Number of Agents: " + this.numberOfAgents + nl;
			results += nl;

			// Modes:
			results += "Mode; NumberOfTrips; TotalDistance; MeanDistance; VarianceDistance; TotalDuration; MeanDuration; Variance Duration" + nl;
			for (String mode : modes.keySet()) {
				Double[] modeVals = modes.get(mode);
				results += mode + del;
				for (int i = 0; i < (modeVals.length - 1); i++) {
					results += modeVals[i] + del;
				}
				results += modeVals[modeVals.length - 1] + nl;
			}
			results += nl;

			// Activities:
			results += "Activity; NumberOfExecutions; TotalDuration; MeanDuration; Variance Duration" + nl;
			for (String activity : activities.keySet()) {
				Double[] actVals = activities.get(activity);
				results += activity + del;
				for (int i = 0; i < (actVals.length - 1); i++) {
					results += actVals[i] + del;
				}
				results += actVals[actVals.length - 1] + nl;
			}
			results += nl;

			return results;
		}
	}
}
