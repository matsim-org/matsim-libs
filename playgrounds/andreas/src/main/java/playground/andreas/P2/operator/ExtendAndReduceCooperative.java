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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PConstants.CoopState;
import playground.andreas.P2.pbox.PFranchise;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.PStrategy;
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
import playground.andreas.P2.replanning.modules.RouteEnvelopeExtension;

/**
 * @author droeder
 *
 */
public class ExtendAndReduceCooperative extends AbstractCooperative{
	public static final String COOP_NAME = "ExtendAndReduceCooperative"; 

	private String lastStrategy = null;
	private static final Logger log = Logger
			.getLogger(ExtendAndReduceCooperative.class);
	
	private final List<String> reduceStopsAfter = new ArrayList<String>(){{
		add(RandomRouteStartExtension.STRATEGY_NAME);
		add(RandomRouteEndExtension.STRATEGY_NAME);
		add(RectangleHullRouteExtension.STRATEGY_NAME);
		add(ConvexHullRouteExtension.STRATEGY_NAME);
		add(RouteEnvelopeExtension.STRATEGY_NAME);
	}};
	
	private final List<String> reduceTimeAfter = new ArrayList<String>(){{
		add(RandomEndTimeAllocator.STRATEGY_NAME);
		add(RandomStartTimeAllocator.STRATEGY_NAME); 
		add(MaxRandomStartTimeAllocator.STRATEGY_NAME);
		add(MaxRandomEndTimeAllocator.STRATEGY_NAME);
	}};
	

	private boolean inProgress;
	
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
		// balance the budget first
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
		
		// always buy vehicles without test
		PStrategy strategy = new AggressiveIncreaseNumberOfVehicles(new ArrayList<String>());
		PPlan buy = strategy.run(this);
		if(!(buy == null)){
			this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() + buy.getNVehicles());
		}
		// check if currently replanning is in progress, due to the fixed strategy-order of this cooperative
		if(this.inProgress){
			this.replanInProgress(pStrategyManager, iteration);
		}else{
			this.randomReplan(pStrategyManager, iteration);
		}
		// update transit-lines anyway
		this.updateCurrentTransitLine();
	}

	/**
	 * @param pStrategyManager
	 * @param iteration
	 */
	private void randomReplan(PStrategyManager pStrategyManager, int iteration) {
		PStrategy s = pStrategyManager.chooseStrategy();
		PPlan p = s.run(this);
		
		if(!(p == null)){
			if(this.reduceStopsAfter.contains(s.getName()) || this.reduceTimeAfter.contains(s.getName())){
				this.lastStrategy = s.getName();
				this.inProgress = true;
			}
			this.testPlan = p;
			this.testPlan.setNVehicles(1);
			this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() -1);
		}
	}

	private double lastScore;
	
	/**
	 * @param pStrategyManager
	 * @param iteration
	 */
	private void replanInProgress(PStrategyManager pStrategyManager, int iteration) {
		PStrategy s;
		if(this.reduceStopsAfter.contains(this.lastStrategy)){
			this.lastScore = this.bestPlan.getScorePerVehicle();
			s = pStrategyManager.getReduceStopsToBeServed();
			this.lastStrategy = null;
		}else if(this.reduceTimeAfter.contains(this.lastStrategy)){
			this.lastScore = this.bestPlan.getScorePerVehicle();
			s = pStrategyManager.getReduceTimeServed();
			this.lastStrategy = null;
		}else{
			// accept the new plan if the score per Vehicle is better
			if(this.lastScore < this.testPlan.getScorePerVehicle()){
				this.testPlan.setNVehicles(this.testPlan.getNVehicles() + this.bestPlan.getNVehicles());
				this.bestPlan = this.testPlan;
			}
			// start replanning from beginning
			this.lastStrategy = null;
			this.inProgress = false;
			this.testPlan = null;
			// start the process again
			this.randomReplan(pStrategyManager, iteration);
			return;
		}
		//store best-plan temporally and set best-plan to test-plan
		PPlan temp = this.bestPlan;
		this.bestPlan = this.testPlan;
		// increase number of Vehicles of former test-plan so replanning is possible. remove the vehicle again later
		this.bestPlan.setNVehicles(this.bestPlan.getNVehicles() + 1);
		PPlan newPlan = s.run(this);
		// set the bestPlan again
		this.bestPlan = temp;
		// set the new testplan
		if(!(newPlan == null)){
			newPlan.setNVehicles(1);
			this.testPlan = newPlan;
		}else{
			// the new plan is null, so the replanning-Process ends here. But maybe
			// the testplan from the earlier steps is better. So keep the better plan...
			if(this.bestPlan.getScorePerVehicle() < this.testPlan.getScorePerVehicle()){
				this.testPlan.setNVehicles(this.testPlan.getNVehicles() + this.bestPlan.getNVehicles());
				this.bestPlan = this.testPlan;
			}
			this.inProgress = false;
			this.testPlan = null;
		}
	}
	

}
