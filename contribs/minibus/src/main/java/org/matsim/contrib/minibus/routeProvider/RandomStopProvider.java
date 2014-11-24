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

package org.matsim.contrib.minibus.routeProvider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.genericUtils.GridNode;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Provides random draws of transit stops facilities. The draw is weighted by the number of activities within the stops proximity.
 * 
 * @author aneumann
 *
 */
final class RandomStopProvider {
	
	private final static Logger log = Logger.getLogger(RandomStopProvider.class);
	
	private final double gridSize;
	private LinkedHashMap<TransitStopFacility, Double> stops2Weight;
	private double totalWeight = 0.0;
	private int lastIteration;

	private final Population population;
	private final TransitSchedule pStopsOnly;

	private String outputDir;

	
	public RandomStopProvider(PConfigGroup pConfig, Population population, TransitSchedule pStopsOnly, String outputDir){
		this.gridSize = pConfig.getGridSize();
		this.lastIteration = -1;
		
		this.population = population;
		this.pStopsOnly = pStopsOnly;
		this.outputDir = outputDir;		
	}
	
	private void updateWeights(){
		this.stops2Weight = new LinkedHashMap<>();
		this.totalWeight = 0.0;
		double numberOfActsInPlans = 0;
		
		// count acts for all grid nodes
		LinkedHashMap<String, Integer> gridNodeId2ActsCountMap = new LinkedHashMap<>();
		for (Person person : this.population.getPersons().values()) {
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					numberOfActsInPlans++;
					String gridNodeId = GridNode.getGridNodeIdForCoord(act.getCoord(), this.gridSize);
					if (gridNodeId2ActsCountMap.get(gridNodeId) == null) {
						gridNodeId2ActsCountMap.put(gridNodeId, 0);
					}
					gridNodeId2ActsCountMap.put(gridNodeId, gridNodeId2ActsCountMap.get(gridNodeId) + 1);
				}
			}
		}
		
		// sort facilities for all grid nodes
		LinkedHashMap<String, List<TransitStopFacility>> gridNodeId2StopsMap = new LinkedHashMap<>();
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			String gridNodeId = GridNode.getGridNodeIdForCoord(stop.getCoord(), this.gridSize);
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
				this.stops2Weight.put(stop, actsCountForThisGridNodeId);
				this.totalWeight += actsCountForThisGridNodeId;
			}
		}
		
		log.info("Initialized with " + this.stops2Weight.size() + " of " + this.pStopsOnly.getFacilities().values().size() + " stops covering " + (this.totalWeight / numberOfActsInPlans) + " percent of activities");
	}


	public TransitStopFacility getRandomTransitStop(int currentIteration) {
		if (this.lastIteration != currentIteration) {
			this.updateWeights();
			this.writeToFile(currentIteration);
			this.lastIteration = currentIteration;
			
			if (this.totalWeight == 0.0) {
				log.info("No weights found. Probably no population given. Falling back to old behavior.");
			}
		}
		
		if (this.totalWeight == 0.0) {
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
			accumulatedWeight += stop2WeightEntry.getValue();
			if (accumulatedWeight >= rnd) {
				return stop2WeightEntry.getKey();
			}
		}
		
		log.warn("Could not find any stop. This should not happen. Check gridSize in config.");
		return null;
	}
	
	/**
	 * Draws one random transit stop facility from the given choice set with respect to their weights.
	 * 
	 * @param choiceSet
	 * @return
	 */
	public TransitStopFacility drawRandomStopFromList(List<TransitStopFacility> choiceSet) {
		if (this.stops2Weight == null) {
			updateWeights();
		}
		double totalWeightOfChoiceSet = 0.0;
		for (TransitStopFacility stop : choiceSet) {
			if (this.stops2Weight.get(stop) != null) {
				totalWeightOfChoiceSet += this.stops2Weight.get(stop);
			}
		}
		
		if (totalWeightOfChoiceSet == 0.0) {
			// old version
			int i = 0;
			double rndTreshold = MatsimRandom.getRandom().nextDouble() * choiceSet.size();
			for (TransitStopFacility stop : choiceSet) {
				i++;
				if(rndTreshold <= i){
					return stop;
				}
			}
			return null;
		}

		double accumulatedWeightOfChoiceSet = 0.0;
		double rndTreshold = MatsimRandom.getRandom().nextDouble() * totalWeightOfChoiceSet;
		for (TransitStopFacility stop : choiceSet) {
			if (this.stops2Weight.get(stop) != null) {
				accumulatedWeightOfChoiceSet += this.stops2Weight.get(stop);
			}
			if (rndTreshold <= accumulatedWeightOfChoiceSet) {
				return stop;
			}
		}
		
		log.warn("Could not draw a random stop from the given choice set " + choiceSet);
		return null;
	}
	
	private void writeToFile(int currentIteration) {
		if (this.outputDir == null) {
			return;
		}
		try {
			if (this.lastIteration == -1) {
				// init output dir
				this.outputDir = this.outputDir + PConstants.statsOutputFolder + RandomStopProvider.class.getSimpleName() + "/";
				new File(this.outputDir).mkdir();
			}
			
			BufferedWriter writer = IOUtils.getBufferedWriter(outputDir + currentIteration + ".stopId2stopWeight.txt.gz");
			writer.write("# stop id; x; y; weight"); writer.newLine();
			for (Entry<TransitStopFacility, Double> stopEntry : this.stops2Weight.entrySet()) {
				writer.write(stopEntry.getKey().getId().toString() + "; " + stopEntry.getKey().getCoord().getX() + "; " + stopEntry.getKey().getCoord().getY() + "; " + stopEntry.getValue().toString()); writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
