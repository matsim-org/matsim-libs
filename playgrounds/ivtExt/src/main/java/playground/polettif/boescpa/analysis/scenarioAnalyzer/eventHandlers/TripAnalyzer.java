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

package playground.polettif.boescpa.analysis.scenarioAnalyzer.eventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import playground.polettif.boescpa.analysis.scenarioAnalyzer.ScenarioAnalyzer;
import playground.polettif.boescpa.analysis.spatialCutters.SpatialCutter;
import playground.polettif.boescpa.analysis.trips.Trip;
import playground.polettif.boescpa.analysis.trips.TripEventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Decorates playground.polettif.boescpa.analysis.trips.TripHandler with the method createResults(..).
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
public class TripAnalyzer extends ScenarioAnalyzerEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		ActivityStartEventHandler, PersonStuckEventHandler, LinkLeaveEventHandler {

	private final TripEventHandler tripHandler;
	private final Network network;
	private int scaleFactor = 1;
	private Map<String, ModeResult> modes = new HashMap<>();
	private Map<String, ActivityResult> activities = new HashMap<>();
	private SpatialCutter spatialEventCutter = null;

	public TripAnalyzer(Network network) {
		this.tripHandler = new TripEventHandler(network);
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
	 * @param spatialEventCutter Defining the area to analyze.
	 * @return A multiline String containing the above listed results.
	 */
	@Override
	public String createResults(SpatialCutter spatialEventCutter, int scaleFactor) {
		this.scaleFactor = scaleFactor;
		// reset the analysis
		this.modes.clear();
		this.activities.clear();
		// analyze
        this.spatialEventCutter = spatialEventCutter;
		analyzeEvents();
		// create results
		String results = getTripResults();
		results += ScenarioAnalyzer.NL;
		results += getActivityResults();
		return results;
	}

	private String getTripResults() {
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
		String results = "Activity; NumberOfExecutions; MeanDuration [hr];" + ScenarioAnalyzer.NL;
		for (String activity : activities.keySet()) {
			Double[] actVals = activities.get(activity).getActVals();
			results += activity + ScenarioAnalyzer.DEL;
			results += actVals[0] + ScenarioAnalyzer.DEL;
			results += (actVals[2]/60) + ScenarioAnalyzer.NL;
		}
		return results;
	}

	private void analyzeEvents() {
        Map<Id<Person>, Double> agentCurrentActStartTime = new HashMap<>();
        Map<Id<Person>, String> agentCurrentActPurpose = new HashMap<>();
        ActivityResult actVals;
        for (Trip trip : tripHandler.getTrips()) {
            // add trip to mode stats
            if ((considerLink(trip.startLinkId) || considerLink(trip.endLinkId)) && trip.startTime < ANALYSIS_END_TIME) {
                String mode = trip.mode;
                if (mode.equals("bike") || mode.equals("walk")) {
                    mode = "slow_mode";
                } else if (mode.equals("transit_walk")) {
                    mode = "pt";
                }
                ModeResult modeVals = getMode(mode);
                modeVals.numberOfTrips++;
                modeVals.modeDistances.add(trip.distance);
                modeVals.modeDurations.add(trip.duration);
            }
            // add trip to act stats
            double actStartTime, actEndTime;
            if (considerLink(trip.startLinkId)) {
                if (!agentCurrentActStartTime.containsKey(trip.agentId)) {
                    actVals = getActivity("h");
                    actStartTime = 0;
                } else {
                    actVals = getActivity(agentCurrentActPurpose.get(trip.agentId));
                    actStartTime = agentCurrentActStartTime.get(trip.agentId);
                }
                if (trip.startTime < ANALYSIS_END_TIME) {
                    actEndTime = trip.startTime;
                } else {
                    actEndTime = ANALYSIS_END_TIME;
                }
                if (actStartTime < actEndTime) {
                    actVals.numberOfActivities++;
                    actVals.actDurations.add(actEndTime - actStartTime);
                }
            }
            // prepare for next activity as long as start of next activity is < ANALYSIS_END_TIME
            //  (this is independent of inside or outside area)
            agentCurrentActStartTime.put(trip.agentId, trip.endTime);
            agentCurrentActPurpose.put(trip.agentId, trip.purpose);
        }
        // finish the last activities of all agents with last trip.starttime < ANALYSIS_END_TIME
        for (Id<Person> agentId : agentCurrentActStartTime.keySet()) {
            if (agentCurrentActStartTime.get(agentId) < ANALYSIS_END_TIME && agentCurrentActPurpose.get(agentId) != null) {
                actVals = getActivity(agentCurrentActPurpose.get(agentId));
                actVals.numberOfActivities++;
                actVals.actDurations.add(ANALYSIS_END_TIME - agentCurrentActStartTime.get(agentId));
            }
        }
	}

	private boolean considerLink(Id<Link> linkId) {
		return linkId != null && spatialEventCutter.spatiallyConsideringLink(network.getLinks().get(linkId));
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
