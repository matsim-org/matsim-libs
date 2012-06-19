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

package playground.andreas.P2.plan;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.helper.PConfigGroup;

/**
 * 
 * @author aneumann
 *
 */
public class RandomStopProvider {
	
	private final static Logger log = Logger.getLogger(RandomStopProvider.class);
	
	private final double gridSize;
	private HashMap<TransitStopFacility, Double> stops2Weight;
	private double totalWeight = 0.0;
	private int lastIteration;

	private Population population;
	private TransitSchedule pStopsOnly;
	
	public RandomStopProvider(PConfigGroup pConfig, Population population, TransitSchedule pStopsOnly){
		this.gridSize = pConfig.getRandomStopProviderGridSize();
		this.lastIteration = -1;
		
		this.population = population;
		this.pStopsOnly = pStopsOnly;
	}
	
	private void updateWeights(){
		this.stops2Weight = new HashMap<TransitStopFacility, Double>();
		this.totalWeight = 0.0;
		double numberOfActsInPlans = 0;
		
		// count acts for all grid nodes
		HashMap<String, Integer> gridNodeId2ActsCountMap = new HashMap<String, Integer>();
		for (Person person : this.population.getPersons().values()) {
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					numberOfActsInPlans++;
					String gridNodeId = this.getGridNodeIdForAct(act.getCoord());
					if (gridNodeId2ActsCountMap.get(gridNodeId) == null) {
						gridNodeId2ActsCountMap.put(gridNodeId, new Integer(0));
					}
					gridNodeId2ActsCountMap.put(gridNodeId, new Integer(gridNodeId2ActsCountMap.get(gridNodeId) + 1));
				}
			}
		}
		
		// sort facilities for all grid nodes
		HashMap<String, List<TransitStopFacility>> gridNodeId2StopsMap = new HashMap<String, List<TransitStopFacility>>();
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			String gridNodeId = this.getGridNodeIdForAct(stop.getCoord());
			if (gridNodeId2StopsMap.get(gridNodeId) == null) {
				gridNodeId2StopsMap.put(gridNodeId, new LinkedList<TransitStopFacility>());
			}
			gridNodeId2StopsMap.get(gridNodeId).add(stop);
		}
		
		// associate the number of acts per grid node with the corresponding transit stop facilities
		for (Entry<String, List<TransitStopFacility>> stopsEntry : gridNodeId2StopsMap.entrySet()) {
			double actsCountForThisGridNodeId = 0; 
			if (gridNodeId2ActsCountMap.get(stopsEntry.getKey()) != null) {
				actsCountForThisGridNodeId = gridNodeId2ActsCountMap.get(stopsEntry.getKey()).doubleValue();
			}
			
			// no acts in this area - ignore the stops in this area as well
			if (actsCountForThisGridNodeId == 0.0) {
				continue;
			}
			
			// divide count by the number of associated stops, thus neglecting a higher probability for stops located in a dense network area
			actsCountForThisGridNodeId = actsCountForThisGridNodeId / stopsEntry.getValue().size();
			for (TransitStopFacility stop : stopsEntry.getValue()) {
				this.stops2Weight.put(stop, new Double(actsCountForThisGridNodeId));
				this.totalWeight += actsCountForThisGridNodeId;
			}
		}
		
		log.info("Initialized with " + this.stops2Weight.size() + " of " + this.pStopsOnly.getFacilities().values().size() + " stops covering " + (this.totalWeight / numberOfActsInPlans) + " percent of activities");
	}


	public TransitStopFacility getRandomTransitStop(int currentIteration) {
		if (lastIteration != currentIteration) {
			this.updateWeights();
			lastIteration = currentIteration;
		}
		
		if (this.totalWeight == 0.0) {
			log.info("No weights found. Probably to population given. Falling back to old behavior.");
			// old version
			int i = this.pStopsOnly.getFacilities().size();
			for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
				if(MatsimRandom.getRandom().nextDouble() < 1.0 / i){
					return stop;
				}
				i--;
			}
			return null;
		}
		
		double rnd = MatsimRandom.getRandom().nextDouble() * this.totalWeight;
		double accumulatedWeight = 0.0;
		for (Entry<TransitStopFacility, Double> stop2WeightEntry : this.stops2Weight.entrySet()) {
			accumulatedWeight += stop2WeightEntry.getValue().doubleValue();
			if (accumulatedWeight >= rnd) {
				return stop2WeightEntry.getKey();
			}
		}
		
		log.warn("Could not find any stop. This should not happen. Check gridSize in config.");
		return null;
	}
	
	private String getGridNodeIdForAct(Coord coord){
		int xSlot = (int) (coord.getX() / this.gridSize);
		int ySlot = (int) (coord.getY() / this.gridSize);
		String gridNodeId = xSlot + "-" + ySlot;
		return gridNodeId;
	}
}
