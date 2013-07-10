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
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;

import playground.pieter.singapore.utils.postgresql.travelcomponents.Journey;
import playground.pieter.singapore.utils.postgresql.travelcomponents.TravellerChain;
import playground.pieter.singapore.utils.postgresql.travelcomponents.Trip;

/**
 * @author fouriep, wrashid
 *         <P>
 *         Track the average density and flow of vehicles and transit passengers
 *         inside a link.
 *         <P>
 *         <strong> For transit scenarios only, separates flows and densities by
 *         mode </strong>
 * 
 */
public class MultiModalFlowAndDensityCollector implements LinkLeaveEventHandler, LinkEnterEventHandler,
		AgentArrivalEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		AgentWait2LinkEventHandler {
	private class PTVehicle {

		// Attributes
		private Id transitLineId;
		private Id transitRouteId;
		boolean in = false;
		private HashSet<Id> passengers = new HashSet<Id>();
		Id lastStop;
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

	}

	// set the length of interval
	private int binSizeInSeconds;
	private HashMap<Id, int[]> linkOutFlowCar;
	private HashMap<Id, int[]> linkInFlowCar;
	// store the times when the number of vehicles on each link changes,
	// along with the number of vehicles at that point in time
	private HashMap<Id, TreeMap<Integer, Integer>> deltaFlowCar;
	// average the flows from the delta flow map for each time bin
	private HashMap<Id, double[]> avgDeltaFlowCar;
	private Map<Id, ? extends Link> filteredEquilNetLinks; // define
	private int lastBinIndex = 0;
	private HashSet<Id> transitDriverIds = new HashSet<Id>();
	private Map<Id, PTVehicle> ptVehicles = new HashMap<Id, PTVehicle>();
	private HashMap<Id, Id> lastEnteredLink = new HashMap<Id, Id>();
	private HashMap<Id, int[]> linkOutFlowPTVehicle;
	private HashMap<Id, int[]> linkInFlowPTVehicle;
	private HashMap<Id, TreeMap<Integer, Integer>> deltaFlowPTVehicle;
	private HashMap<Id, double[]> avgDeltaFlowPTVehicle;
	private HashMap<Id, int[]> linkOutFlowPTPassenger;
	private HashMap<Id, int[]> linkInFlowPTPassenger;
	private HashMap<Id, TreeMap<Integer, Integer>> deltaFlowPTPassenger;
	private HashMap<Id, double[]> avgDeltaFlowPTPassenger;

	public MultiModalFlowAndDensityCollector(Map<Id, ? extends Link> filteredEquilNetLinks, int binSizeInSeconds) {
		this.filteredEquilNetLinks = filteredEquilNetLinks;
		this.binSizeInSeconds = binSizeInSeconds;
	}

	@Override
	public void reset(int iteration) {
		linkOutFlowCar = new HashMap<Id, int[]>(); // reset the variables
													// (private
		linkInFlowCar = new HashMap<Id, int[]>();
		deltaFlowCar = new HashMap<Id, TreeMap<Integer, Integer>>();
		avgDeltaFlowCar = new HashMap<Id, double[]>();

		linkOutFlowPTVehicle = new HashMap<Id, int[]>(); // reset the variables
															// (private
		linkInFlowPTVehicle = new HashMap<Id, int[]>();
		deltaFlowPTVehicle = new HashMap<Id, TreeMap<Integer, Integer>>();
		avgDeltaFlowPTVehicle = new HashMap<Id, double[]>();

		linkOutFlowPTPassenger = new HashMap<Id, int[]>(); // reset the
															// variables
															// (private
		linkInFlowPTPassenger = new HashMap<Id, int[]>();
		deltaFlowPTPassenger = new HashMap<Id, TreeMap<Integer, Integer>>();
		avgDeltaFlowPTPassenger = new HashMap<Id, double[]>();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) { // call from
													// NetworkReadExample
		leaveLink(event.getLinkId(), event.getTime(), ptVehicles.containsKey(event.getVehicleId()),
				event.getVehicleId());
	}

	private void leaveLink(Id linkId, double time, boolean isTransitVehicle, Id vehicleId) {
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}
		boolean processingPassengers = false;
		boolean done = false;
		HashMap<Id, int[]> linkOutFlow;
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
					calculateAverageDeltaFlowsAndReinitialize(time, lastBinIndex, deltaFlow, avgDeltaFlow);
					lastBinIndex = binIndex;
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

	public void handleEvent(AgentWait2LinkEvent event) {
		enterLink(event.getLinkId(), event.getTime(), transitDriverIds.contains(event.getPersonId()), null);
	}

	private void enterLink(Id linkId, double time, boolean isTransitVehicle, Id vehicleId) {
		if (!filteredEquilNetLinks.containsKey(linkId)) {
			return; // if the link is not in the link set, then exit the method
		}
		boolean processingPassengers = false;
		boolean done = false;
		HashMap<Id, int[]> linkOutFlow;
		HashMap<Id, int[]> linkInFlow;
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
				linkInFlow.put(linkId, new int[(86400 / binSizeInSeconds) + 1]);
				linkOutFlow.put(linkId, new int[(86400 / binSizeInSeconds) + 1]);
				avgDeltaFlow.put(linkId, new double[(86400 / binSizeInSeconds) + 1]);
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
					calculateAverageDeltaFlowsAndReinitialize(time, lastBinIndex, deltaFlow, avgDeltaFlow);
					lastBinIndex = binIndex;
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

	private void calculateAverageDeltaFlowsAndReinitialize(double time, int binIndex,
			HashMap<Id, TreeMap<Integer, Integer>> deltaFlow, HashMap<Id, double[]> avgDeltaFlow) {

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

	public HashMap<Id, int[]> getLinkOutFlowCar() {
		return linkOutFlowCar;
	}

	public HashMap<Id, int[]> getLinkInFlowCar() {
		return linkInFlowCar;
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if (lastEnteredLink.containsKey(event.getPersonId()) && lastEnteredLink.get(event.getPersonId()) != null) {
			if (lastEnteredLink.get(event.getPersonId()).equals(event.getLinkId())) {
				leaveLink(event.getLinkId(), event.getTime(), transitDriverIds.contains(event.getPersonId()), null);
				lastEnteredLink.put(event.getPersonId(), null); // reset value
			}

		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		lastEnteredLink.put(event.getPersonId(), event.getLinkId());
		enterLink(event.getLinkId(), event.getTime(), ptVehicles.containsKey(event.getVehicleId()),
				event.getVehicleId());
	}

	public HashMap<Id, double[]> getAvgDeltaFlowCar() {
		return avgDeltaFlowCar;
	}

	public void handleEvent(TransitDriverStartsEvent event) {
		try {
			ptVehicles.put(event.getVehicleId(), new PTVehicle(event.getTransitLineId(), event.getTransitRouteId()));
			transitDriverIds.add(event.getDriverId());
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		try {
			if (transitDriverIds.contains(event.getPersonId()))
				return;
			if (ptVehicles.keySet().contains(event.getVehicleId())) {

				ptVehicles.get(event.getVehicleId()).addPassenger(event.getPersonId());

			}
		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (transitDriverIds.contains(event.getPersonId()))
			return;
		try {
			if (ptVehicles.keySet().contains(event.getVehicleId())) {

				PTVehicle vehicle = ptVehicles.get(event.getVehicleId());
				vehicle.removePassenger(event.getPersonId());

			}

		} catch (Exception e) {
			String fullStackTrace = org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(e);
			System.err.println(fullStackTrace);
			System.err.println(event.toString());
		}
	}
}
