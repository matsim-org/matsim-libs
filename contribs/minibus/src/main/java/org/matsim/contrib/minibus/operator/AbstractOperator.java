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
import org.matsim.contrib.minibus.PConfigGroup.LogRouteDesignVsTotalScore;
import org.matsim.contrib.minibus.PConstants.OperatorState;
import org.matsim.contrib.minibus.performance.PTransitLineMerger;
import org.matsim.contrib.minibus.replanning.PStrategy;
import org.matsim.contrib.minibus.replanning.PStrategyManager;
import org.matsim.contrib.minibus.routeProvider.PRouteProvider;
import org.matsim.contrib.minibus.scoring.PScoreContainer;
import org.matsim.contrib.minibus.scoring.routeDesignScoring.RouteDesignScoringManager;
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
	private double score;
	private double scoreLastIteration;
	int numberOfVehiclesInReserve;
	
	PRouteProvider routeProvider;
	int currentIteration;
	
	private final LogRouteDesignVsTotalScore logRouteDesignVsTotalScore;

	AbstractOperator(Id<Operator> id, PConfigGroup pConfig, PFranchise franchise){
		this.id = id;
		this.numberOfIterationsForProspecting = pConfig.getNumberOfIterationsForProspecting();
		this.costPerVehicleBuy = pConfig.getPricePerVehicleBought();
		this.costPerVehicleSell = pConfig.getPricePerVehicleSold();
		this.costPerVehicleAndDay = pConfig.getCostPerVehicleAndDay();
		this.minOperationTime = pConfig.getMinOperationTime();
		this.mergeTransitLine = pConfig.getMergeTransitLine();
		this.logRouteDesignVsTotalScore = pConfig.getLogLogRouteDesignVsTotalScore();
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
	public void score(Map<Id<Vehicle>, PScoreContainer> pScores, SubsidyI subsidy, 
			RouteDesignScoringManager routeDesignScoringManager) {
		this.setScoreLastIteration(this.getScore());
		this.setScore(0);
		
		// score all plans
		for (PPlan plan : this.getAllPlans()) {
			scorePlan(pScores, plan, routeDesignScoringManager);
			
			if (subsidy != null) {
				Id<PPlan> pplanId = Id.create(plan.getLine().getId().toString() + "-" + plan.getId().toString(), PPlan.class);
				double subsidyAmount = subsidy.getSubsidy(pplanId);
				double newPlanScore = subsidyAmount + plan.getScore();
				plan.setScore(newPlanScore);
			}
			
			this.setScore(this.getScore() + plan.getScore());

			for (TransitRoute route : plan.getLine().getRoutes().values()) {
				route.setDescription(plan.toString(this.budget + this.getScore()));
			}
		}
		
		processScore();
	}
	
	void processScore() {
		// score all vehicles not associated with plans
		setScore(getScore() - this.numberOfVehiclesInReserve * this.costPerVehicleAndDay);
		
		if (this.getScore() > 0.0) {
			this.operatorState = OperatorState.INBUSINESS;
		}
		
		if (this.operatorState.equals(OperatorState.PROSPECTING)) {
			if(this.numberOfIterationsForProspecting == 0){
				if (this.getScore() < 0.0) {
					// no iterations for prospecting left and score still negative - terminate
					this.operatorState = OperatorState.BANKRUPT;
				}
			}
			this.numberOfIterationsForProspecting--;
		}

		this.budget += this.getScore();
		
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

	private final void scorePlan(Map<Id<Vehicle>, PScoreContainer> driverId2ScoreMap, PPlan plan, RouteDesignScoringManager routeDesignScoringManager) {
		double totalLineScore = 0.0;
		int totalTripsServed = 0;

		for (Id<Vehicle> vehId : plan.getVehicleIds()) {
			totalLineScore += driverId2ScoreMap.get(vehId).getTotalRevenue();
			totalTripsServed += driverId2ScoreMap.get(vehId).getTripsServed();
		}

		totalLineScore = capAndAddRouteDesignScore (plan, totalLineScore, routeDesignScoringManager);

		plan.setScore(totalLineScore);
		plan.setTripsServed(totalTripsServed);
	}
	
	private double capAndAddRouteDesignScore (PPlan plan, double originaltotalLineScore, RouteDesignScoringManager routeDesignScoringManager) {
		double totalLineScore = originaltotalLineScore;
		
		if (routeDesignScoringManager.isActive()) {
			
			double routeDesignScore = routeDesignScoringManager.scoreRouteDesign(plan);
			
			if (routeDesignScore != 0) {
				/*
				 * Cap route design score, if score is bad enough to eliminate the plan, so it
				 * would affect other plans less. Don't cap if a plan performs really bad
				 * financially besides non-monetary metrics like route design scoring. At
				 * minScoreToSurvive CarefulMultiPlanOperator would leave at least one vehicle
				 * on the plan, so plan could survive
				 */
				
				double minScoreToSurvive = -costPerVehicleSell * plan.getNVehicles();
				if (originaltotalLineScore >= minScoreToSurvive) {
					/*
					 * Limit negative score to the amount at which the plan is eliminated. Multiply
					 * route design score with number of vehicles, so route design score still
					 * has an influence on busy lines.
					 */
					totalLineScore = Math.max(originaltotalLineScore + routeDesignScore * plan.getNVehicles(),
							minScoreToSurvive - 0.0001 * costPerVehicleSell);
				} else {
					// plan already has a bad score that will cause its elimination, don't cap this
					// monetary cost
				}
				
				if (logRouteDesignVsTotalScore.equals(LogRouteDesignVsTotalScore.onlyNonZeroRouteDesignScore)) {
					log.info("operator " + id + ". line " + plan.getLine().getId() + ". TransitRoute(s) "
							+ plan.getLine().getRoutes().values().iterator().next().getId()
							+ ". original totalLineScore: " + originaltotalLineScore + ". routeDesignScore: "
							+ routeDesignScore + ". minScoreToSurvive: " + minScoreToSurvive
							+ ". totalLineScore after: " + totalLineScore);
				}
			}
		}
		
		return totalLineScore;
	}
	
	double getScore() {
		return score;
	}

	void setScore(double score) {
		this.score = score;
	}

	double getScoreLastIteration() {
		return scoreLastIteration;
	}

	void setScoreLastIteration(double scoreLastIteration) {
		this.scoreLastIteration = scoreLastIteration;
	}
	
}