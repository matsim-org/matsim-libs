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
package playground.vsp.buildingEnergy.energyCalculation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyMATSimDataReader.LinkOccupancyStats;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyMATSimDataReader.PopulationStats;
import playground.vsp.buildingEnergy.linkOccupancy.LinkActivityOccupancyCounter;

/**
 * 
 * Calculates the probabilities of performing a certain activity for a certain {@link LinkActivityOccupancyCounter}
 * @author droeder
 *
 */
class BuildingEnergyActivityProbabilityCalculator {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(BuildingEnergyActivityProbabilityCalculator.class);
	private List<Id> ids;

	BuildingEnergyActivityProbabilityCalculator(List<Id> linkIds) {
		this.ids = linkIds;
	}
	
	/**
	 * 
	 * @return the probabilities of performing a certain activity for a certain {@link LinkActivityOccupancyCounter}
	 */
	private Map<String, Double> calcActivityProbability(double maxOfActivityType, Map<String, LinkActivityOccupancyCounter> counter){
		Map<String, Double> map = new HashMap<String, Double>();
		for(Entry<String, LinkActivityOccupancyCounter> e: counter.entrySet()){
			Double d = 0.;
			for(Id id: ids){
				d += e.getValue().getMaximumOccupancy(id);
			}
			map.put(e.getKey(), d/maxOfActivityType);
		}
		return map;
	}
	
	public ActivityProbabilities run(
							Map<String, Map<String, LinkOccupancyStats>> run2Type2RawOccupancy,
							Map<String, PopulationStats> run2PopulationStats) {
		Map<String, Map<String, Map<String, Double>>> runs = new HashMap<String, Map<String,Map<String, Double>>>();
		for(Entry<String, Map<String, LinkOccupancyStats>> e: run2Type2RawOccupancy.entrySet()){
			Map<String, Map<String, Double>> types = new HashMap<String, Map<String,Double>>();
			for(Entry<String, LinkOccupancyStats> ee: e.getValue().entrySet()){
				types.put(ee.getKey(), calcActivityProbability(run2PopulationStats.get(e.getKey()).getHomeAndWorkCnt(), ee.getValue().getStats()));
			}
			runs.put(e.getKey(), types);
		}
		return new ActivityProbabilities(runs);
	}
	
	class ActivityProbabilities{
		private Map<String, Map<String, Map<String, Double>>> proba;
		
		public ActivityProbabilities(Map<String, Map<String, Map<String, Double>>> proba) {
			this.proba = proba;
		}
		
		
		Map<String, Map<String, Map<String, Double>>> getProbabilities(){
			return proba;
		}
		
		Map<String, Double> getProbability(String run, String type){
			return (proba.containsKey(run) ? proba.get(run).get(type) : null);
		}
	}

	
}

