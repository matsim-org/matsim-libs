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

package org.matsim.contrib.minibus.replanning;

import org.apache.log4j.Logger;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;

/**
 * 
 * Creates a completely new 24h plan.
 * 
 * @author aneumann
 *
 */
public final class CreateNew24hPlan extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(CreateNew24hPlan.class);
	private static final String STRATEGY_NAME = "CreateNew24hPlan";

	public CreateNew24hPlan(ArrayList<String> parameter) {
		super();
		if(parameter.size() != 0){
			log.error("Too many parameter. Will ignore: " + parameter);
		}
	}
	
	@Override
	public PPlan run(Operator operator) {
		PPlan newPlan;		
		
		do {
			double startTime = 0.0;
			double endTime = 24 * 3600.0;
			
			TransitStopFacility stop1 = operator.getRouteProvider().getRandomTransitStop(operator.getCurrentIteration());
			TransitStopFacility stop2 = operator.getRouteProvider().getRandomTransitStop(operator.getCurrentIteration());
			while(stop1.getId() == stop2.getId()){
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
		} while (operator.getFranchise().planRejected(newPlan));		

		return newPlan;
	}

	@Override
	public String getStrategyName() {
		return CreateNew24hPlan.STRATEGY_NAME;
	}

}