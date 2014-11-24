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

package org.matsim.contrib.minibus.replanning;

import org.apache.log4j.Logger;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.contrib.minibus.operator.TimeProvider;
import org.matsim.core.gbl.MatsimRandom;

import java.util.ArrayList;

/**
 * 
 * Sets a new endTime between the old endTime and midnight.
 * 
 * @author aneumann
 *
 */
public final class MaxRandomEndTimeAllocator extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(MaxRandomEndTimeAllocator.class);	
	public static final String STRATEGY_NAME = "MaxRandomEndTimeAllocator";
	
	private final int mutationRange;
	private final int timeBinSize;
	private final boolean searchInBothDirections;
	
	public MaxRandomEndTimeAllocator(ArrayList<String> parameter) {
		super();
		if(parameter.size() != 3){
			log.error("Parameter 1: Mutation range in seconds");
			log.error("Parameter 2: Time bin size in seconds");
			log.error("Parameter 3: Search in both directions true/false. Setting it true will double the total mutation range.");
		}
		this.mutationRange = Integer.parseInt(parameter.get(0));
		this.timeBinSize = Integer.parseInt(parameter.get(1));
		this.searchInBothDirections = Boolean.parseBoolean(parameter.get(2));
	}
	
	@Override
	public PPlan run(Operator operator) {
		// change endTime
		PPlan newPlan = new PPlan(operator.getNewPlanId(), this.getStrategyName(), operator.getBestPlan().getId());
		newPlan.setNVehicles(1);
		newPlan.setStopsToBeServed(operator.getBestPlan().getStopsToBeServed());
		newPlan.setStartTime(operator.getBestPlan().getStartTime());
		
		// get a valid new end time
		double timeMutation;
		if (searchInBothDirections) {
			timeMutation = (MatsimRandom.getRandom().nextDouble() - 0.5) * 2.0 * this.mutationRange;
		} else {
			timeMutation = MatsimRandom.getRandom().nextDouble() * this.mutationRange;
		}
		
		double newEndTime = Math.min(24 * 3600.0, operator.getBestPlan().getEndTime() + timeMutation);
				
		// cast time to time bin size
		newEndTime = TimeProvider.getSlotForTime(newEndTime, this.timeBinSize) * this.timeBinSize;
		
		// increase to match min operation time
		while (newEndTime < operator.getBestPlan().getStartTime() + operator.getMinOperationTime()) {
			newEndTime += this.timeBinSize;
		}
		
		newPlan.setEndTime(newEndTime);
		
		if(newPlan.getEndTime() <= newPlan.getStartTime()){
			// Could not find a valid new plan
			return null;
		}
		
		newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
		
		return newPlan;
	}

	public String getStrategyName() {
		return MaxRandomEndTimeAllocator.STRATEGY_NAME;
	}
}
