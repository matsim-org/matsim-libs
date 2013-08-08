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

package playground.andreas.P2.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.operator.Cooperative;

/**
 * 
 * Creates a new plan from scratch. New stops have a minimal distance of twice the grid size. 
 * 
 * @author aneumann
 *
 */
public class CreateNewPlan extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(CreateNewPlan.class);
	public static final String STRATEGY_NAME = "CreateNewPlan";
	
	private final int maxNumberOfTries = 100;

	private final double timeSlotSize;
	private final double minInitialStopDistance;
	private TimeProvider timeProvider;

	public CreateNewPlan(ArrayList<String> parameter) {
		super(parameter);
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
	public PPlan run(Cooperative cooperative) {
		PPlan newPlan;		
		
		int triesPerformed = 0;
		
		do {
			triesPerformed++;
			
			// get a valid start time
			double startTime = this.timeProvider.getRandomTimeInInterval(0 * 3600.0, 24 * 3600.0 - cooperative.getMinOperationTime());
			double endTime = this.timeProvider.getRandomTimeInInterval(startTime + cooperative.getMinOperationTime(), 24 * 3600.0);
			
//			double startTime = MatsimRandom.getRandom().nextDouble() * (24.0 * 3600.0 - cooperative.getMinOperationTime());
//			startTime = TimeProvider.getSlotForTime(startTime, this.timeSlotSize) * this.timeSlotSize;
//			
//			double endTime = startTime + cooperative.getMinOperationTime() + MatsimRandom.getRandom().nextDouble() * (24.0 * 3600.0 - cooperative.getMinOperationTime() - startTime);
//			endTime = TimeProvider.getSlotForTime(endTime, this.timeSlotSize) * this.timeSlotSize;
			
			if (startTime == endTime) {
				endTime += this.timeSlotSize;
			}
			
			while (startTime + cooperative.getMinOperationTime() > endTime) {
				endTime += this.timeSlotSize;
			}
			
			if (startTime + cooperative.getMinOperationTime() > endTime) {
				log.warn("Already increased the time of operation by one time slot in order to meet the minimum time of operation criteria of " + cooperative.getMinOperationTime());
				log.warn("Start time is: " + startTime);
				log.warn("End time is: " + endTime);
				log.warn("Will continue anyway...");
			}
			
			TransitStopFacility stop1 = cooperative.getRouteProvider().getRandomTransitStop(cooperative.getCurrentIteration());
			TransitStopFacility stop2 = cooperative.getRouteProvider().getRandomTransitStop(cooperative.getCurrentIteration());

			while (CoordUtils.calcDistance(stop1.getCoord(), stop2.getCoord()) < this.minInitialStopDistance) {
				stop2 = cooperative.getRouteProvider().getRandomTransitStop(cooperative.getCurrentIteration());
			}
			
			ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<TransitStopFacility>();
			stopsToBeServed.add(stop1);
			stopsToBeServed.add(stop2);
			
			newPlan = new PPlan(cooperative.getNewRouteId(), this.getName());
			newPlan.setStopsToBeServed(stopsToBeServed);
			newPlan.setStartTime(startTime);
			newPlan.setEndTime(endTime);
			newPlan.setNVehicles(1);
			newPlan.setStopsToBeServed(stopsToBeServed);
			
			newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));

		} while (cooperative.getFranchise().planRejected(newPlan) && triesPerformed < this.maxNumberOfTries);		

		if(!(triesPerformed < this.maxNumberOfTries)){
			log.warn("Exceeded the maximum number of tries (" + this.maxNumberOfTries + ") to find a new plan for operator " + cooperative.getId() + ". Returning null");
			return null;
		}
		
		return newPlan;
	}

	@Override
	public String getName() {
		return CreateNewPlan.STRATEGY_NAME;
	}

}