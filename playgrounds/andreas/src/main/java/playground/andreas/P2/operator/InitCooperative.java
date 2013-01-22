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

package playground.andreas.P2.operator;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PConstants.CoopState;
import playground.andreas.P2.pbox.PFranchise;
import playground.andreas.P2.replanning.PStrategy;
import playground.andreas.P2.replanning.PStrategyManager;
import playground.andreas.P2.replanning.modules.deprecated.AggressiveIncreaseNumberOfVehicles;

/**
 * Manages one paratransit line
 * 
 * @author aneumann
 *
 */
public class InitCooperative extends AbstractCooperative{
	
	private final static Logger log = Logger.getLogger(InitCooperative.class);
	public static final String COOP_NAME = "InitCooperative"; 
	
	boolean firstIteration = true;
	boolean needToReduceRoute = true;
	boolean needToReduceTime = true;

	public InitCooperative(Id id, PConfigGroup pConfig, PFranchise franchise){
		super(id, pConfig, franchise);
	}

	public void replan(PStrategyManager pStrategyManager, int iteration) {	
		this.currentIteration = iteration;
		
		if(this.testPlan != null){
			// compare scores
			if (this.score > this.scoreLastIteration){
				// testPlan improves the plan, apply its modification to bestPlan, transfer the vehicle from the testPlan to the bestPlan
				this.bestPlan.setStopsToBeServed(this.testPlan.getStopsToBeServed());
				this.bestPlan.setStartTime(this.testPlan.getStartTime());
				this.bestPlan.setEndTime(this.testPlan.getEndTime());
			}
			this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() + this.testPlan.getNVehicles());
			this.testPlan = null;
		}
		
		// balance the budget
		if(this.budget < 0){
			// insufficient, sell vehicles
			int numberOfVehiclesToSell = -1 * Math.min(-1, (int) Math.floor(this.budget / this.costPerVehicleSell));
			
			if(this.bestPlan.getNVehicles() - numberOfVehiclesToSell < 1){
				// can not balance the budget by selling vehicles, bankrupt
				log.error("This should not be possible at this time.");
				this.coopState = CoopState.BANKRUPT;
				return;
			}

			// can balance the budget, so sell vehicles
			this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() - numberOfVehiclesToSell);
			this.budget += this.costPerVehicleSell * numberOfVehiclesToSell;
//			log.info("Sold " + numberOfVehiclesToSell + " vehicle from line " + this.id + " - new budget is " + this.budget);
		}
		
		if (firstIteration) {
			PStrategy strategy = new AggressiveIncreaseNumberOfVehicles(new ArrayList<String>());
			this.testPlan = strategy.run(this);
			this.firstIteration = false;
		} else if (this.needToReduceRoute) {
			PStrategy strategy = pStrategyManager.getReduceStopsToBeServed();
			this.testPlan = strategy.run(this);
			if (this.testPlan != null) {
				this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() - 1);
			}
			this.needToReduceRoute = false;
		} else if (this.needToReduceTime) {
			PStrategy strategy = pStrategyManager.getReduceTimeServed();
			this.testPlan = strategy.run(this);
			if (this.testPlan != null) {
				this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() - 1);
			}
			this.needToReduceTime = false;
		} else {
		
			// First buy vehicles
			PStrategy strategy = new AggressiveIncreaseNumberOfVehicles(new ArrayList<String>());
			this.testPlan = strategy.run(this);

			// Second replan, if testplan null
			if (this.testPlan == null) {
				strategy = pStrategyManager.chooseStrategy();
				if (strategy != null) {
					this.testPlan = strategy.run(this);
					if (this.testPlan != null) {
						this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() - 1);
					}
				}
			}
		}
		
		// reinitialize the plan
		this.bestPlan.setLine(this.routeProvider.createTransitLine(this.id, this.bestPlan));
		
		this.updateCurrentTransitLine();
	}
	
}