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

import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.andreas.P2.replanning.PStrategy;

/**
 * 
 * Increases the number of vehicles for the given cooperative's best plan, if the budget allows for that.
 * 
 * Buys as much vehicles as possible.
 * 
 * @author aneumann
 *
 */
public class AggressiveIncreaseNumberOfVehicles extends PStrategy implements PPlanStrategy{
	
	private final static Logger log = Logger.getLogger(AggressiveIncreaseNumberOfVehicles.class);
	
	public static final String STRATEGY_NAME = "AggressiveIncreaseNumberOfVehicles";
	
	public AggressiveIncreaseNumberOfVehicles(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("There are no parameters allowed for that module");
		}
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		
		int vehicleBought = 0;
		
		while (cooperative.getBudget() > cooperative.getCostPerVehicleBuy()) {
			// budget ok, buy one
			cooperative.setBudget(cooperative.getBudget() - cooperative.getCostPerVehicleBuy());
			vehicleBought++;
		}					
		
		if (vehicleBought == 0) {
			return null;
		}
			
		// vehicles were bought - create plan
		PPlan plan = new PPlan(cooperative.getBestPlan().getId(), this.getName(), cooperative.getBestPlan().getStopsToBeServed(), cooperative.getBestPlan().getStartTime(), cooperative.getBestPlan().getEndTime());
		plan.setScore(cooperative.getBestPlan().getScore());
		plan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), plan.getStartTime(), plan.getEndTime(), vehicleBought, plan.getStopsToBeServed(), new IdImpl(cooperative.getCurrentIteration())));
		return plan;
	}

	@Override
	public String getName() {
		return AggressiveIncreaseNumberOfVehicles.STRATEGY_NAME;
	}
}