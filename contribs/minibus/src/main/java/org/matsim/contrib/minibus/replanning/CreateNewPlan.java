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
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.contrib.minibus.operator.TimeProvider;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;

/**
 * 
 * Creates a new plan from scratch. New stops have a minimal distance of twice the grid size. 
 * 
 * @author aneumann
 *
 */
public final class CreateNewPlan extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(CreateNewPlan.class);
	public static final String STRATEGY_NAME = "CreateNewPlan";

    private final double timeSlotSize;
	private final double minInitialStopDistance;
	private TimeProvider timeProvider;

	public CreateNewPlan(ArrayList<String> parameter) {
		super();
		if(parameter.size() != 2){
			log.error("Wrong number of parameters. Will ignore: " + parameter);
			log.error("Parameter 1: Time slot size for new start and end times");
			log.error("Parameter 2: Min distance of the two initial stops");
		}
		
		this.timeSlotSize = Double.parseDouble(parameter.get(0));
		this.minInitialStopDistance = Double.parseDouble(parameter.get(1));
	}
	
	public void setTimeProvider(TimeProvider timeProvider){
		this.timeProvider = timeProvider;
	}
	
	@Override
	public PPlan run(Operator operator) {
		PPlan newPlan;		
		
		int triesPerformed = 0;

        int maxNumberOfTries = 100;
        do {
			triesPerformed++;
			
			// get a valid start time
			double startTime = this.timeProvider.getRandomTimeInInterval(0 * 3600.0, 24 * 3600.0 - operator.getMinOperationTime());
			double endTime = this.timeProvider.getRandomTimeInInterval(startTime + operator.getMinOperationTime(), 24 * 3600.0);
			
			if (startTime == endTime) {
				endTime += this.timeSlotSize;
			}
			
			while (startTime + operator.getMinOperationTime() > endTime) {
				endTime += this.timeSlotSize;
			}
			
			if (startTime + operator.getMinOperationTime() > endTime) {
				log.warn("Already increased the time of operation by one time slot in order to meet the minimum time of operation criteria of " + operator.getMinOperationTime());
				log.warn("Start time is: " + startTime);
				log.warn("End time is: " + endTime);
				log.warn("Will continue anyway...");
			}
			
			TransitStopFacility stop1 = operator.getRouteProvider().getRandomTransitStop(operator.getCurrentIteration());
			TransitStopFacility stop2 = operator.getRouteProvider().getRandomTransitStop(operator.getCurrentIteration());

			while (CoordUtils.calcEuclideanDistance(stop1.getCoord(), stop2.getCoord()) < this.minInitialStopDistance) {
				stop2 = operator.getRouteProvider().getRandomTransitStop(operator.getCurrentIteration());
			}
			
			ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<>();
			stopsToBeServed.add(stop1);
			stopsToBeServed.add(stop2);
			
			newPlan = new PPlan(operator.getNewPlanId(), this.getStrategyName(), PConstants.founderPlanId);
			newPlan.setStopsToBeServed(stopsToBeServed);
			newPlan.setStartTime(startTime);
			newPlan.setEndTime(endTime);
			newPlan.setNVehicles(1);
			newPlan.setStopsToBeServed(stopsToBeServed);
			
			newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));

		} while (operator.getFranchise().planRejected(newPlan) && triesPerformed < maxNumberOfTries);

		if(!(triesPerformed < maxNumberOfTries)){
			log.warn("Exceeded the maximum number of tries (" + maxNumberOfTries + ") to find a new plan for operator " + operator.getId() + ". Returning null");
			return null;
		}
		
		return newPlan;
	}

	@Override
	public String getStrategyName() {
		return CreateNewPlan.STRATEGY_NAME;
	}

}