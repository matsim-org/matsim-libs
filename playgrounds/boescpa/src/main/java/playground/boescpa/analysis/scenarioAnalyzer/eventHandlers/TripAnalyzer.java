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

package playground.boescpa.analysis.scenarioAnalyzer.eventHandlers;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import playground.boescpa.analysis.scenarioAnalyzer.ScenarioAnalyzer;
import playground.boescpa.analysis.scenarioAnalyzer.spatialEventCutters.SpatialEventCutter;
import playground.boescpa.analysis.trips.tripCreation.TripHandler;
import playground.boescpa.analysis.trips.tripCreation.TripProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Decorates playground.boescpa.analysis.trips.tripCreation.TripHandler with the method createResults(..).
 *
 * Returns for every mode:
 *	o	Number of trips []
 *	o	Total distance travelled [km]
 *	o	Mean and standard deviation of distances travelled [km]
 *	o	Total duration [min]
 *	o	Mean and standard deviation of durations [min]
 *
 * @author boescpa
 */
public class TripAnalyzer implements ScenarioAnalyzerEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler,
		ActivityStartEventHandler, PersonStuckEventHandler, LinkLeaveEventHandler {

	private static Logger log = Logger.getLogger(TripAnalyzer.class);

	private final TripHandler tripHandler;
	private final Network network;
	private int scaleFactor = 1;
	private Map<String, ModeResult> modes = new HashMap<>();
	private Map<String, ActivityResult> activities = new HashMap<>();
	private SpatialEventCutter spatialEventCutter = null;

	public TripAnalyzer(Network network) {
		this.tripHandler = new TripHandler();
		this.network = network;
		this.reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.tripHandler.reset(0);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.tripHandler.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.tripHandler.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.tripHandler.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.tripHandler.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.tripHandler.handleEvent(event);
	}

	/**
	 * * Returns for every mode and activity:
	 *	o	Number of trips []
	 *	o	Total distance travelled [km]
	 *	o	Mean and standard deviation of distances travelled [km]
	 *	o	Total duration [min]
	 *	o	Mean and standard deviation of durations [min]
	 *
	 * @param spatialEventCutter
	 * @return A multiline String containing the above listed results.
	 */
	@Override
	public String createResults(SpatialEventCutter spatialEventCutter, int scaleFactor) {
		this.scaleFactor = scaleFactor;
		// reset the analysis
		this.modes.clear();
		this.activities.clear();
		// analyze
		analyzeEvents(spatialEventCutter);
		// create results
		String results = getTripResults();
		results += ScenarioAnalyzer.NL;
		results += getActivityResults();
		return results;
	}

	private String getTripResults() {
		/*String results = "Mode; NumberOfTrips; TotalDuration; MeanDuration; StdDevDuration; TotalDistance; MeanDistance; StdDevDistance" + ScenarioAnalyzer.NL;
		for (String mode : modes.keySet()) {
			Double[] modeVals = modes.get(mode).getModeVals();
			if (mode.contains("pt")) {
				correctForAdditionalDistancePT(modeVals);
			}
			results += mode + ScenarioAnalyzer.DEL;
			for (int i = 0; i < (modeVals.length - 1); i++) {
				results += modeVals[i] + ScenarioAnalyzer.DEL;
			}
			results += modeVals[modeVals.length - 1] + ScenarioAnalyzer.NL;
		}
		return results;*/
		String results = "Mode; NumberOfTrips; MeanDuration [min]; MeanDistance [km];" + ScenarioAnalyzer.NL;
		for (String mode : modes.keySet()) {
			Double[] modeVals = modes.get(mode).getModeVals();
			if (mode.contains("pt")) {
				correctForAdditionalDistancePT(modeVals);
			}
			results += mode + ScenarioAnalyzer.DEL;
			results += modeVals[0] + ScenarioAnalyzer.DEL;
			results += modeVals[2] + ScenarioAnalyzer.DEL;
			results += modeVals[5] + ScenarioAnalyzer.NL;
		}
		return results;
	}

	private void correctForAdditionalDistancePT(Double[] modeVals) {
		for (int i = 4; i < 7; i++) {
			modeVals[i] *= 1.2;
		}
	}

	private String getActivityResults() {
		/*String results = "Activity; NumberOfExecutions; TotalDuration; MeanDuration; StdDevDuration" + ScenarioAnalyzer.NL;
		for (String activity : activities.keySet()) {
			Double[] actVals = activities.get(activity).getActVals();
			results += activity + ScenarioAnalyzer.DEL;
			for (int i = 0; i < (actVals.length - 1); i++) {
				results += actVals[i] + ScenarioAnalyzer.DEL;
			}
			results += actVals[actVals.length - 1] + ScenarioAnalyzer.NL;
		}
		return results;*/
		String results = "Activity; NumberOfExecutions; MeanDuration [hr];" + ScenarioAnalyzer.NL;
		for (String activity : activities.keySet()) {
			Double[] actVals = activities.get(activity).getActVals();
			results += activity + ScenarioAnalyzer.DEL;
			results += actVals[0] + ScenarioAnalyzer.DEL;
			results += (actVals[2]/60) + ScenarioAnalyzer.NL;
		}
		return results;
	}

	private void analyzeEvents(SpatialEventCutter spatialEventCutter) {
		if (spatialEventCutter != null) {
			this.spatialEventCutter = spatialEventCutter;
		} else {
			log.warn("No spatial event cutter provided. Will analyze the full network.");
		}

		for (Id personId : tripHandler.getStartLink().keySet()) {
			if (!personId.toString().contains("pt")) {
				ArrayList<Id> startLinks = tripHandler.getStartLink().getValues(personId);
				ArrayList<String> modes = tripHandler.getMode().getValues(personId);
				ArrayList<String> purposes = tripHandler.getPurpose().getValues(personId);
				ArrayList<Double> startTimes = tripHandler.getStartTime().getValues(personId);
				ArrayList<Id> endLinks = tripHandler.getEndLink().getValues(personId);
				ArrayList<Double> endTimes = tripHandler.getEndTime().getValues(personId);
				ArrayList<LinkedList<Id>> pathList = tripHandler.getPath().getValues(personId);

				// Trip analysis:
				/*for (int i = 0; i < startLinks.size(); i++) {
					if (endLinks.get(i) != null) {
						if (considerLink(startLinks.get(i)) || considerLink(endLinks.get(i))) {
							ModeResult modeVals = getMode(modes.get(i));
							modeVals.numberOfTrips++;
							modeVals.modeDistances.add((double) TripProcessor.calcTravelDistance(pathList.get(i), network, startLinks.get(i), endLinks.get(i)));
							modeVals.modeDurations.add(TripProcessor.calcTravelTime(startTimes.get(i), endTimes.get(i)));
						}
					}
				}*/
				for (int i = 0; i < startLinks.size(); i++) {
					if (endLinks.get(i) != null) {
						if ((considerLink(startLinks.get(i)) || considerLink(endLinks.get(i))) && startTimes.get(i) < 86400) {
							String mode = modes.get(i);
							if (mode.equals("bike") || mode.equals("walk")) {
								mode = "slow_mode";
							} else if (mode.equals("transit_walk")) {
								mode = "pt";
							}
							ModeResult modeVals = getMode(mode);
							modeVals.numberOfTrips++;
							modeVals.modeDistances.add((double) TripProcessor.calcTravelDistance(pathList.get(i), network, startLinks.get(i), endLinks.get(i)));
							modeVals.modeDurations.add(TripProcessor.calcTravelTime(startTimes.get(i), endTimes.get(i)));
						}
					}
				}

				// Activity analysis:
				/*ActivityResult actVals;
				if (considerLink(startLinks.get(0))) {
					actVals = getActivity("h");
					actVals.numberOfActivities++;
					actVals.actDurations.add(startTimes.get(0));
				}
				for (int i = 1; i < startTimes.size(); i++) {
					if (endLinks.get(i) != null && considerLink(endLinks.get(i))) {
						actVals = getActivity(purposes.get(i - 1));
						actVals.numberOfActivities++;
						actVals.actDurations.add(startTimes.get(i) - endTimes.get(i - 1));
					}
				}
				if (endLinks.get(endLinks.size()-1) != null && endTimes.get(endTimes.size()-1) < 24*60*60 && considerLink(endLinks.get(endLinks.size()-1))) {
					actVals = getActivity("h");
					actVals.numberOfActivities++;
					actVals.actDurations.add(24*60*60 - endTimes.get(endTimes.size()-1));
				}*/
				ActivityResult actVals;
				if (considerLink(startLinks.get(0))) {
					actVals = getActivity("h");
					actVals.numberOfActivities++;
					actVals.actDurations.add(startTimes.get(0));
				}
				int i = 1;
				while (i < startTimes.size() && startTimes.get(i) < 86400) {
					if (endLinks.get(i-1) != null && considerLink(endLinks.get(i-1))) {
						actVals = getActivity(purposes.get(i-1));
						actVals.numberOfActivities++;
						actVals.actDurations.add(startTimes.get(i) - endTimes.get(i-1));
					}
					i++;
				}
				if (i == startTimes.size() || endTimes.get(i-1) < 86400) {
					if (endLinks.get(i-1) != null && considerLink(endLinks.get(i-1))) {
						actVals = getActivity(purposes.get(i-1));
						actVals.numberOfActivities++;
						actVals.actDurations.add(86400 - endTimes.get(i-1));
					}
				}
			}
		}
	}

	private boolean considerLink(Id id) {
		return spatialEventCutter == null || spatialEventCutter.spatiallyConsideringLink(network.getLinks().get(id));
	}

	private ModeResult getMode(String mode) {
		ModeResult modeResult = this.modes.get(mode);
		if (modeResult == null) {
			modeResult = new ModeResult();
			this.modes.put(mode, modeResult);
		}
		return modeResult;
	}

	private class ModeResult {
		public ArrayList<Double> modeDistances = new ArrayList<>();
		public ArrayList<Double> modeDurations = new ArrayList<>();
		public double numberOfTrips = 0;

		/**
		 * @return NumberOfTrips; TotalDuration; MeanDuration; StdDevDuration; TotalDistance; MeanDistance; StdDevDistance
		 */
		public Double[] getModeVals() {
			Double[] modeVals = new Double[7];

			// Number of Trips:
			modeVals[0] = numberOfTrips;

			// Duration [min]: Total, Mean and StdDev
			modeVals[1] = total(modeDurations);
			modeVals[2] = modeVals[1]/modeVals[0];
			modeVals[3] = stddev(modeDurations, modeVals[2]);
			for (int i = 1; i < 4; i++) {
				modeVals[i] /= 60;
			}

			// Distance [km]: Total, Mean and StdDev
			modeVals[4] = total(modeDistances);
			modeVals[5] = modeVals[4]/modeVals[0];
			modeVals[6] = stddev(modeDistances, modeVals[5]);
			for (int i = 4; i < 7; i++) {
				modeVals[i] /= 1000;
			}

			// Scale mode vals:
			modeVals[0] *= scaleFactor;
			modeVals[1] *= scaleFactor;
			modeVals[4] *= scaleFactor;

			return modeVals;
		}
	}

	private ActivityResult getActivity(String activity) {
		String act = activity.substring(0,1);
		ActivityResult activityResult = this.activities.get(act);
		if (activityResult == null) {
			activityResult = new ActivityResult();
			this.activities.put(act, activityResult);
		}
		return activityResult;
	}

	private class ActivityResult {
		public ArrayList<Double> actDurations = new ArrayList<>();
		public double numberOfActivities = 0;

		/**
		 * @return NumberOfExecutions; TotalDuration; MeanDuration; StdDev Duration
		 */
		public Double[] getActVals() {
			Double[] actVals = new Double[4];

			// Number of Trips:
			actVals[0] = numberOfActivities;

			// Duration [min]: Total, Mean and StdDev
			actVals[1] = total(actDurations);
			actVals[2] = actVals[1]/actVals[0];
			actVals[3] = stddev(actDurations, actVals[2]);
			for (int i = 1; i < 4; i++) {
				actVals[i] /= 60;
			}

			// Scale act vals:
			actVals[0] *= scaleFactor;
			actVals[1] *= scaleFactor;

			return actVals;
		}
	}

	private double stddev(ArrayList<Double> allVals, double mean) {
		double var = 0;
		for (double val : allVals) {
			var += (val-mean)*(val-mean);
		}
		return Math.sqrt(var/allVals.size());
	}

	private double total(ArrayList<Double> allVals) {
		double total = 0;
		for (double val : allVals) {
			total += val;
		}
		return total;
	}
}
