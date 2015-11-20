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
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;

/**
 * @author fouriep, with liberal dollops of code lifted from wrashid's playground :) 
 *         <P>
 *         Track the average density and flow of vehicles and transit passengers
 *         inside a link.
 *         <P>
 *         <strong> For transit scenarios only, separates flows and densities by
 *         mode </strong>
 * 
 */
public class MultiModalFlowAndDensityCollector implements LinkLeaveEventHandler, LinkEnterEventHandler,
		PersonArrivalEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		Wait2LinkEventHandler, TransitDriverStartsEventHandler {
	private class PTVehicle {

		// Attributes
		private final Id transitLineId;
		private final Id transitRouteId;
		boolean in = false;
		private final HashSet<Id> passengers = new HashSet<>();
		Id lastStop;
		private Id lastLink;
		private double linkEnterTime = 0.0;

		// Constructors
		public PTVehicle(Id transitLineId, Id transitRouteId) {
			this.transitLineId = transitLineId;
			this.transitRouteId = transitRouteId;
		}

		public int getNumberOfpassengers() {
			return passengers.size();
		}

		public void addPassenger(Id passengerId) {
			passengers.add(passengerId);
		}

		public void removePassenger(Id passengerId) {
			passengers.remove(passengerId);
		}

		public Id getLastLinkId() {
			return lastLink;
		}

		public void setLastLinkId(Id link) {
			lastLink = link;
		}

	}

	public enum FlowType {
		Car, PTVehicle, PTPassenger
	}

	// set the length of interval
	private final int binSizeInSeconds;
	private final int numberOfTimeBins;
	private HashMap<Id<Link>, int[]> linkOutFlowCar;
	private HashMap<Id<Link>, int[]> linkInFlowCar;
	// store the times when the number of vehicles on each link changes,
	// along with the number of vehicles at that point in time
	private HashMap<Id, TreeMap<Integer, Integer>> deltaFlowCar;
	// average the flows from the delta flow map for each time bin
	private HashMap<Id, double[]> avgDeltaFlowCar;
	private final Map<Id<Link>, ? extends Link> filteredEquilNetLinks; // define
	private int lastBinIndex = 0;
	private final HashMap<Id, Id> transitDriverIdToVehicleId = new HashMap<>();
	private final Map<Id, PTVehicle> ptVehicles = new HashMap<>();
	private final HashMap<Id, Id> lastEnteredLink = new HashMap<>();
	private HashMap<Id<Link>, int[]> linkOutFlowPTVehicle;
	private HashMap<Id<Link>, int[]> linkInFlowPTVehicle;
	private HashMap<Id, TreeMap<Integer, Integer>> deltaFlowPTVehicle;
	private HashMap<Id, double[]> avgDeltaFlowPTVehicle;
	private HashMap<Id<Link>, int[]> linkOutFlowPTPassenger;
	private HashMap<Id<Link>, int[]> linkInFlowPTPassenger;
	private HashMap<Id, TreeMap<Integer, Integer>> deltaFlowPTPassenger;
	private HashMap<Id, double[]> avgDeltaFlowPTPassenger;
	private double lastTime;
	private int enterLinkCount;
	private int leavLinkCount;

	public MultiModalFlowAndDensityCollector(Map<Id<Link>, ? extends Link> filteredEquilNetLinks, int binSizeInSeconds) {
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
		this.numberOfTimeBins = (86400 / binSizeInSeconds) + 1;
	}

	@Override
	public void reset(int iteration) {
		linkOutFlowCar = new HashMap<>(); // reset the variables
													// (private
		linkInFlowCar = new HashMap<>();
		deltaFlowCar = new HashMap<>();
		avgDeltaFlowCar = new HashMap<>();

		linkOutFlowPTVehicle = new HashMap<>(); // reset the variables
															// (private
		linkInFlowPTVehicle = new HashMap<>();
		deltaFlowPTVehicle = new HashMap<>();
		avgDeltaFlowPTVehicle = new HashMap<>();

		linkOutFlowPTPassenger = new HashMap<>(); // reset the
															// variables
															// (private
		linkInFlowPTPassenger = new HashMap<>();
		deltaFlowPTPassenger = new HashMap<>();
		avgDeltaFlowPTPassenger = new HashMap<>();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) { // call from
													// NetworkReadExample
		leaveLink(event.getLinkId(), event.getTime(), ptVehicles.containsKey(event.getVehicleId()),
				event.getVehicleId());
	}

	private void leaveLink(Id linkId, double time, boolean isTransitVehicle, Id vehicleId) {
		this.leavLinkCount++;
		this.lastTime = time;
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}
		boolean processingPassengers = false;
		boolean done = false;
		HashMap<Id<Link>, int[]> linkOutFlow;
		HashMap<Id, int[]> linkInFlow;
		HashMap<Id, TreeMap<Integer, Integer>> deltaFlow;
		HashMap<Id, double[]> avgDeltaFlow;
		while (!done) {
			if (isTransitVehicle) {
				if (!processingPassengers) {
					linkOutFlow = linkOutFlowPTVehicle;
					deltaFlow = deltaFlowPTVehicle;
					avgDeltaFlow = avgDeltaFlowPTVehicle;
				} else {
					linkOutFlow = linkOutFlowPTPassenger;
					deltaFlow = deltaFlowPTPassenger;
					avgDeltaFlow = avgDeltaFlowPTPassenger;
				}

			} else {
				linkOutFlow = linkOutFlowCar;
				deltaFlow = deltaFlowCar;
				avgDeltaFlow = avgDeltaFlowCar;
			}
			int[] outBins = linkOutFlow.get(linkId);

			int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));

			if (time < 86400) {
				// increment the number of vehicles/passengers entering this
				// link
				outBins[binIndex] = processingPassengers ? (outBins[binIndex] + ptVehicles.get(vehicleId)
						.getNumberOfpassengers()) : (outBins[binIndex] + 1);

				if (binIndex == lastBinIndex) {
					int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
					// check if we're processing passengers and remove all the
					// passengers in the vehicle from the link if so, else just
					// remove this vehicle from the link
					deltaFlow.get(linkId).put(
							(int) time,
							processingPassengers ? (lastDelta - ptVehicles.get(vehicleId).getNumberOfpassengers())
									: (lastDelta - 1));
				} else {
					calculateAverageDeltaFlowsAndReinitialize(binIndex);
					int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
					// check if we're processing passengers and remove all the
					// passengers in the vehicle from the link if so, else just
					// remove this vehicle from the link
					deltaFlow.get(linkId).put(
							(int) time,
							processingPassengers ? (lastDelta - ptVehicles.get(vehicleId).getNumberOfpassengers())
									: (lastDelta - 1));

				}
			}
			if (isTransitVehicle) {
				if (!processingPassengers) {
					processingPassengers = true;
				} else {
					done = true;
				}
			} else {
				done = true;
			}
		}

	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		Id vehId = transitDriverIdToVehicleId.get(event.getPersonId());
		if (vehId != null)
			ptVehicles.get(vehId).setLastLinkId(event.getLinkId());
		enterLink(event.getLinkId(), event.getTime(), vehId != null, vehId);
	}

	private void enterLink(Id linkId, double time, boolean isTransitVehicle, Id vehicleId) {
		this.enterLinkCount++;
		this.lastTime = time;
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}
		boolean processingPassengers = false;
		boolean done = false;
		HashMap<Id<Link>, int[]> linkOutFlow;
		HashMap<Id<Link>, int[]> linkInFlow;
		HashMap<Id, TreeMap<Integer, Integer>> deltaFlow;
		HashMap<Id, double[]> avgDeltaFlow;
		while (!done) {
			if (isTransitVehicle) {
				if (!processingPassengers) {
					linkOutFlow = linkOutFlowPTVehicle;
					linkInFlow = linkInFlowPTVehicle;
					deltaFlow = deltaFlowPTVehicle;
					avgDeltaFlow = avgDeltaFlowPTVehicle;
				} else {
					linkOutFlow = linkOutFlowPTPassenger;
					linkInFlow = linkInFlowPTPassenger;
					deltaFlow = deltaFlowPTPassenger;
					avgDeltaFlow = avgDeltaFlowPTPassenger;
				}

			} else {
				linkOutFlow = linkOutFlowCar;
				linkInFlow = linkInFlowCar;
				deltaFlow = deltaFlowCar;
				avgDeltaFlow = avgDeltaFlowCar;
			}
			// this has to run for all
			if (!linkInFlow.containsKey(linkId)) {
				// size the array to number of intervals
				linkInFlow.put(linkId, new int[numberOfTimeBins]);
				linkOutFlow.put(linkId, new int[numberOfTimeBins]);
				avgDeltaFlow.put(linkId, new double[numberOfTimeBins]);
				// start tracking the number of vehicles on this link
				deltaFlow.put(linkId, new TreeMap<Integer, Integer>());
				TreeMap<Integer, Integer> df = deltaFlow.get(linkId);
				df.put(0, 0); // assume all links start empty

			}
			int[] inBins = linkInFlow.get(linkId);

			int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));

			if (time < 86400) {
				// increment the number of vehicles/passengers entering this
				// link
				inBins[binIndex] = processingPassengers ? (inBins[binIndex] + ptVehicles.get(vehicleId)
						.getNumberOfpassengers()) : (inBins[binIndex] + 1);

				if (binIndex == lastBinIndex) {
					int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
					deltaFlow.get(linkId).put(
							(int) time,
							processingPassengers ? (lastDelta + ptVehicles.get(vehicleId).getNumberOfpassengers())
									: (lastDelta + 1));

				} else {
					calculateAverageDeltaFlowsAndReinitialize(binIndex);
					int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
					deltaFlow.get(linkId).put(
							(int) time,
							processingPassengers ? (lastDelta + ptVehicles.get(vehicleId).getNumberOfpassengers())
									: (lastDelta + 1));
				}
			}
			if (isTransitVehicle) {
				if (!processingPassengers) {
					processingPassengers = true;
				} else {
					done = true;
				}
			} else {
				done = true;
			}
		}
	}

	/**
	 * @param linkId
	 * @param time
	 *            <P>
	 *            increments the number of passengers on a link when a person
	 *            boards a pt vehicle
	 */
	private void enterPassenger(Id linkId, double time) {
		this.lastTime = time;
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return;
		}

		HashMap<Id, TreeMap<Integer, Integer>> deltaFlow;
		HashMap<Id, double[]> avgDeltaFlow;

		deltaFlow = deltaFlowPTPassenger;
		avgDeltaFlow = avgDeltaFlowPTPassenger;
		HashMap<Id<Link>, int[]> linkInFlow = linkInFlowPTPassenger;

		int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));
		int[] inBins = linkInFlow.get(linkId);
		if (time < 86400) {
			inBins[binIndex] = inBins[binIndex] + 1;
			if (binIndex == lastBinIndex) {
				int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
				deltaFlow.get(linkId).put((int) time, lastDelta + 1);

			} else {
				calculateAverageDeltaFlowsAndReinitialize(binIndex);
				int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
				deltaFlow.get(linkId).put((int) time, lastDelta + 1);
			}
		}

	}

	/**
	 * @param linkId
	 * @param time
	 *            <P>
	 *            decrements the number of passengers on a link when a person
	 *            exits a transit vehicle
	 */
	private void exitPassenger(Id linkId, double time) {
		this.lastTime = time;
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}

		HashMap<Id, TreeMap<Integer, Integer>> deltaFlow;
		HashMap<Id, double[]> avgDeltaFlow;

		deltaFlow = deltaFlowPTPassenger;
		avgDeltaFlow = avgDeltaFlowPTPassenger;
		HashMap<Id<Link>, int[]> linkOutFlow = linkOutFlowPTPassenger;

		int binIndex = (int) Math.round(Math.floor(time / binSizeInSeconds));
		int[] outBins = linkOutFlow.get(linkId);
		if (time < 86400) {
			// increment the number of vehicles/passengers entering this
			// link
			outBins[binIndex] = outBins[binIndex] + 1;
			if (binIndex == lastBinIndex) {
				int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
				deltaFlow.get(linkId).put((int) time, lastDelta - 1);

			} else {
				calculateAverageDeltaFlowsAndReinitialize(binIndex);
				int lastDelta = deltaFlow.get(linkId).lastEntry().getValue();
				deltaFlow.get(linkId).put((int) time, lastDelta - 1);
			}
		}

	}

	private void calculateAverageDeltaFlowsAndReinitialize(int binIndex) {

		HashMap<Id, TreeMap<Integer, Integer>> deltaFlow = null;
		HashMap<Id, double[]> avgDeltaFlow = null;
		while (lastBinIndex < binIndex) {

			for (FlowType flowType : FlowType.values()) {
				switch (flowType) {
				case Car:
					deltaFlow = deltaFlowCar;
					avgDeltaFlow = avgDeltaFlowCar;
					break;
				case PTPassenger:
					deltaFlow = deltaFlowPTPassenger;
					avgDeltaFlow = avgDeltaFlowPTPassenger;
					break;
				case PTVehicle:
					deltaFlow = deltaFlowPTVehicle;
					avgDeltaFlow = avgDeltaFlowPTVehicle;
					break;
				default:
					break;
				}
				for (Id id : deltaFlow.keySet()) {
					ArrayList<Integer> times = new ArrayList<>();
					times.addAll(deltaFlow.get(id).keySet());
					Collections.sort(times);
					int endTime = (lastBinIndex + 1) * binSizeInSeconds;
					double weightedLevel = 0;
					int lastDelta = 0;
					for (int t = times.size() - 1; t >= 0; t--) {
						lastDelta = deltaFlow.get(id).get(times.get(t));
						weightedLevel += lastDelta * (endTime - times.get(t));
						endTime = times.get(t);
					}
					avgDeltaFlow.get(id)[lastBinIndex] = weightedLevel / binSizeInSeconds;
					// store the last delta and initialize the new structure
					// with
					// this
					// value
					lastDelta = deltaFlow.get(id).lastEntry().getValue();
					deltaFlow.get(id).clear();
					deltaFlow.get(id).put((lastBinIndex + 1) * binSizeInSeconds, lastDelta);
				}
			}
			// complete, update the binindex
			lastBinIndex++;
		}
	}

	public HashMap<Id<Link>, int[]> getLinkOutFlow(FlowType flowType) {
		switch (flowType) {
		case Car:
			return this.linkOutFlowCar;
		case PTVehicle:
			return this.linkOutFlowPTVehicle;
		case PTPassenger:
			return this.linkOutFlowPTPassenger;

		default:
			return null;
		}
	}

	public HashMap<Id<Link>, int[]> getLinkInFlow(FlowType flowType) {
		switch (flowType) {
		case Car:
			return this.linkInFlowCar;
		case PTVehicle:
			return this.linkInFlowPTVehicle;
		case PTPassenger:
			return this.linkInFlowPTPassenger;

		default:
			return null;
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (lastEnteredLink.containsKey(event.getPersonId()) && lastEnteredLink.get(event.getPersonId()) != null) {
			if (lastEnteredLink.get(event.getPersonId()).equals(event.getLinkId())) {

				Id vehId = transitDriverIdToVehicleId.get(event.getPersonId());
				leaveLink(event.getLinkId(), event.getTime(), vehId != null, vehId);

				lastEnteredLink.put(event.getPersonId(), null); // reset value
			}

		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		lastEnteredLink.put(event.getDriverId(), event.getLinkId());
		if (ptVehicles.containsKey(event.getVehicleId()))
			ptVehicles.get(event.getVehicleId()).setLastLinkId(event.getLinkId());

		enterLink(event.getLinkId(), event.getTime(), ptVehicles.containsKey(event.getVehicleId()),
				event.getVehicleId());
	}

	public HashMap<Id, double[]> getAvgDeltaFlow(FlowType flowType) {
		switch (flowType) {
		case Car:
			return this.avgDeltaFlowCar;
		case PTVehicle:
			return this.avgDeltaFlowPTVehicle;
		case PTPassenger:
			return this.avgDeltaFlowPTPassenger;

		default:
			return null;
		}
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		try {
			ptVehicles.put(event.getVehicleId(), new PTVehicle(event.getTransitLineId(), event.getTransitRouteId()));
			transitDriverIdToVehicleId.put(event.getDriverId(), event.getVehicleId());
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		try {
			if (transitDriverIdToVehicleId.keySet().contains(event.getPersonId()))
				return;
			if (ptVehicles.keySet().contains(event.getVehicleId())) {

				ptVehicles.get(event.getVehicleId()).addPassenger(event.getPersonId());
				enterPassenger(ptVehicles.get(event.getVehicleId()).getLastLinkId(), event.getTime());

			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (transitDriverIdToVehicleId.keySet().contains(event.getPersonId()))
			return;
		try {
			if (ptVehicles.keySet().contains(event.getVehicleId())) {

				PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
				vehicle.removePassenger(event.getPersonId());
				exitPassenger(ptVehicles.get(event.getVehicleId()).getLastLinkId(), event.getTime());

			}

		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	public int getNumberOfTimeBins() {
		return numberOfTimeBins;
	}

	public HashMap<Id, double[]> calculateDensity(HashMap<Id, int[]> deltaFlow, Map<Id, ? extends Link> links) {
		// send actual link info.)
		HashMap<Id, double[]> density = new HashMap<>();

		for (Id linkId : deltaFlow.keySet()) {
			density.put(linkId, null);
		}

		for (Id linkId : density.keySet()) {
			int[] deltaflowBins = deltaFlow.get(linkId);// give labels to
														// deltaflowBins
			double[] densityBins = new double[deltaflowBins.length];
			Link link = links.get(linkId);
			densityBins[0] = deltaflowBins[0];
			for (int i = 1; i < deltaflowBins.length; i++) {
				densityBins[i] = (densityBins[i - 1] + deltaflowBins[i]);
			}

			for (int i = 1; i < deltaflowBins.length; i++) {
				densityBins[i] = densityBins[i] / (link.getLength() * link.getNumberOfLanes()) * 1000;
			}

			density.put(linkId, densityBins);
			deltaFlow.remove(linkId);
		}

		return density;
	}

	public HashMap<Id, int[]> calculateOccupancy(HashMap<Id<Link>, int[]> deltaFlow, Map<Id<Link>, ? extends Link> links) {
		// send actual link info.)
		HashMap<Id, int[]> occupancy = new HashMap<>();

		for (Id linkId : deltaFlow.keySet()) {
			occupancy.put(linkId, null);
		}

		for (Id linkId : occupancy.keySet()) {

			int[] deltaflowBins = deltaFlow.get(linkId);// give labels to
														// deltaflowBins
			int[] occupancyBins = new int[deltaflowBins.length];
			Link link = links.get(linkId);
			occupancyBins[0] = deltaflowBins[0];
			for (int i = 1; i < deltaflowBins.length; i++) {
				occupancyBins[i] = (occupancyBins[i - 1] + deltaflowBins[i]);
			}

			occupancy.put(linkId, occupancyBins);
			deltaFlow.remove(linkId);
		}

		return occupancy;
	}

	public int getEnterLinkCount() {
		return enterLinkCount;
	}

	public int getLeavLinkCount() {
		return leavLinkCount;
	}
}
