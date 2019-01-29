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

import java.util.ArrayList;

/**
 * 
 * Changes the end time of operation by drawing randomly a new time slot from endTime to midnight.
 * The draw is weighted by the number of activities in those time slots (see {@link TimeProvider}).
 * 
 * @author aneumann
 *
 */
public final class WeightedEndTimeExtension extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(WeightedEndTimeExtension.class);	
	public static final String STRATEGY_NAME = "WeightedEndTimeExtension";
	
	private TimeProvider timeProvider = null;
	
	public WeightedEndTimeExtension(ArrayList<String> parameter) {
		super();
		if(parameter.size() != 0){
			log.error("No parameters needed for this module.");
		}
	}
	
	public void setTimeProvider(TimeProvider timeProvider){
		this.timeProvider = timeProvider;
	}
	
	@Override
	public PPlan run(Operator operator) {
		// change endTime
		PPlan newPlan = new PPlan(operator.getNewPlanId(), this.getStrategyName(), operator.getBestPlan().getId());
		newPlan.setNVehicles(1);
		newPlan.setStopsToBeServed(operator.getBestPlan().getStopsToBeServed());
		newPlan.setStartTime(operator.getBestPlan().getStartTime());
		
		// get a valid new end time
		double newEndTime = this.timeProvider.getRandomTimeInInterval(operator.getBestPlan().getEndTime(), 24 * 3600.0);
		newPlan.setEndTime(newEndTime);
		
		if(newPlan.getEndTime() <= newPlan.getStartTime()){
			// Could not find a valid new plan
			return null;
		}
		
		newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
		
		return newPlan;
	}

	@Override
	public String getStrategyName() {
		return WeightedEndTimeExtension.STRATEGY_NAME;
	}
}
