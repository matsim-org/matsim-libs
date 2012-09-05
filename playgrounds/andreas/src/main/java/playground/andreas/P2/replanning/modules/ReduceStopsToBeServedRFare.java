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

package playground.andreas.P2.replanning.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.AbstractPStrategyModule;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.scoring.fare.FareContainer;
import playground.andreas.P2.scoring.fare.FareContainerHandler;
import playground.andreas.utils.stats.RecursiveStatsContainer;

/**
 * 
 * Removes all stops belonging to a demand relation with trips below a certain threshold.
 * Threshold is standard deviation of number of trips of all relations twice a scaling factor.
 * based on route and fare
 * 
 * @author aneumann
 *
 */
public class ReduceStopsToBeServedRFare extends AbstractPStrategyModule implements FareContainerHandler{
	
	private final static Logger log = Logger.getLogger(ReduceStopsToBeServedRFare.class);
	
	public static final String STRATEGY_NAME = "ReduceStopsToBeServedRFare";
	
	private HashMap<Id,HashMap<Id,HashMap<Id,Double>>> route2StartStop2EndStop2WeightMap = new HashMap<Id, HashMap<Id,HashMap<Id,Double>>>();
	
	private final double sigmaScale;
	private final boolean useFareAsWeight;
	
	public ReduceStopsToBeServedRFare(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 2){
			log.error("Too many parameter. Will ignore: " + parameter);
			log.error("Parameter 1: Scaling factor for sigma");
			log.error("Parameter 2: true=use the fare as weight, false=use number of trips as weight");
		}
		this.sigmaScale = Double.parseDouble(parameter.get(0));
		this.useFareAsWeight = Boolean.parseBoolean(parameter.get(1));
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
		
		ArrayList<TransitStopFacility> stopsToBeServed = getStopsToBeServed(this.route2StartStop2EndStop2WeightMap.get(routeToOptimize.getId()), routeToOptimize);
		
		if (stopsToBeServed.size() < 2) {
			// too few stops left - cannot return a new plan
			return null;
		}
		
		// profitable route, change startTime
		PPlan newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()), this.getName());
		newPlan.setStopsToBeServed(stopsToBeServed);
		newPlan.setStartTime(cooperative.getBestPlan().getStartTime());
		newPlan.setEndTime(cooperative.getBestPlan().getEndTime());
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStopsToBeServed(), new IdImpl(cooperative.getCurrentIteration())));
		
		if(cooperative.getFranchise().planRejected(newPlan)){
			// plan is rejected by franchise system
			return null;
		}		
		
		return newPlan;
	}

	private ArrayList<TransitStopFacility> getStopsToBeServed(HashMap<Id,HashMap<Id,Double>> startStop2EndStop2WeightMap, TransitRoute routeToOptimize) {
		ArrayList<TransitStopFacility> tempStopsToBeServed = new ArrayList<TransitStopFacility>();
		RecursiveStatsContainer stats = new RecursiveStatsContainer();
		
		if (startStop2EndStop2WeightMap == null) {
			// There is no entry for that particular line - possibly no demand - returning empty line
			return tempStopsToBeServed;
		}
		
		// calculate standard deviation
		for (HashMap<Id, Double> endStop2TripsMap : startStop2EndStop2WeightMap.values()) {
			for (Double trips : endStop2TripsMap.values()) {
				stats.handleNewEntry(trips.doubleValue());
			}
		}
		
		if (stats.getNumberOfEntries() == 1) {
			// We use circular routes. There is always a way back (with no demand). Add a second entry.
			stats.handleNewEntry(0.0);			
		}
		
		double sigmaTreshold = stats.getStdDev() * this.sigmaScale;
		Set<Id> stopIdsAboveTreshold = new TreeSet<Id>();
		
		// Get all stops serving a demand above threshold
		for (Entry<Id, HashMap<Id, Double>> endStop2TripsMapEntry : startStop2EndStop2WeightMap.entrySet()) {
			for (Entry<Id, Double> tripEntry : endStop2TripsMapEntry.getValue().entrySet()) {
				if (tripEntry.getValue().doubleValue() > sigmaTreshold) {
					// ok - add the corresponding stops to the set
					stopIdsAboveTreshold.add(endStop2TripsMapEntry.getKey());
					stopIdsAboveTreshold.add(tripEntry.getKey());
				}
			}
		}
		
		// Get new stops to be served
		for (TransitRouteStop stop : routeToOptimize.getStops()) {
			if (stopIdsAboveTreshold.contains(stop.getStopFacility().getId())) {
				tempStopsToBeServed.add(stop.getStopFacility());
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
		return ReduceStopsToBeServedRFare.STRATEGY_NAME;
	}
	
	@Override
	public void reset(int iteration) {
		this.route2StartStop2EndStop2WeightMap = new HashMap<Id, HashMap<Id,HashMap<Id,Double>>>();
	}

	@Override
	public void handleFareContainer(FareContainer fareContainer) {
		Id routeId = fareContainer.getRouteId();
		Id startStopId = fareContainer.getStopEntered();
		Id endStopId = fareContainer.getStopLeft();
		
		if (this.route2StartStop2EndStop2WeightMap.get(routeId) == null) {
			this.route2StartStop2EndStop2WeightMap.put(routeId, new HashMap<Id, HashMap<Id,Double>>());
		}

		if (this.route2StartStop2EndStop2WeightMap.get(routeId).get(startStopId) == null) {
			this.route2StartStop2EndStop2WeightMap.get(routeId).put(startStopId, new HashMap<Id,Double>());
		}

		if (this.route2StartStop2EndStop2WeightMap.get(routeId).get(startStopId).get(endStopId) == null) {
			this.route2StartStop2EndStop2WeightMap.get(routeId).get(startStopId).put(endStopId, new Double(0.0));
		}

		double oldWeight = this.route2StartStop2EndStop2WeightMap.get(routeId).get(startStopId).get(endStopId).doubleValue();
		double additionalWeight = 1.0;
		if (this.useFareAsWeight) {
			additionalWeight = fareContainer.getFare();
		}
		this.route2StartStop2EndStop2WeightMap.get(routeId).get(startStopId).put(endStopId, new Double(oldWeight + additionalWeight));
	}
}