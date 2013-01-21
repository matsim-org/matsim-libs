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

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.AbstractPStrategyModule;

/**
 * 
 * Changes the end time of operation randomly within a certain range, leaving a minimal operating time.
 * 
 * @author aneumann
 *
 */
@Deprecated
public class RandomEndTimeAllocator extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(RandomEndTimeAllocator.class);
	
	public static final String STRATEGY_NAME = "RandomEndTimeAllocator";
	
	private final int mutationRange;

	public RandomEndTimeAllocator(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 1){
			log.error("Missing parameter: 1 - Mutation range in seconds");
		}
		this.mutationRange = Integer.parseInt(parameter.get(0));
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		// profitable route, change startTime
		PPlan newPlan = new PPlan(cooperative.getNewRouteId(), this.getName());
		newPlan.setNVehicles(1);
		newPlan.setStopsToBeServed(cooperative.getBestPlan().getStopsToBeServed());
		newPlan.setStartTime(cooperative.getBestPlan().getStartTime());
		
		// get a valid new end time
		double newEndTime = Math.min(24 * 3600.0, cooperative.getBestPlan().getEndTime() + (-0.5 + MatsimRandom.getRandom().nextDouble()) * this.mutationRange);
		newEndTime = Math.max(newEndTime, cooperative.getBestPlan().getStartTime() + cooperative.getMinOperationTime());
		newPlan.setEndTime(newEndTime);
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));
		
		return newPlan;
	}

	@Override
	public String getName() {
		return RandomEndTimeAllocator.STRATEGY_NAME;
	}

}
