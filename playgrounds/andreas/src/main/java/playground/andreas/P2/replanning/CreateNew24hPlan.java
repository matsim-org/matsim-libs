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

package playground.andreas.P2.replanning;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;

/**
 * 
 * Creates a completely new 24h plan.
 * 
 * @author aneumann
 *
 */
public class CreateNew24hPlan extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(CreateNew24hPlan.class);
	public static final String STRATEGY_NAME = "CreateNew24hPlan";

	public CreateNew24hPlan(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("Too many parameter. Will ignore: " + parameter);
		}
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		PPlan newPlan;		
		
		do {
			double startTime = 0.0;
			double endTime = 24 * 3600.0;
			
			TransitStopFacility stop1 = cooperative.getRouteProvider().getRandomTransitStop();
			TransitStopFacility stop2 = cooperative.getRouteProvider().getRandomTransitStop();
			while(stop1.getId() == stop2.getId()){
				stop2 = cooperative.getRouteProvider().getRandomTransitStop();
			}
			
			ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<TransitStopFacility>();
			stopsToBeServed.add(stop1);
			stopsToBeServed.add(stop2);
			
			newPlan = new PPlan(new IdImpl(cooperative.getCurrentIteration() + "-init"), stopsToBeServed, startTime, endTime); 
			
			newPlan.setLine(cooperative.getRouteProvider().createTransitLine(new IdImpl(cooperative.getId().toString() + "-init"), newPlan.getStartTime(), newPlan.getEndTime(), 1, stopsToBeServed, new IdImpl(cooperative.getCurrentIteration() + "-init")));
		} while (cooperative.getFranchise().planRejected(newPlan));		

		return newPlan;
	}

	@Override
	public String getName() {
		return CreateNew24hPlan.STRATEGY_NAME;
	}

}