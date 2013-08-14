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

package playground.andreas.P2.replanning.modules.deprecated;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.AbstractPStrategyModule;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.modules.ReduceTimeServedRFare;

/**
 * @deprecated Use {@link ReduceTimeServedRFare} with parameter false.<br>
 * 
 * Restricts the time of operation to temporal relations higher than a certain threshold.
 * Threshold is standard deviation of number of trips of all relations twice a scaling factor.
 * based on route
 * 
 * @author aneumann
 *
 */
@Deprecated
public class ReduceTimeServedR extends AbstractPStrategyModule implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private final static Logger log = Logger.getLogger(ReduceTimeServedR.class);
	
	public static final String STRATEGY_NAME = "ReduceTimeServedR";
	private final double sigmaScale;
	private final int timeBinSize;
	
	private LinkedHashMap<Id,LinkedHashMap<Integer,LinkedHashMap<Integer,Integer>>> route2StartTimeSlot2EndTimeSlot2TripsMap = new LinkedHashMap<Id, LinkedHashMap<Integer,LinkedHashMap<Integer,Integer>>>();
	
	private LinkedHashMap<Id, Id> vehId2routeIdMap = new LinkedHashMap<Id, Id>();
	private LinkedHashMap<Id, Integer> personId2startTimeSlot = new LinkedHashMap<Id, Integer>();
	
	private String pIdentifier;
	
	public ReduceTimeServedR(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 2){
			log.error("Too many parameter. Will ignore: " + parameter);
			log.error("Parameter 1: Scaling factor for sigma");
			log.error("Parameter 2: Time bin size in seconds");
		}
		this.sigmaScale = Double.parseDouble(parameter.get(0));
		this.timeBinSize = Integer.parseInt(parameter.get(1));
		log.info("enabled");
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		
		if (cooperative.getBestPlan().getNVehicles() <= 1) {
			return null;
		}
		
		// get best plans route id
		TransitRoute routeToOptimize = null;
		if (cooperative.getBestPlan().getLine().getRoutes().size() != 1) {
			log.error("There should be only one route at this time - Please check");
		}
		for (TransitRoute route : cooperative.getBestPlan().getLine().getRoutes().values()) {
			routeToOptimize = route;
		}
		
		Tuple<Double,Double> timeToBeServed = getTimeToBeServed(this.route2StartTimeSlot2EndTimeSlot2TripsMap.get(routeToOptimize.getId()));
		
		if (timeToBeServed == null) {
			// Could not modify plan
			return null;
		}
		
		// profitable route, change startTime
		PPlan newPlan = new PPlan(cooperative.getNewRouteId(), this.getName());
		newPlan.setNVehicles(1);
		newPlan.setStopsToBeServed(cooperative.getBestPlan().getStopsToBeServed());
		
		newPlan.setStartTime(timeToBeServed.getFirst());
		newPlan.setEndTime(timeToBeServed.getSecond());
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));
		
		return newPlan;
	}


	private Tuple<Double,Double> getTimeToBeServed(LinkedHashMap<Integer,LinkedHashMap<Integer,Integer>> startSlot2EndSlot2TripsMap) {
		RecursiveStatsContainer stats = new RecursiveStatsContainer();
		
		if (startSlot2EndSlot2TripsMap == null) {
			// There is no entry for that particular line - possibly no demand - returning empty line
			return null;
		}
		
		// calculate standard deviation
		for (LinkedHashMap<Integer, Integer> EndSlot2TripsMap : startSlot2EndSlot2TripsMap.values()) {
			for (Integer trips : EndSlot2TripsMap.values()) {
				stats.handleNewEntry(trips.doubleValue());
			}
		}
		
		if (stats.numberOfEntries == 1) {
			// We use circular routes. There is always a way back (with no demand). Add a second entry.
			stats.handleNewEntry(0.0);			
		}
		
		double sigmaTreshold = stats.getStdDev() * this.sigmaScale;
		Set<Integer> slotsAboveTreshold = new TreeSet<Integer>();
		
		// Get all slots serving a demand above threshold
		for (Entry<Integer, LinkedHashMap<Integer, Integer>> endSlot2TripsMapEntry : startSlot2EndSlot2TripsMap.entrySet()) {
			for (Entry<Integer, Integer> tripEntry : endSlot2TripsMapEntry.getValue().entrySet()) {
				if (tripEntry.getValue().doubleValue() > sigmaTreshold) {
					// ok - add the corresponding slots to the set
					slotsAboveTreshold.add(endSlot2TripsMapEntry.getKey());
					slotsAboveTreshold.add(tripEntry.getKey());
				}
			}
		}
		
		// Get new slots to be served
		double min = Integer.MAX_VALUE;
		double max = Integer.MIN_VALUE;
		
		for (Integer slot : slotsAboveTreshold) {
			if (slot.doubleValue() < min) {
				min = slot.doubleValue();
			}
			if (slot.doubleValue() > max) {
				max = slot.doubleValue();
			}
		}
		
		// convert slots to time
		min = min * this.timeBinSize;
		max = (max + 1) * this.timeBinSize;
		
		Tuple<Double, Double> timeSlotsOfOperation = new Tuple<Double, Double>(min, max);
		return timeSlotsOfOperation;
	}

	@Override
	public String getName() {
		return ReduceTimeServedR.STRATEGY_NAME;
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehId2routeIdMap.put(event.getVehicleId(), event.getTransitRouteId());		
	}

	@Override
	public void reset(int iteration) {
		this.route2StartTimeSlot2EndTimeSlot2TripsMap = new LinkedHashMap<Id, LinkedHashMap<Integer,LinkedHashMap<Integer,Integer>>>();
		this.vehId2routeIdMap = new LinkedHashMap<Id, Id>();
		this.personId2startTimeSlot = new LinkedHashMap<Id, Integer>();
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				Id routeId = this.vehId2routeIdMap.get(event.getVehicleId());		
				if (this.route2StartTimeSlot2EndTimeSlot2TripsMap.get(routeId) == null) {
					this.route2StartTimeSlot2EndTimeSlot2TripsMap.put(routeId, new LinkedHashMap<Integer, LinkedHashMap<Integer,Integer>>());
				}

				Integer startTimeSlot = this.personId2startTimeSlot.get(event.getPersonId());
				if (this.route2StartTimeSlot2EndTimeSlot2TripsMap.get(routeId).get(startTimeSlot) == null) {
					this.route2StartTimeSlot2EndTimeSlot2TripsMap.get(routeId).put(startTimeSlot, new LinkedHashMap<Integer,Integer>());
				}

				Integer endTimeSlot = this.getTimeSlotForTime(event.getTime());
				if (this.route2StartTimeSlot2EndTimeSlot2TripsMap.get(routeId).get(startTimeSlot).get(endTimeSlot) == null) {
					this.route2StartTimeSlot2EndTimeSlot2TripsMap.get(routeId).get(startTimeSlot).put(endTimeSlot, new Integer(0));
				}

				int oldCountsValue = this.route2StartTimeSlot2EndTimeSlot2TripsMap.get(routeId).get(startTimeSlot).get(endTimeSlot);
				this.route2StartTimeSlot2EndTimeSlot2TripsMap.get(routeId).get(startTimeSlot).put(endTimeSlot, new Integer(oldCountsValue + 1));
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				this.personId2startTimeSlot.put(event.getPersonId(), this.getTimeSlotForTime(event.getTime()));
			}
		}
	}
	
	public void setPIdentifier(String pIdentifier) {
		this.pIdentifier = pIdentifier;		
	}
	
	private class RecursiveStatsContainer {
		private double numberOfEntries = Double.NaN;
		private double arithmeticMean;
		private double tempVar;
		
		public void handleNewEntry(double entry){
			// new entries n + 1
			double meanCoops_n_1;
			double tempVarCoops_n_1;

			if(Double.isNaN(this.numberOfEntries)){
				// initialize
				this.numberOfEntries = 0;
				this.arithmeticMean = 0;
				this.tempVar = 0;
			}

			// calculate the exact mean and variance

			// calculate new mean
			meanCoops_n_1 =  (this.numberOfEntries * this.arithmeticMean + entry) / (this.numberOfEntries + 1);

			if (this.numberOfEntries == 0) {
				tempVarCoops_n_1 = 0;
			} else {
				tempVarCoops_n_1 = this.tempVar + (this.numberOfEntries + 1) / (this.numberOfEntries) * (meanCoops_n_1 - entry) * (meanCoops_n_1 - entry);
			}
			
			this.numberOfEntries++;

			// store em away
			this.arithmeticMean = meanCoops_n_1;
			this.tempVar = tempVarCoops_n_1;
		}

		public double getStdDev() {
			if (this.numberOfEntries > 1){
				return Math.sqrt(1.0/(this.numberOfEntries - 1.0) * this.tempVar);
			}			
			return Double.NaN;
		}
	}
	
	private int getTimeSlotForTime(double time){
		return ((int) time / this.timeBinSize);
	}
}