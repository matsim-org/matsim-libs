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

package playground.polettif.boescpa.analysis.scenarioAnalyzer.eventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import playground.polettif.boescpa.analysis.scenarioAnalyzer.ScenarioAnalyzer;
import playground.polettif.boescpa.analysis.spatialCutters.SpatialCutter;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates an MFD for the events file.
 *
 * @author boescpa
 */
public class MFDCreator extends ScenarioAnalyzerEventHandler implements LinkLeaveEventHandler,
		LinkEnterEventHandler, PersonArrivalEventHandler {

	private static final int TIMEBINSIZE = 5*60; // [sec]
	private static final boolean EXCLUDEPT = true;

	private int currentTimeBin;
	private Map<Integer, Map<Id<Link>, Integer>> densities;
	private Map<Id<Link>, Integer> densityTimeBin;
	private Map<Integer, Map<Id<Link>, Integer>> flows;
	private Map<Id<Link>, Integer> flowTimeBin;

	private final Network network;

	public MFDCreator(Network network) {
		this.network = network;
		reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.currentTimeBin = 0;
		this.densities = new HashMap<>();
		this.densityTimeBin = new HashMap<>();
		this.densities.put(0, this.densityTimeBin);
		this.flows = new HashMap<>();
		this.flowTimeBin = new HashMap<>();
		this.flows.put(0, this.flowTimeBin);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (!EXCLUDEPT || !event.getVehicleId().toString().contains(TransportMode.pt)) {
			incDensity(event.getLinkId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (!EXCLUDEPT || !event.getVehicleId().toString().contains(TransportMode.pt)) {
			decDensity(event.getLinkId(), event.getTime());
			incFlow(event.getLinkId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!EXCLUDEPT || !event.getPersonId().toString().contains(TransportMode.pt)) {
			decDensity(event.getLinkId(), event.getTime());
		}
	}

	@Override
	public String createResults(SpatialCutter spatialEventCutter, int scaleFactor) {
		String timebinString = "time bins" + ScenarioAnalyzer.DEL;
		String densityString = "densities" + ScenarioAnalyzer.DEL;
		String flowString = "flows" + ScenarioAnalyzer.DEL;
		for (int i = 0; i <= currentTimeBin; i++) {
			timebinString = timebinString + i + ScenarioAnalyzer.DEL;
			// density:
			int densitiesTimeBin = 0;
			for (Id<Link> linkId : this.densities.get(i).keySet()) {
				if (spatialEventCutter.spatiallyConsideringLink(this.network.getLinks().get(linkId))) {
					densitiesTimeBin += this.densities.get(i).get(linkId);
				}
			}
			densityString = densityString + (densitiesTimeBin*scaleFactor) + ScenarioAnalyzer.DEL;
			// flow:
			int flowsTimeBin = 0;
			for (Id<Link> linkId : this.flows.get(i).keySet()) {
				if (spatialEventCutter.spatiallyConsideringLink(this.network.getLinks().get(linkId))) {
					flowsTimeBin += this.flows.get(i).get(linkId);
				}
			}
			flowString = flowString + (flowsTimeBin*scaleFactor) + ScenarioAnalyzer.DEL;
		}
		return timebinString + ScenarioAnalyzer.NL
				+ densityString + ScenarioAnalyzer.NL
				+ flowString + ScenarioAnalyzer.NL;
	}

	private void incDensity(Id<Link> linkId, double time) {
		// get time bin:
        getTimeBin(time);

		// inc density on link:
        incStat(linkId, densityTimeBin);
	}

	private void decDensity(Id<Link> linkId, double time) {
		// get time bin:
        getTimeBin(time);

		// dec density on link:
		Integer newLinkDensity = densityTimeBin.get(linkId);
		if (newLinkDensity != null) {
			newLinkDensity = newLinkDensity - 1;
		} else {
			newLinkDensity = 0;
		}
		if (newLinkDensity == 0) {
			densityTimeBin.remove(linkId);
		} else {
			densityTimeBin.put(linkId, newLinkDensity);
		}
	}

	private void incFlow(Id<Link> linkId, double time) {
		// get time bin:
        getTimeBin(time);

		// inc flow on link:
        incStat(linkId, flowTimeBin);
	}

    private void incStat(Id<Link> linkId, Map<Id<Link>, Integer> timeBin) {
        int newLinkValue = 1;
        if (timeBin.keySet().contains(linkId)) {
            newLinkValue = timeBin.get(linkId) + 1;
        }
        timeBin.put(linkId, newLinkValue);
    }

    private void getTimeBin(double time) {
        int timeBin = (int) Math.floor(time/TIMEBINSIZE);
        while (currentTimeBin < timeBin) {
            incTimeBin();
        }
    }

    private void incTimeBin() {
		currentTimeBin++;
		// new densityTimeBin:
		densityTimeBin = new HashMap<>();
		densityTimeBin.putAll(densities.get(currentTimeBin - 1));
		densities.put(currentTimeBin, densityTimeBin);
		// new flowTimeBin:
		flowTimeBin = new HashMap<>();
		flows.put(currentTimeBin, flowTimeBin);
	}
}
