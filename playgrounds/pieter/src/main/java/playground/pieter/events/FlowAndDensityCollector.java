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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

/**
 * @author fouriep, wrashid
 *         <P>
 *         track the average density and flow inside a link
 * 
 */
public class FlowAndDensityCollector implements LinkLeaveEventHandler,
		LinkEnterEventHandler, AgentArrivalEventHandler,
		AgentWait2LinkEventHandler {
	private int binSizeInSeconds; // set the length of interval
	private HashMap<Id, int[]> linkOutFlow; // define
	private HashMap<Id, int[]> linkInFlow;
	// store the times when thenumber of vehicles on each link changes,
	// along with the number of vehicles at that point in time
	private HashMap<Id, TreeMap<Integer, Integer>> deltaFlow;
	// average the flows from the deltaflow map for each time bin
	private HashMap<Id, double[]> avgDeltaFlow;
	private Map<Id, ? extends Link> filteredEquilNetLinks; // define
	private int lastBinIndex = 0;

	// personId, linkId
	private HashMap<Id, Id> lastEnteredLink = new HashMap<Id, Id>(); // define

	public FlowAndDensityCollector(
			Map<Id, ? extends Link> filteredEquilNetLinks, int binSizeInSeconds) {
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
	}

	@Override
	public void reset(int iteration) {
		linkOutFlow = new HashMap<Id, int[]>(); // reset the variables (private
		linkInFlow = new HashMap<Id, int[]>();
		deltaFlow = new HashMap<Id, TreeMap<Integer, Integer>>();
		avgDeltaFlow = new HashMap<Id, double[]>();
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

	public void handleEvent(AgentWait2LinkEvent event) {
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
			avgDeltaFlow
					.put(linkId, new double[(86400 / binSizeInSeconds) + 1]);
			// start tracking the number of vehicles on this link
			deltaFlow.put(linkId, new TreeMap<Integer, Integer>());
			TreeMap<Integer, Integer> df = deltaFlow.get(linkId);
			df.put(0, 0); // assume all links start empty

		}

		int[] inBins = linkInFlow.get(linkId);
		int[] outBins = linkOutFlow.get(linkId);

		int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));

		if (time < 86400) {
//			increment the number of vehicles entering this link
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

	private void calculateAverageDeltaFlowsAndReinitialize(double time,
			int binIndex) {

		for (Id id : deltaFlow.keySet()) {
			ArrayList<Integer> times = new ArrayList<Integer>();
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
			}
			avgDeltaFlow.get(id)[binIndex] = weightedLevel
					/ (double) binSizeInSeconds;
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
	public void handleEvent(AgentArrivalEvent event) {
		if (lastEnteredLink.containsKey(event.getPersonId())
				&& lastEnteredLink.get(event.getPersonId()) != null) {
			if (lastEnteredLink.get(event.getPersonId()).equals(
					event.getLinkId())) {
				linkLeave(event.getLinkId(), event.getTime());
				lastEnteredLink.put(event.getPersonId(), null); // reset value
			}

		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		lastEnteredLink.put(event.getPersonId(), event.getLinkId());
		enterLink(event.getLinkId(), event.getTime());
	}

	public HashMap<Id, double[]> getAvgDeltaFlow() {
		return avgDeltaFlow;
	}

	// class TimeAndLevel {
	// private LinkedList<Integer> times;
	// public TimeAndLevel(int key, int value) {
	// super();
	// this.times = new LinkedList<Integer>();
	// this.values = new LinkedList<Integer>();
	// times.add(key);
	// values.add(value);
	// }
	//
	// private LinkedList<Integer> values;
	//
	// public void put(int key, int value) {
	// try {
	// int idx = times.indexOf(key);
	// values.set(idx, value);
	// } catch (Exception e) {
	// times.add(key);
	// values.add(value);
	//
	// }
	// }
	//
	// public int get(int key) {
	// int idx = times.indexOf(key);
	// return values.get(idx);
	// }
	//
	// public LinkedList<Integer> getTimes() {
	// return times;
	// }
	//
	// public LinkedList<Integer> getValues() {
	// return values;
	// }
	// }
}
