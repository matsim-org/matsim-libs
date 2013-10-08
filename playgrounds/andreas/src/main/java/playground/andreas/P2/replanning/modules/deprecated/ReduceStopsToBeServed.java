/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.AbstractPStrategyModule;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.modules.ReduceStopsToBeServedRFare;

/**
 * 
 * Removes all stops belonging to a demand relation with trips below a certain threshold.
 * Threshold is standard deviation of number of trips of all relations twice a scaling factor.
 * 
 * @author aneumann
 *
 * @deprecated Use {@linkplain ReduceStopsToBeServedRFare} instead
 */
public class ReduceStopsToBeServed extends AbstractPStrategyModule implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private final static Logger log = Logger.getLogger(ReduceStopsToBeServed.class);
	
	public static final String STRATEGY_NAME = "ReduceStopsToBeServed";
	
	private LinkedHashMap<Id,LinkedHashMap<Id,LinkedHashMap<Id,Integer>>> line2StartStop2EndStop2TripsMap = new LinkedHashMap<Id, LinkedHashMap<Id,LinkedHashMap<Id,Integer>>>();
	
	private LinkedHashMap<Id, Id> vehId2lineIdMap = new LinkedHashMap<Id, Id>();
	private LinkedHashMap<Id, Id> vehId2stopIdMap = new LinkedHashMap<Id, Id>();	
	private LinkedHashMap<Id, Id> personId2startStopId = new LinkedHashMap<Id, Id>();
	
	private String pIdentifier;
	
	private final double sigmaScale;
	
	public ReduceStopsToBeServed(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 1){
			log.error("Too many parameter. Will ignore: " + parameter);
		}
		this.sigmaScale = Double.parseDouble(parameter.get(0));
		log.info("enabled");
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		ArrayList<TransitStopFacility> stopsToBeServed = getStopsToBeServed(this.line2StartStop2EndStop2TripsMap.get(cooperative.getId()), cooperative.getBestPlan().getLine());
		
		if (stopsToBeServed.size() < 2) {
			// too few stops left - cannot return a new plan
			return null;
		}
		
		// profitable route, change startTime
		PPlan newPlan = new PPlan(cooperative.getNewRouteId(), this.getName());
		newPlan.setNVehicles(1);
		newPlan.setStopsToBeServed(stopsToBeServed);
		newPlan.setStartTime(cooperative.getBestPlan().getStartTime());
		newPlan.setEndTime(cooperative.getBestPlan().getEndTime());
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));
		
		return newPlan;
	}

	private ArrayList<TransitStopFacility> getStopsToBeServed(LinkedHashMap<Id,LinkedHashMap<Id,Integer>> startStop2EndStop2TripsMap, TransitLine line) {
		ArrayList<TransitStopFacility> tempStopsToBeServed = new ArrayList<TransitStopFacility>();
		RecursiveStatsContainer stats = new RecursiveStatsContainer();
		
		if (startStop2EndStop2TripsMap == null) {
			// There is no entry for that particular line - possibly no demand - returning empty line
			return tempStopsToBeServed;
		}
		
		// calculate standard deviation
		for (LinkedHashMap<Id, Integer> endStop2TripsMap : startStop2EndStop2TripsMap.values()) {
			for (Integer trips : endStop2TripsMap.values()) {
				stats.handleNewEntry(trips.doubleValue());
			}
		}
		
		if (stats.numberOfEntries == 1) {
			// We use circular routes. There is always a way back (with no demand). Add a second enty.
			stats.handleNewEntry(0.0);			
		}
		
		double sigmaTreshold = stats.getStdDev() * this.sigmaScale;
		Set<Id> stopIdsAboveTreshold = new TreeSet<Id>();
		
		// Get all stops serving a demand above threshold
		for (Entry<Id, LinkedHashMap<Id, Integer>> endStop2TripsMapEntry : startStop2EndStop2TripsMap.entrySet()) {
			for (Entry<Id, Integer> tripEntry : endStop2TripsMapEntry.getValue().entrySet()) {
				if (tripEntry.getValue().doubleValue() > sigmaTreshold) {
					// ok - add the corresponding stops to the set
					stopIdsAboveTreshold.add(endStop2TripsMapEntry.getKey());
					stopIdsAboveTreshold.add(tripEntry.getKey());
				}
			}
		}
		
		// Get new stops to be served
		if (line.getRoutes().size() != 1) {
			log.error("There should be only one route at this time - Please check");
		}
		
		for (TransitRoute route : line.getRoutes().values()) {
			for (TransitRouteStop stop : route.getStops()) {
				if (stopIdsAboveTreshold.contains(stop.getStopFacility().getId())) {
					tempStopsToBeServed.add(stop.getStopFacility());
				}
			}
		}
		
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<TransitStopFacility>();
		
		// avoid using a stop twice in a row
		for (TransitStopFacility stop : tempStopsToBeServed) {
			if (stopsToBeServed.size() > 1) {
				if (stopsToBeServed.get(stopsToBeServed.size() - 1).getId() != stop.getId()) {
					stopsToBeServed.add(stop);
				}
			} else {
				stopsToBeServed.add(stop);
			}			
		}
		
		// delete last stop, if it is the same as the first one
		if (stopsToBeServed.size() > 1) {
			if (stopsToBeServed.get(0).getId() == stopsToBeServed.get(stopsToBeServed.size() - 1).getId()) {
				stopsToBeServed.remove(stopsToBeServed.size() - 1);
			}
		}
		
		return stopsToBeServed;
	}

	@Override
	public String getName() {
		return ReduceStopsToBeServed.STRATEGY_NAME;
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehId2lineIdMap.put(event.getVehicleId(), event.getTransitLineId());		
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			this.vehId2stopIdMap.put(event.getVehicleId(), event.getFacilityId());
		}
	}

	@Override
	public void reset(int iteration) {
		this.line2StartStop2EndStop2TripsMap = new LinkedHashMap<Id, LinkedHashMap<Id,LinkedHashMap<Id,Integer>>>();
		this.vehId2lineIdMap = new LinkedHashMap<Id, Id>();
		this.vehId2stopIdMap = new LinkedHashMap<Id, Id>();	
		this.personId2startStopId = new LinkedHashMap<Id, Id>();
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				Id lineId = this.vehId2lineIdMap.get(event.getVehicleId());		
				if (this.line2StartStop2EndStop2TripsMap.get(lineId) == null) {
					this.line2StartStop2EndStop2TripsMap.put(lineId, new LinkedHashMap<Id, LinkedHashMap<Id,Integer>>());
				}

				Id startStop = this.personId2startStopId.get(event.getPersonId());
				if (this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop) == null) {
					this.line2StartStop2EndStop2TripsMap.get(lineId).put(startStop, new LinkedHashMap<Id,Integer>());
				}

				Id endStop = this.vehId2stopIdMap.get(event.getVehicleId());
				if (this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop).get(endStop) == null) {
					this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop).put(endStop, new Integer(0));
				}

				int oldCountsValue = this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop).get(endStop);
				this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop).put(endStop, new Integer(oldCountsValue + 1));
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				this.personId2startStopId.put(event.getPersonId(), this.vehId2stopIdMap.get(event.getVehicleId()));
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
}