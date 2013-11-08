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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.vsp.buildingEnergy.linkOccupancy.LinkActivityOccupancyCounter;

/**
 * 
 * Calculates the probabilities of performing a certain activity for a certain {@link LinkActivityOccupancyCounter}
 * @author droeder
 *
 */
public class BuidlingEnergyActivityProbabilityCalculator {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(BuidlingEnergyActivityProbabilityCalculator.class);
	private int maxOfActivityType;
	private Map<String, LinkActivityOccupancyCounter> counter;
	private Set<Id> ids;

	public BuidlingEnergyActivityProbabilityCalculator(int maxOfActivityType, Map<String, LinkActivityOccupancyCounter> counter, Set<Id> linkIds) {
		this.maxOfActivityType = maxOfActivityType;
		this.counter = counter;
		this.ids = linkIds;
	}
	
	/**
	 * 
	 * @return the probabilities of performing a certain activity for a certain {@link LinkActivityOccupancyCounter}
	 */
	public final Map<String, Double> run(){
		Map<String, Double> map = new HashMap<String, Double>();
		for(Entry<String, LinkActivityOccupancyCounter> e: this.counter.entrySet()){
			Double d = 0.;
			for(Id id: ids){
				d += e.getValue().getMaximumOccupancy(id);
			}
			map.put(e.getKey(), d/maxOfActivityType);
		}
		return map;
	}

	
}

