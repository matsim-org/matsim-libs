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

import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyConsumptionRule.BuildingEnergyConsumptionRuleFactory;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyMATSimDataReader.LinkOccupancyStats;
import playground.vsp.buildingEnergy.linkOccupancy.LinkActivityOccupancyCounter;

/**
 * @author droeder
 *
 */
class BuildingEnergyAggregatedEnergyConsumptionCalculator {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(BuildingEnergyAggregatedEnergyConsumptionCalculator.class);
	private BuildingEnergyConsumptionRuleFactory rules;
	private Map<String, EnergyConsumption> consumption;
	private List<Id> links;
	private List<Integer> timeBins;
	private Map<String, Map<Id, Integer>> maxValues;

	BuildingEnergyAggregatedEnergyConsumptionCalculator(BuildingEnergyConsumptionRuleFactory rules, 
				List<Id> links, List<Integer> timeBins, Map<String, Map<Id, Integer>> maxValues) {
		this.rules = rules;
		this.links = links;
		this.timeBins = timeBins;
		this.maxValues = maxValues;
		init();
	}
	
	/**
	 * 
	 */
	private void init() {
		this.consumption = new HashMap<String, EnergyConsumption>();
	}

//	Map<String, EnergyConsumption> getEnergyConsumptionPerRun(){
//		return this.consumption;
//	}
	
	Map<String, EnergyConsumption> run(Map<String, Map<String, LinkOccupancyStats>> occupancyRun2Type2Occupancy){
		log.info("calculating energy-consumption.");
		for(Entry<String, Map<String, LinkOccupancyStats>> e: occupancyRun2Type2Occupancy.entrySet()){
			EnergyConsumption c = consumption.get(e.getKey());
			if(c == null){
				c = new EnergyConsumption();
				consumption.put(e.getKey(), c);
			}
			for(Entry<String, LinkOccupancyStats> ee: e.getValue().entrySet()){
				BuildingEnergyConsumptionRule rule = rules.getRule(ee.getKey());
				Map<Id, Integer> maxValues = this.maxValues.get(ee.getKey());
				calcEnergyConsumptionPerType(ee.getValue(), maxValues, rule, c, ee.getKey());
			}
		}
		log.info("finished (calculating energy-consumption).");
		return consumption;
	}
	
	
	
		/**
		 * @param runRawData
		 * @param officeSizePerLink
		 * @return
		 */
		private void calcEnergyConsumptionPerType(LinkOccupancyStats runRawData,
				Map<Id, Integer> officeSizePerLink,
				BuildingEnergyConsumptionRule rule,
				EnergyConsumption consumption,
				String type) {
			for(Id l: links){
				Integer maxSize = officeSizePerLink.get(l);
				// no activities.
				if(maxSize == 0) continue;
				for(int i : timeBins){
					LinkActivityOccupancyCounter oc = runRawData.getStats().get(String.valueOf(i));
					Integer currentOccupancy = oc.getMaximumOccupancy(l);
					Double energyConsumption = rule.getEnergyConsumption_kWh(maxSize, currentOccupancy);
					Double d = consumption.put(i, energyConsumption, type);
					if(d != null) consumption.put(i, d + energyConsumption, type);
				}
			}
		}
	
	class EnergyConsumption{
		private Map<String, Map<Integer, Double>> consumption;
		
		private EnergyConsumption(){
			consumption = new HashMap<String, Map<Integer, Double>>();
		}

		/**
		 * @param i
		 * @param energyConsumption
		 * @param type
		 * @return
		 */
		private Double put(Integer i, Double energyConsumption, String type) {
			Map<Integer, Double> map = consumption.get(type);
			if(map ==  null){
				map = new HashMap<Integer, Double>();
				consumption.put(type, map);
			}
			return map.put(i, energyConsumption);
		}
		
		final Map<String, Map<Integer, Double>> getActType2Consumption(){
			return consumption;
		}
	}

}

