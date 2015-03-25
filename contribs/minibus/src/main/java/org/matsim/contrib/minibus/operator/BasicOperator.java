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

package org.matsim.contrib.minibus.operator;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants.OperatorState;
import org.matsim.contrib.minibus.replanning.PStrategy;
import org.matsim.contrib.minibus.replanning.PStrategyManager;

/**
 * Operator that manages one paratransit line
 * 
 * @author aneumann
 *
 */
public final class BasicOperator extends AbstractOperator{
	
	public static final String OPERATOR_NAME = "BasicOperator"; 

	public BasicOperator(Id<Operator> id, PConfigGroup pConfig, PFranchise franchise){
		super(id, pConfig, franchise);
	}

	@Override
	public void replan(PStrategyManager pStrategyManager, int iteration) {
		this.currentIteration = iteration;
		
		if(this.testPlan != null){
			// compare scores
			if (this.score > this.scoreLastIteration){
				// testPlan improves the plan, apply its modification to bestPlan, transfer the vehicle from the testPlan to the bestPlan
				// changed to get a more useful output in the pOperatorLogger /dr
				this.testPlan.setNVehicles(this.testPlan.getNVehicles() + this.bestPlan.getNVehicles());
				this.bestPlan = this.testPlan;
			}else{
				this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() + this.testPlan.getNVehicles());
			}
			this.testPlan = null;
		}
		
		// balance the budget
		if(this.budget < 0){
			// insufficient, sell vehicles
			int numberOfVehiclesToSell = -1 * Math.min(-1, (int) Math.floor(this.budget / this.costPerVehicleSell));
			
			if(this.bestPlan.getNVehicles() - numberOfVehiclesToSell < 1){
				// can not balance the budget by selling vehicles, bankrupt
				log.error("This should not be possible at this time.");
				this.operatorState = OperatorState.BANKRUPT;
				return;
			}

			// can balance the budget, so sell vehicles
			this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() - numberOfVehiclesToSell);
			this.budget += this.costPerVehicleSell * numberOfVehiclesToSell;
//			log.info("Sold " + numberOfVehiclesToSell + " vehicle from line " + this.id + " - new budget is " + this.budget);
		}

		// THIS IS A COPY FROM org.matsim.contrib.minibus.replanning.modules.deprecated.AggressiveIncreaseNumberOfVehicles;
		// First buy vehicles
		int vehicleBought = 0;
		
		while (this.getBudget() > this.getCostPerVehicleBuy()) {
			// budget ok, buy one
			this.setBudget(this.getBudget() - this.getCostPerVehicleBuy());
			vehicleBought++;
		}					
		
		if (vehicleBought == 0) {
			this.testPlan = null;
		}
			
		// vehicles were bought - create plan
		PPlan oldPlan = this.getBestPlan();
		PPlan plan = new PPlan(this.getNewPlanId(), "Copied code from AggressiveIncreaseNumberOfVehicles", oldPlan.getId());
		plan.setStopsToBeServed(oldPlan.getStopsToBeServed());
		plan.setStartTime(oldPlan.getStartTime());
		plan.setEndTime(oldPlan.getEndTime());
		plan.setScore(this.getBestPlan().getScore());
		
		plan.setNVehicles(vehicleBought);
		
		plan.setLine(this.getRouteProvider().createTransitLineFromOperatorPlan(this.getId(), plan));
		
		this.testPlan = plan;
		// END OF COPY
		
		
		// Second replan, if testplan null
		if (this.testPlan == null) {
			PStrategy strategy = pStrategyManager.chooseStrategy();
			if (strategy != null) {
				this.testPlan = strategy.run(this);
				if (this.testPlan != null) {
					this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() - 1);
				}
			}
		}
		
		// reinitialize the plan
		this.bestPlan.setLine(this.routeProvider.createTransitLineFromOperatorPlan(this.id, this.bestPlan));
		
		this.updateCurrentTransitLine();
	}
	
}