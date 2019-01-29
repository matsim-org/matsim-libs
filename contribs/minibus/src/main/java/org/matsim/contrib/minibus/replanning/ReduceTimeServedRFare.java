/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.fare.StageContainer;
import org.matsim.contrib.minibus.fare.StageContainerHandler;
import org.matsim.contrib.minibus.fare.TicketMachineI;
import org.matsim.contrib.minibus.genericUtils.RecursiveStatsContainer;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.*;
import java.util.Map.Entry;

/**
 * 
 * Restricts the time of operation to temporal relations higher than a certain threshold.
 * Threshold is standard deviation of number of trips or collected fare of all relations twice a scaling factor.
 * 
 * @author aneumann
 *
 */
public final class ReduceTimeServedRFare extends AbstractPStrategyModule implements StageContainerHandler{
	
	private final static Logger log = Logger.getLogger(ReduceTimeServedRFare.class);
	
	public static final String STRATEGY_NAME = "ReduceTimeServedRFare";

	private final double sigmaScale;
	private final int timeBinSize;
	private final boolean useFareAsWeight;
	private final boolean allowForSplitting;
	
	private LinkedHashMap<Id<TransitRoute>,LinkedHashMap<Integer,LinkedHashMap<Integer,Double>>> route2StartTimeSlot2EndTimeSlot2WeightMap = new LinkedHashMap<>();
	private TicketMachineI ticketMachine;


	public ReduceTimeServedRFare(ArrayList<String> parameter) {
		super();
		if(parameter.size() != 4){
			log.error("Too many parameter. Will ignore: " + parameter);
			log.error("Parameter 1: Scaling factor for sigma");
			log.error("Parameter 2: Time bin size in seconds");
			log.error("Parameter 3: true=use the fare as weight, false=use number of trips as weight");
			log.error("Parameter 4: true=allow for picking one of the demand segments, false=enforce coverage of all demand segments");
		}
		this.sigmaScale = Double.parseDouble(parameter.get(0));
		this.timeBinSize = Integer.parseInt(parameter.get(1));
		this.useFareAsWeight = Boolean.parseBoolean(parameter.get(2));
		this.allowForSplitting = Boolean.parseBoolean(parameter.get(3));
		log.info("enabled");
	}
	
	@Override
	public PPlan run(Operator operator) {
		// get best plans route id
		TransitRoute routeToOptimize = null;
		
		if (operator.getBestPlan().getLine().getRoutes().size() != 1) {
			log.error("There should be only one route at this time - Please check");
		}
		for (TransitRoute route : operator.getBestPlan().getLine().getRoutes().values()) {
			routeToOptimize = route;
		}
		
		Tuple<Double,Double> timeToBeServed = getTimeToBeServed(this.route2StartTimeSlot2EndTimeSlot2WeightMap.get(routeToOptimize.getId()));
		
		if (timeToBeServed == null) {
			// Could not modify plan
			return null;
		}
		
		// profitable route, change startTime
		PPlan newPlan = new PPlan(operator.getNewPlanId(), this.getStrategyName(), operator.getBestPlan().getId());
		newPlan.setNVehicles(1);
		newPlan.setStopsToBeServed(operator.getBestPlan().getStopsToBeServed());
		
		newPlan.setStartTime(timeToBeServed.getFirst());
		newPlan.setEndTime(timeToBeServed.getSecond());
		
		newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
		
		return newPlan;
	}


	private Tuple<Double,Double> getTimeToBeServed(LinkedHashMap<Integer,LinkedHashMap<Integer,Double>> startSlot2EndSlot2TripsMap) {
		RecursiveStatsContainer stats = new RecursiveStatsContainer();
		
		if (startSlot2EndSlot2TripsMap == null) {
			// There is no entry for that particular line - possibly no demand - returning empty line
			return null;
		}
		
		// calculate standard deviation
		for (LinkedHashMap<Integer, Double> EndSlot2TripsMap : startSlot2EndSlot2TripsMap.values()) {
			for (Double trips : EndSlot2TripsMap.values()) {
				stats.handleNewEntry(trips);
			}
		}
		
		if (stats.getNumberOfEntries() == 1) {
			// We use circular routes. There is always a way back (with no demand). Add a second entry.
			stats.handleNewEntry(0.0);			
		}
		
		double sigmaTreshold = stats.getStdDev() * this.sigmaScale;
		Set<Integer> slotsAboveTreshold = new TreeSet<>();
		
		// Get all slots serving a demand above threshold
		for (Entry<Integer, LinkedHashMap<Integer, Double>> endSlot2TripsMapEntry : startSlot2EndSlot2TripsMap.entrySet()) {
			for (Entry<Integer, Double> tripEntry : endSlot2TripsMapEntry.getValue().entrySet()) {
				if (tripEntry.getValue() > sigmaTreshold) {
					// ok - add the corresponding slots to the set
					slotsAboveTreshold.add(endSlot2TripsMapEntry.getKey());
					slotsAboveTreshold.add(tripEntry.getKey());
				}
			}
		}
		
		// Get new slots to be served
		if (allowForSplitting) {
			return this.getSplittedTimeOfOperation(slotsAboveTreshold);
		} else {
			return this.getMaxTimeOfOperation(slotsAboveTreshold);
		}
	}

	private Tuple<Double, Double> getSplittedTimeOfOperation(Set<Integer> slotsAboveTreshold) {
		
		// Sort the time slots
		LinkedList<Integer> sortedSlotsAboveTreshold = new LinkedList<>();
		sortedSlotsAboveTreshold.addAll(slotsAboveTreshold);
		Collections.sort(sortedSlotsAboveTreshold);
		
		// Generate seamlessly connected time slot tuples
		List<Tuple<Integer, Integer>> timeSlotTuples = new LinkedList<>();
		
		int currentStart = -1;
		int currentEnd = -1;
		for (Integer slot : sortedSlotsAboveTreshold) {
			if (currentStart == -1) {
				// a new slot begins
				currentStart = slot;
				currentEnd = currentStart;
			} else {
				// continue with the current slot
				// it's an old time slot which needs to be enlarged or completed
				if (slot == currentEnd + 1) {
					// this timeslot can be enlarged
					currentEnd = slot;
				} else {
					// need to finish this one and start a new one
					if (currentEnd == currentStart) {
						// end time is not allowed to be the same as start time
						currentEnd++;
					}
					timeSlotTuples.add(new Tuple<>(currentStart, currentEnd));
					currentStart = slot;
					currentEnd = currentStart;
				}
			}
		}

		// complete the last one
		if (currentEnd == currentStart) {
			// end time is not allowed to be the same as start time
			currentEnd++;
		}
		timeSlotTuples.add(new Tuple<>(currentStart, currentEnd));
		
		// Get total weight of all time slot tuples
		double totalWeight = 0.0;
		for (Tuple<Integer, Integer> tuple : timeSlotTuples) {
			int weight = tuple.getSecond() - tuple.getFirst();
			totalWeight += weight;
		}
		
		// Weighted random draw
		double accumulatedWeight = 0.0;
		double rndTreshold = MatsimRandom.getRandom().nextDouble() * totalWeight;
		for (Tuple<Integer, Integer> tuple : timeSlotTuples) {
			accumulatedWeight += tuple.getSecond() - tuple.getFirst();
			if (rndTreshold <= accumulatedWeight) {
				// ok, take this one
				return getTimeOfOperationFromTimeSlots(tuple.getFirst(), tuple.getSecond());
			}
		}		
		
		log.error("Should never be able to get here");
		return null;
	}
	
	private Tuple<Double, Double> getMaxTimeOfOperation(Set<Integer> slotsAboveTreshold) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		
		for (Integer slot : slotsAboveTreshold) {
			if (slot.doubleValue() < min) {
				min = slot;
			}
			if (slot.doubleValue() > max) {
				max = slot;
			}
		}
		
		return getTimeOfOperationFromTimeSlots(min, max);
	}

	@Override
	public String getStrategyName() {
		return ReduceTimeServedRFare.STRATEGY_NAME;
	}
	
	@Override
	public void reset() {
		this.route2StartTimeSlot2EndTimeSlot2WeightMap = new LinkedHashMap<>();
	}

	@Override
	public void handleFareContainer(StageContainer stageContainer) {
		Id<TransitRoute> routeId = stageContainer.getRouteId();
		Integer startTimeSlot = this.getTimeSlotForTime(stageContainer.getTimeEntered());
		Integer endTimeSlot = this.getTimeSlotForTime(stageContainer.getTimeLeft());
		
		if (this.route2StartTimeSlot2EndTimeSlot2WeightMap.get(routeId) == null) {
			this.route2StartTimeSlot2EndTimeSlot2WeightMap.put(routeId, new LinkedHashMap<Integer, LinkedHashMap<Integer,Double>>());
		}
	
		if (this.route2StartTimeSlot2EndTimeSlot2WeightMap.get(routeId).get(startTimeSlot) == null) {
			this.route2StartTimeSlot2EndTimeSlot2WeightMap.get(routeId).put(startTimeSlot, new LinkedHashMap<Integer,Double>());
		}
	
		if (this.route2StartTimeSlot2EndTimeSlot2WeightMap.get(routeId).get(startTimeSlot).get(endTimeSlot) == null) {
			this.route2StartTimeSlot2EndTimeSlot2WeightMap.get(routeId).get(startTimeSlot).put(endTimeSlot, (double) 0);
		}
	
		double oldWeight = this.route2StartTimeSlot2EndTimeSlot2WeightMap.get(routeId).get(startTimeSlot).get(endTimeSlot);
		double additionalWeight = 1.0;
		if (this.useFareAsWeight) {
			additionalWeight = this.ticketMachine.getFare(stageContainer);
		}
		this.route2StartTimeSlot2EndTimeSlot2WeightMap.get(routeId).get(startTimeSlot).put(endTimeSlot, oldWeight + additionalWeight);
	}

	public void setTicketMachine(TicketMachineI ticketMachine) {
		this.ticketMachine = ticketMachine;
	}

	private int getTimeSlotForTime(double time){
		return ((int) time / this.timeBinSize);
	}

	private Tuple<Double, Double> getTimeOfOperationFromTimeSlots(int startSlot, int endSlot){
		// convert slots to time
		double startTime = ((double) startSlot) * this.timeBinSize;
		double endTime = ((double) endSlot + 1) * this.timeBinSize;
		
		return new Tuple<>(startTime, endTime);
	}
}