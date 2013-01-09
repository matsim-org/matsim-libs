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

import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.AbstractPStrategyModule;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.TimeProvider;

/**
 * 
 * Changes the start time of operation by drawing randomly a new time slot from midnight to startTime.
 * The draw is weighted by the number of activities in those time slots (see {@link TimeProvider}).
 * 
 * @author aneumann
 *
 */
public class WeightedStartTimeExtension extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(WeightedStartTimeExtension.class);
	public static final String STRATEGY_NAME = "WeightedStartTimeExtension";

	private TimeProvider timeProvider = null;
	
	public WeightedStartTimeExtension(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("No parameters needed for this module.");
		}
	}
	
	public void setTimeProvider(TimeProvider timeProvider){
		this.timeProvider = timeProvider;
	}

	@Override
	public PPlan run(Cooperative cooperative) {
		if (cooperative.getBestPlan().getNVehicles() <= 1) {
			return null;
		}
		
		// enough vehicles to test, change startTime
		PPlan newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration()), this.getName());
		newPlan.setStopsToBeServed(cooperative.getBestPlan().getStopsToBeServed());
		
		// get a valid new start time
		double newStartTime = this.timeProvider.getRandomTimeInInterval(0.0, cooperative.getBestPlan().getStartTime());
		newPlan.setStartTime(newStartTime);
		newPlan.setEndTime(cooperative.getBestPlan().getEndTime());
		
		if(newPlan.getEndTime() <= newPlan.getStartTime()){
			// Could not find a valid new plan
			return null;
		}
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan.getStartTime(), newPlan.getEndTime(), 1, newPlan.getStopsToBeServed(), new IdImpl(cooperative.getCurrentIteration())));

		return newPlan;
	}
	
	@Override
	public String getName() {
		return WeightedStartTimeExtension.STRATEGY_NAME;
	}
}
