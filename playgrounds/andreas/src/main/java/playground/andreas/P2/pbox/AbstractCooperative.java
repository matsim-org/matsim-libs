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

package playground.andreas.P2.pbox;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PConstants.CoopState;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.andreas.P2.replanning.PStrategyManager;
import playground.andreas.P2.scoring.ScoreContainer;

/**
 * Common implementation for all cooperatives, except for replanning
 * 
 * @author aneumann
 *
 */
public abstract class AbstractCooperative implements Cooperative{
	
	protected final static Logger log = Logger.getLogger(AbstractCooperative.class);
	
	protected final Id id;
	
	private PFranchise franchise;
	private final double costPerVehicleBuy;
	protected final double costPerVehicleSell;
	private final double minOperationTime;
	
	protected CoopState coopState;

	protected PPlan bestPlan;
	protected PPlan testPlan;

	protected TransitLine currentTransitLine;
	private int numberOfIterationsForProspecting;
	
	protected double budget;
	protected double score;
	protected double scoreLastIteration;
	
	protected PRouteProvider routeProvider;
	protected int currentIteration;

	public AbstractCooperative(Id id, PConfigGroup pConfig, PFranchise franchise){
		this.id = id;
		this.numberOfIterationsForProspecting = pConfig.getNumberOfIterationsForProspecting();
		this.costPerVehicleBuy = pConfig.getPricePerVehicleBought();
		this.costPerVehicleSell = pConfig.getPricePerVehicleSold();
		this.minOperationTime = pConfig.getMinOperationTime();
		this.franchise = franchise;
	}

	public void init(PRouteProvider pRouteProvider, PPlanStrategy initialStrategy, int iteration, double initialBudget) {
		this.coopState = CoopState.PROSPECTING;
		this.budget = initialBudget;
		this.currentIteration = iteration;
		this.routeProvider = pRouteProvider;
		this.bestPlan = initialStrategy.run(this);
		this.testPlan = null;
	}

	public void score(TreeMap<Id, ScoreContainer> driverId2ScoreMap) {
		this.scoreLastIteration = this.score;
		this.score = 0;
		
		for (PPlan plan : this.getAllPlans()) {
			scorePlan(driverId2ScoreMap, plan);
			this.score += plan.getScore();
			for (TransitRoute route : plan.getLine().getRoutes().values()) {
				route.setDescription(plan.toString(this.budget + this.score));
			}
		}
		
		if (this.score > 0.0) {
			this.coopState = CoopState.INBUSINESS;
		}
		
		if (this.coopState.equals(CoopState.PROSPECTING)) {
			if(this.numberOfIterationsForProspecting == 0){
				if (this.score < 0.0) {
					// no iterations for prospecting left and score still negative - terminate
					this.coopState = CoopState.BANKRUPT;
				}
			}
			this.numberOfIterationsForProspecting--;
		}

		this.budget += this.score;
		
		
		// check, if bankrupt
		if(this.budget < 0){
			// insufficient, sell vehicles
			int numberOfVehiclesToSell = -1 * Math.min(-1, (int) Math.floor(this.budget / this.costPerVehicleSell));
			
			double numberOfVehicles = 0.0;			
			for (PPlan plan : this.getAllPlans()) {
				numberOfVehicles += plan.getNVehicles();
			}
			
			if(numberOfVehicles - numberOfVehiclesToSell < 1){
				// can not balance the budget by selling vehicles, bankrupt
				this.coopState = CoopState.BANKRUPT;
			}
		}
	}
	
	abstract public void replan(PStrategyManager pStrategyManager, int iteration);
	
	public Id getId() {
		return this.id;
	}
	
	public PFranchise getFranchise(){
		return this.franchise;
	}

	@Override
	public double getMinOperationTime() {
		return this.minOperationTime;
	}

	public TransitLine getCurrentTransitLine() {		
		return this.currentTransitLine;		
	}	

	public PPlan getBestPlan() {
		return this.bestPlan;
	}

	public List<PPlan> getAllPlans(){
		List<PPlan> plans = new LinkedList<PPlan>();
		if(this.bestPlan != null){
			plans.add(this.bestPlan);
		}
		if(this.testPlan != null){
			plans.add(this.testPlan);
		}		
		return plans;
	}
	
	public double getBudget(){
		return this.budget;
	}

	public int getCurrentIteration() {
		return this.currentIteration;
	}

	public PRouteProvider getRouteProvider() {
		return this.routeProvider;
	}

	public double getCostPerVehicleBuy() {
		return costPerVehicleBuy;
	}

	public double getCostPerVehicleSell() {
		return costPerVehicleSell;
	}

	@Override
	public CoopState getCoopState() {
		return this.coopState;
	}

	public void setBudget(double budget) {
		this.budget = budget;
	}

	private void scorePlan(TreeMap<Id, ScoreContainer> driverId2ScoreMap, PPlan plan) {
		double totalLineScore = 0.0;
		int totalTripsServed = 0;
		
		for (Id vehId : plan.getVehicleIds()) {
			totalLineScore += driverId2ScoreMap.get(vehId).getTotalRevenue();
			totalTripsServed += driverId2ScoreMap.get(vehId).getTripsServed();
		}
		
		plan.setScore(totalLineScore);
		plan.setTripsServed(totalTripsServed);
	}
}