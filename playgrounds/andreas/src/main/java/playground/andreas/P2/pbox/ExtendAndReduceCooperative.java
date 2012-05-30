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
package playground.andreas.P2.pbox;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PConstants.CoopState;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.andreas.P2.replanning.PStrategyManager;
import playground.andreas.P2.replanning.modules.AggressiveIncreaseNumberOfVehicles;
import playground.andreas.P2.replanning.modules.ConvexHullRouteExtension;
import playground.andreas.P2.replanning.modules.MaxRandomEndTimeAllocator;
import playground.andreas.P2.replanning.modules.MaxRandomStartTimeAllocator;
import playground.andreas.P2.replanning.modules.RandomEndTimeAllocator;
import playground.andreas.P2.replanning.modules.RandomRouteEndExtension;
import playground.andreas.P2.replanning.modules.RandomRouteStartExtension;
import playground.andreas.P2.replanning.modules.RandomStartTimeAllocator;
import playground.andreas.P2.replanning.modules.RectangleHullRouteExtension;

/**
 * @author droeder
 *
 */
public class ExtendAndReduceCooperative extends AbstractCooperative{
	public static final String COOP_NAME = "ExtendAndReduceCooperative"; 

	private String lastStrategy = null;
	private static final Logger log = Logger
			.getLogger(ExtendAndReduceCooperative.class);
	
	
	/**
	 * @param id
	 * @param pConfig
	 * @param franchise
	 */
	public ExtendAndReduceCooperative(Id id, PConfigGroup pConfig, PFranchise franchise) {
		super(id, pConfig, franchise);
	}

	@Override
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
		
		// First buy vehicles
		PPlanStrategy strategy = new AggressiveIncreaseNumberOfVehicles(new ArrayList<String>());
		this.testPlan = strategy.run(this);
		if(!(this.testPlan == null)){
			this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() + this.testPlan.getNVehicles());
//			this.bestPlan.setLine(this.routeProvider.createTransitLine(
//					this.id, this.bestPlan.getStartTime(), 
//					this.bestPlan.getEndTime(), 
//					this.bestPlan.getNVehicles() + this.testPlan.getNVehicles(), 
//					this.bestPlan.getStopsToBeServed(), 
//					this.bestPlan.getId()));
			this.testPlan = null;
			
		}
		
		PPlanStrategy nextStrategy;
		if((this.lastStrategy == RandomRouteStartExtension.STRATEGY_NAME) ||
				(this.lastStrategy == RandomRouteEndExtension.STRATEGY_NAME) ||
				(this.lastStrategy == RectangleHullRouteExtension.STRATEGY_NAME)||
				(this.lastStrategy == ConvexHullRouteExtension.STRATEGY_NAME)){
			nextStrategy = pStrategyManager.getReduceStopsToBeServed();
		}
		else if((this.lastStrategy == RandomEndTimeAllocator.STRATEGY_NAME) ||
				(this.lastStrategy == RandomStartTimeAllocator.STRATEGY_NAME)|| 
				(this.lastStrategy == MaxRandomStartTimeAllocator.STRATEGY_NAME) ||
				(this.lastStrategy == MaxRandomEndTimeAllocator.STRATEGY_NAME)){
			nextStrategy = pStrategyManager.getReduceTimeServed();
		}else{
			nextStrategy = pStrategyManager.chooseStrategy();
		}
		
		this.lastStrategy = nextStrategy.getName();
		this.testPlan = nextStrategy.run(this);
		if (this.testPlan != null) {
			this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() - 1);
		}
		this.bestPlan.setLine(this.routeProvider.createTransitLine(
								this.id, this.bestPlan.getStartTime(), 
								this.bestPlan.getEndTime(), 
								this.bestPlan.getNVehicles(), 
								this.bestPlan.getStopsToBeServed(), 
								this.bestPlan.getId()));
		this.updateCurrentTransitLine();
	}
	
	

}
