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

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;

import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.AbstractPStrategyModule;

/**
 * 
 * Sets a new endTime between the old endTime and midnight.
 * 
 * @author aneumann
 *
 */
public class MaxRandomEndTimeAllocator extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(MaxRandomEndTimeAllocator.class);	
	public static final String STRATEGY_NAME = "MaxRandomEndTimeAllocator";
	
	private final int mutationRange;
	private final int timeBinSize;
	private final boolean searchInBothDirections;
	
	public MaxRandomEndTimeAllocator(ArrayList<String> parameter) {
		super(parameter);
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
	public PPlan run(Cooperative cooperative) {
		if (cooperative.getBestPlan().getNVehicles() <= 1) {
			return null;
		}
		
		// enough vehicles to test, change endTime
		PPlan newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()), this.getName());
		newPlan.setStopsToBeServed(cooperative.getBestPlan().getStopsToBeServed());
		newPlan.setStartTime(cooperative.getBestPlan().getStartTime());
		
		// get a valid new end time
		double timeMutation;
		if (searchInBothDirections) {
			timeMutation = (MatsimRandom.getRandom().nextDouble() - 0.5) * 2.0 * this.mutationRange;
		} else {
			timeMutation = MatsimRandom.getRandom().nextDouble() * this.mutationRange;
		}
		
		double newEndTime = Math.min(24 * 3600.0, cooperative.getBestPlan().getEndTime() + timeMutation);
		newEndTime = Math.max(newEndTime, cooperative.getBestPlan().getStartTime() + cooperative.getMinOperationTime());
		
		// cast time to time bin size
		newEndTime = this.getTimeSlotForTime(newEndTime) * this.timeBinSize;
		newPlan.setEndTime(newEndTime);
		
		if(newPlan.getEndTime() <= newPlan.getStartTime()){
			// Could not find a valid new plan
			return null;
		}
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStopsToBeServed(), new IdImpl(cooperative.getCurrentIteration())));
		
		return newPlan;
	}

	@Override
	public String getName() {
		return MaxRandomEndTimeAllocator.STRATEGY_NAME;
	}
	
	private int getTimeSlotForTime(double time){
		return ((int) time / this.timeBinSize);
	}
}
