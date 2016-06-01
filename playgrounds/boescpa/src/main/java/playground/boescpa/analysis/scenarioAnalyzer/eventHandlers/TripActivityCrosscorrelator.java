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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import playground.boescpa.analysis.scenarioAnalyzer.ScenarioAnalyzer;
import playground.boescpa.analysis.spatialCutters.SpatialCutter;
import playground.boescpa.analysis.trips.Trip;
import playground.boescpa.analysis.trips.TripEventHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Decorates playground.boescpa.analysis.trips.TripHandler with the method createResults(..).
 *
 * Returns for every mode:
 *	o	Number of trips [] to reach every activity.
 *
 * @author boescpa
 */
public class TripActivityCrosscorrelator extends ScenarioAnalyzerEventHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		ActivityStartEventHandler, PersonStuckEventHandler, LinkLeaveEventHandler {

	private final TripEventHandler tripHandler;
	private final Network network;
	private int scaleFactor = 1;
	private List<String> modes = new ArrayList<>();
	private List<String> activities = new ArrayList<>();
	private List<List<Integer>> counts = new ArrayList<>(); // outer field: modes, inner field: activities
	private SpatialCutter spatialEventCutter = null;

	public TripActivityCrosscorrelator(Network network) {
		this.tripHandler = new TripEventHandler(network) {
			@Override
			protected boolean agentIsToConsider(Id<Person> personId) {
				return super.agentIsToConsider(personId) && isPersonToConsider(personId);
			}
		};
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
	 * * Returns for every mode:
	 *	o	Number of trips [] to reach every activity
	 *
	 * @param spatialEventCutter Defining the area to analyze.
	 * @param scaleFactor	The number of trips is scaled with this factor before returned.
	 * @return A multiline String containing the above listed results.
	 */
	@Override
	public String createResults(SpatialCutter spatialEventCutter, int scaleFactor) {
		this.scaleFactor = scaleFactor;
		// reset the analysis
		this.modes.clear();
		this.activities.clear();
		this.counts.clear();
		// analyze
        this.spatialEventCutter = spatialEventCutter;
        analyzeEvents();
		// create results
		return getResults();
	}

	private String getResults() {
		// header:
		String results = "Mode/Activity;	";
		for (String act : this.activities) {
			results += act + ScenarioAnalyzer.DEL;
		}
		results += ScenarioAnalyzer.NL;
		// values:
		for (int i = 0; i < this.modes.size(); i++) {
			results += this.modes.get(i) + ScenarioAnalyzer.DEL;
			for (Integer counter : this.counts.get(i)) {
				results += counter + ScenarioAnalyzer.DEL;
			}
			results += ScenarioAnalyzer.NL;
		}
		return results;
	}

	private void analyzeEvents() {
        for (Trip trip : tripHandler.getTrips()) {
            // add trip to mode stats
            if ((considerLink(trip.startLinkId) || considerLink(trip.endLinkId)) && trip.startTime < ANALYSIS_END_TIME) {
                String mode = trip.mode;
                if (mode.equals("bike") || mode.equals("walk")) {
                    mode = "slow_mode";
                } else if (mode.equals("transit_walk")) {
                    mode = "pt";
                }
                addCount(getMode(mode), getActivity(trip.purpose));
            }
        }
	}

	private boolean considerLink(Id<Link> linkId) {
        return linkId != null && spatialEventCutter.spatiallyConsideringLink(network.getLinks().get(linkId));
	}

	private void addCount(int mode, int act) {
		if (mode >= 0 && act >= 0) {
			int counter = this.counts.get(mode).get(act);
			counter += scaleFactor; // = oneOccurence * scaleFactor = 1 * scaleFactor
			this.counts.get(mode).set(act, counter);
		}
	}

	private int getMode(String mode) {
		if (mode != null) {
			int modeResult = this.modes.indexOf(mode);
			if (modeResult == -1) {
				this.modes.add(mode);
				modeResult = this.modes.indexOf(mode);
				// add the new mode to the count fields:
				this.counts.add(new ArrayList<Integer>());
				for (int i = 0; i < this.activities.size(); i++) {
					this.counts.get(this.modes.size() - 1).add(0);
				}
			}
			return modeResult;
		} else {
			return -1;
		}
	}

	private int getActivity(String activity) {
		if (activity != null) {
			String act = activity.substring(0,1);
			int activityResult = this.activities.indexOf(act);
			if (activityResult == -1) {
				this.activities.add(act);
				activityResult = this.activities.indexOf(act);
				// add the new activity to the count fields:
				for (List<Integer> l : this.counts) {
					l.add(0);
				}
			}
			return activityResult;
		} else {
			return -1;
		}
	}
}
