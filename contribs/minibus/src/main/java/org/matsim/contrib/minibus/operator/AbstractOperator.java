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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants.OperatorState;
import org.matsim.contrib.minibus.performance.PTransitLineMerger;
import org.matsim.contrib.minibus.replanning.PStrategy;
import org.matsim.contrib.minibus.replanning.PStrategyManager;
import org.matsim.contrib.minibus.routeProvider.PRouteProvider;
import org.matsim.contrib.minibus.scoring.ScoreContainer;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

/**
 * Common implementation for all operators, except for replanning
 * 
 * @author aneumann
 *
 */
abstract class AbstractOperator implements Operator{
	
	final static Logger log = Logger.getLogger(AbstractOperator.class);
	
	final Id<Operator> id;
	
	private int numberOfPlansTried;
	
	private final PFranchise franchise;
	private final double costPerVehicleBuy;
	final double costPerVehicleSell;
	private final double costPerVehicleAndDay;
	private final double minOperationTime;
	private final boolean mergeTransitLine;
	
	OperatorState operatorState;

	PPlan bestPlan;
	PPlan testPlan;

	private TransitLine currentTransitLine;
	private int numberOfIterationsForProspecting;
	
	double budget;
	double score;
	double scoreLastIteration;
	int numberOfVehiclesInReserve;
	
	PRouteProvider routeProvider;
	int currentIteration;


	AbstractOperator(Id<Operator> id, PConfigGroup pConfig, PFranchise franchise){
		this.id = id;
		this.numberOfIterationsForProspecting = pConfig.getNumberOfIterationsForProspecting();
		this.costPerVehicleBuy = pConfig.getPricePerVehicleBought();
		this.costPerVehicleSell = pConfig.getPricePerVehicleSold();
		this.costPerVehicleAndDay = pConfig.getCostPerVehicleAndDay();
		this.minOperationTime = pConfig.getMinOperationTime();
		this.mergeTransitLine = pConfig.getMergeTransitLine();
		this.franchise = franchise;
	}

	@Override
	public boolean init(PRouteProvider pRouteProvider, PStrategy initialStrategy, int iteration, double initialBudget) {
		this.operatorState = OperatorState.PROSPECTING;
		this.budget = initialBudget;
		this.currentIteration = iteration;
		this.routeProvider = pRouteProvider;
		
		this.bestPlan = initialStrategy.run(this);
		if(this.bestPlan == null) {
			// failed to provide a plan, abort intitialization
			return false;
		}
		
		this.testPlan = null;
		this.numberOfPlansTried = 0;
		this.numberOfVehiclesInReserve = 0;
		
		// everything went fine
		return true;
	}

	@Override
	public void score(Map<Id<Vehicle>, ScoreContainer> driverId2ScoreMap) {
		this.scoreLastIteration = this.score;
		this.score = 0;
		
		// score all plans
		for (PPlan plan : this.getAllPlans()) {
			scorePlan(driverId2ScoreMap, plan);
			this.score += plan.getScore();
			for (TransitRoute route : plan.getLine().getRoutes().values()) {
				route.setDescription(plan.toString(this.budget + this.score));
			}
		}
		
		processScore();
	}
	
	protected void processScore() {
		// score all vehicles not associated with plans
		score -= this.numberOfVehiclesInReserve * this.costPerVehicleAndDay;
		
		if (this.score > 0.0) {
			this.operatorState = OperatorState.INBUSINESS;
		}
		
		if (this.operatorState.equals(OperatorState.PROSPECTING)) {
			if(this.numberOfIterationsForProspecting == 0){
				if (this.score < 0.0) {
					// no iterations for prospecting left and score still negative - terminate
					this.operatorState = OperatorState.BANKRUPT;
				}
			}
			this.numberOfIterationsForProspecting--;
		}

		this.budget += this.score;
		
		// check, if bankrupt
		if(this.budget < 0){
			// insufficient, sell vehicles
			int numberOfVehiclesToSell = -1 * Math.min(-1, (int) Math.floor(this.budget / this.costPerVehicleSell));
			
			int numberOfVehiclesOwned = this.getNumberOfVehiclesOwned();
			
			if(numberOfVehiclesOwned - numberOfVehiclesToSell < 1){
				// can not balance the budget by selling vehicles, bankrupt
				this.operatorState = OperatorState.BANKRUPT;
			}
		}
	}

	@Override
	abstract public void replan(PStrategyManager pStrategyManager, int iteration);
	
	@Override
	public Id<Operator> getId() {
		return this.id;
	}
	
	@Override
	public Id<PPlan> getNewPlanId() {
		Id<PPlan> planId = Id.create(this.currentIteration + "_" + numberOfPlansTried, PPlan.class);
		this.numberOfPlansTried++;
		return planId;
	}
	
	@Override
	public PFranchise getFranchise(){
		return this.franchise;
	}

	@Override
	public double getMinOperationTime() {
		return this.minOperationTime;
	}

	@Override
	public TransitLine getCurrentTransitLine() {
		if (this.currentTransitLine == null) {
			this.updateCurrentTransitLine();
		}
		
		if (this.mergeTransitLine) {
			this.currentTransitLine = PTransitLineMerger.mergeTransitLine(this.currentTransitLine);
		}
		
		return this.currentTransitLine;		
	}	

	@Override
	public PPlan getBestPlan() {
		return this.bestPlan;
	}

	@Override
	public List<PPlan> getAllPlans(){
		List<PPlan> plans = new LinkedList<>();
		if(this.bestPlan != null){
			plans.add(this.bestPlan);
		}
		if(this.testPlan != null){
			plans.add(this.testPlan);
		}		
		return plans;
	}
	
	@Override
	public double getBudget(){
		return this.budget;
	}

	@Override
	public int getNumberOfVehiclesOwned() {
		int numberOfVehicles = 0;			
		for (PPlan plan : this.getAllPlans()) {
			numberOfVehicles += plan.getNVehicles();
		}
		numberOfVehicles += this.numberOfVehiclesInReserve;
		return numberOfVehicles;
	}

	@Override
	public int getCurrentIteration() {
		return this.currentIteration;
	}

	@Override
	public PRouteProvider getRouteProvider() {
		return this.routeProvider;
	}

	double getCostPerVehicleBuy() {
		return costPerVehicleBuy;
	}

	public double getCostPerVehicleSell() {
		return costPerVehicleSell;
	}

	@Override
	public OperatorState getOperatorState() {
		return this.operatorState;
	}

	@Override
	public void setBudget(double budget) {
		this.budget = budget;
	}
	
	void updateCurrentTransitLine(){
		this.currentTransitLine = this.routeProvider.createEmptyLineFromOperator(id);
		for (PPlan plan : this.getAllPlans()) {
			for (TransitRoute route : plan.getLine().getRoutes().values()) {
				this.currentTransitLine.addRoute(route);
			}
		}
	}

	protected final void scorePlan(Map<Id<Vehicle>, ScoreContainer> driverId2ScoreMap, PPlan plan) {
		double totalLineScore = 0.0;
		int totalTripsServed = 0;
		
		for (Id<Vehicle> vehId : plan.getVehicleIds()) {
			totalLineScore += driverId2ScoreMap.get(vehId).getTotalRevenue();
			totalTripsServed += driverId2ScoreMap.get(vehId).getTripsServed();	
		}
		
		plan.setScore(totalLineScore);
		plan.setTripsServed(totalTripsServed);
	}
	
}