/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package playground.pieter.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;

/**
 * @author fouriep, wrashid
 *         <P>
 *         track the average density and flow inside a link
 * 
 */
class FlowAndDensityCollector implements LinkLeaveEventHandler, LinkEnterEventHandler, PersonArrivalEventHandler, Wait2LinkEventHandler {
	private final int binSizeInSeconds; // set the length of interval
	private HashMap<Id, int[]> linkOutFlow; // define
	private HashMap<Id, int[]> linkInFlow;
	// store the times when thenumber of vehicles on each link changes,
	// along with the number of vehicles at that point in time
	private HashMap<Id, TreeMap<Integer, Integer>> deltaFlow;
	// average the flows from the deltaflow map for each time bin
	private HashMap<Id, double[]> avgDeltaFlow;
	private final Map<Id, ? extends Link> filteredEquilNetLinks; // define
	private int lastBinIndex = 0;

	// personId, linkId
	private final HashMap<Id, Id> lastEnteredLink = new HashMap<>(); // define

	public FlowAndDensityCollector(Map<Id, ? extends Link> filteredEquilNetLinks, int binSizeInSeconds) {
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
	}

	@Override
	public void reset(int iteration) {
		linkOutFlow = new HashMap<>(); // reset the variables (private
		linkInFlow = new HashMap<>();
		deltaFlow = new HashMap<>();
		avgDeltaFlow = new HashMap<>();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) { // call from
													// NetworkReadExample
		linkLeave(event.getLinkId(), event.getTime());
	}

	private void linkLeave(Id linkId, double time) {
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}

		int[] outBins = linkOutFlow.get(linkId);
		int[] inBins = linkInFlow.get(linkId);

		int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));

		if (time < 86400) {
			outBins[binIndex] = outBins[binIndex] + 1; // count the number of
														// agents
			if (binIndex == lastBinIndex) {
				int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
				deltaFlow.get(linkId).put((int) time, lastDelta - 1);
			} else {
				calculateAverageDeltaFlowsAndReinitialize(time, lastBinIndex);
				lastBinIndex = binIndex;
				int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
				deltaFlow.get(linkId).put((int) time, lastDelta - 1);
			}
		}

	}

	public void handleEvent(Wait2LinkEvent event) {
		enterLink(event.getLinkId(), event.getTime());
	}

	private void enterLink(Id linkId, double time) {
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}

		if (!linkInFlow.containsKey(linkId)) {
			// size the array to number of intervals
			linkInFlow.put(linkId, new int[(86400 / binSizeInSeconds) + 1]);
			linkOutFlow.put(linkId, new int[(86400 / binSizeInSeconds) + 1]);
			avgDeltaFlow.put(linkId, new double[(86400 / binSizeInSeconds) + 1]);
			// start tracking the number of vehicles on this link
			deltaFlow.put(linkId, new TreeMap<Integer, Integer>());
			TreeMap<Integer, Integer> df = deltaFlow.get(linkId);
			df.put(0, 0); // assume all links start empty

		}

		int[] inBins = linkInFlow.get(linkId);
		int[] outBins = linkOutFlow.get(linkId);

		int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));

		if (time < 86400) {
			// increment the number of vehicles entering this link
			inBins[binIndex] = inBins[binIndex] + 1;
			if (binIndex == lastBinIndex) {
				int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
				deltaFlow.get(linkId).put((int) time, lastDelta + 1);
			} else {
				calculateAverageDeltaFlowsAndReinitialize(time, lastBinIndex);
				lastBinIndex = binIndex;
				int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
				deltaFlow.get(linkId).put((int) time, lastDelta + 1);
			}
		}
	}

	private void calculateAverageDeltaFlowsAndReinitialize(double time, int binIndex) {

		for (Id id : deltaFlow.keySet()) {
			ArrayList<Integer> times = new ArrayList<>();
			times.addAll(deltaFlow.get(id).keySet());
			Collections.sort(times);
			int endTime = (int) time - 1; // the bin ends one second before the
											// start of the new bin, which is
											// the only time this method gets
											// called
			double weightedLevel = 0;
			int lastDelta = 0;
			for (int t = times.size() - 1; t >= 0; t--) {
				lastDelta = deltaFlow.get(id).get(times.get(t));
				weightedLevel += lastDelta * (endTime - times.get(t));
				endTime = times.get(t);
			}
			avgDeltaFlow.get(id)[binIndex] = weightedLevel / (double) binSizeInSeconds;
			// store the last delta and initialize the new structure with this
			// value
			lastDelta = deltaFlow.get(id).floorEntry((int) time).getValue();
			deltaFlow.get(id).clear();
			deltaFlow.get(id).put((int) time, lastDelta);
		}

	}

	public HashMap<Id, int[]> getLinkOutFlow() {
		return linkOutFlow;
	}

	public HashMap<Id, int[]> getLinkInFlow() {
		return linkInFlow;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (lastEnteredLink.containsKey(event.getPersonId()) && lastEnteredLink.get(event.getPersonId()) != null) {
			if (lastEnteredLink.get(event.getPersonId()).equals(event.getLinkId())) {
				linkLeave(event.getLinkId(), event.getTime());
				lastEnteredLink.put(event.getPersonId(), null); // reset value
			}

		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		lastEnteredLink.put(event.getDriverId(), event.getLinkId());
		enterLink(event.getLinkId(), event.getTime());
	}

	public HashMap<Id, double[]> getAvgDeltaFlow() {
		return avgDeltaFlow;
	}

}
