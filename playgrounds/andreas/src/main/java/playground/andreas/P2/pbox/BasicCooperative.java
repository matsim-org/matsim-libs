/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;
import playground.andreas.P2.replanning.CreateNewPlan;
import playground.andreas.P2.replanning.PPlanStrategy;
import playground.andreas.P2.replanning.PStrategyManager;
import playground.andreas.P2.scoring.ScoreContainer;

/**
 * Manages one paratransit line
 * 
 * @author aneumann
 *
 */
public class BasicCooperative implements Cooperative{
	
	private final static Logger log = Logger.getLogger(BasicCooperative.class);
	
	private final Id id;
	
	private PFranchise franchise;
	private final double costPerVehicleBuy;
	private final double costPerVehicleSell;

	private PPlan bestPlan;
	private PPlan testPlan;

	private TransitLine currentTransitLine;
	private double budget;
	private double budgetLastIteration;
	private PRouteProvider routeProvider;
	private int currentIteration;

	public BasicCooperative(Id id, double costPerVehicle, PFranchise franchise){
		this.id = id;
		this.costPerVehicleBuy = costPerVehicle;
		this.costPerVehicleSell = 0.5 * this.costPerVehicleBuy; // TODO Why half the price?
		this.franchise = franchise;
	}

	public void init(PRouteProvider pRouteProvider, int iteration) {
		this.budget = 0.0;
		this.currentIteration = iteration;
		this.routeProvider = pRouteProvider;
	
		PPlanStrategy strategy = new CreateNewPlan(new ArrayList<String>());
		this.bestPlan = strategy.run(this);
		
		this.currentTransitLine = this.routeProvider.createEmptyLine(id);
		for (TransitRoute route : this.bestPlan.getLine().getRoutes().values()) {
			this.currentTransitLine.addRoute(route);
		}
		this.testPlan = null;
	}

	public void score(TreeMap<Id, ScoreContainer> driverId2ScoreMap) {
		this.budgetLastIteration = this.budget;
		for (PPlan plan : this.getAllPlans()) {
			scorePlan(driverId2ScoreMap, plan);
			this.budget += plan.getScore();
			for (TransitRoute route : plan.getLine().getRoutes().values()) {
				route.setDescription(plan.toString(this.budget));
			}
		}
	}

	public void replan(PStrategyManager pStrategyManager, int iteration) {	
		this.currentIteration = iteration;
		
		if(this.testPlan != null){
			// compare score per vehicles TODO This should be the score per vehicle from the last iteration
			if (this.testPlan.getScorePerVehicle() > this.bestPlan.getScorePerVehicle()){
				// testPlan scores better, apply its modification to bestPlan, transfer the vehicle from the testPlan to the bestPlan
				this.bestPlan.setStartStop(this.testPlan.getStartStop());
				this.bestPlan.setEndStop(this.testPlan.getEndStop());
				this.bestPlan.setStartTime(this.testPlan.getStartTime());
				this.bestPlan.setEndTime(this.testPlan.getEndTime());
			}
			this.bestPlan.setNVehicles(this.bestPlan.getNVehciles() + 1);
			this.testPlan = null;
		}
		
		// plan scored negative sell one vehicle, plan scored positive try to buy one
		if(this.budget < this.budgetLastIteration){
			if(this.bestPlan.getNVehciles() > 1){
				// can sell one vehicle
				this.budget += this.costPerVehicleSell * 1;
				this.bestPlan.setNVehicles(this.bestPlan.getNVehciles() - 1);
			}
		} else {
			// plan scored positive
			if(this.budget > this.costPerVehicleBuy){
				// budget ok, buy one
				this.budget -= this.costPerVehicleBuy * 1;
				this.bestPlan.setNVehicles(this.bestPlan.getNVehciles() + 1);
			}
		}

		// balance the budget
		if(this.budget < 0){
			// insufficient, sell vehicles
			int numberOfVehiclesToSell = -1 * Math.min(-1, (int) Math.floor(this.budget / this.costPerVehicleSell));
			
			if(this.bestPlan.getNVehciles() - numberOfVehiclesToSell < 1){
				// can not balance the budget by selling vehicles, bankrupt
				return;
			}

			// can balance the budget, so sell vehicles
			this.bestPlan.setNVehicles(this.bestPlan.getNVehciles() - numberOfVehiclesToSell);
			this.budget += this.costPerVehicleSell * numberOfVehiclesToSell;
			log.info("Sold " + numberOfVehiclesToSell + " vehicle from line " + this.id + " - new budget is " + this.budget);
		}
		
		if(this.budget < 0){
			log.error("There should be no inbalanced budget at this time.");
		}		

		if(this.bestPlan.getNVehciles() > 1){
			// can afford to use one vehicle for testing, get a new testPlan
			PPlanStrategy strategy = pStrategyManager.chooseStrategy();
			this.testPlan = strategy.run(this);
			this.bestPlan.setNVehicles(this.bestPlan.getNVehciles() - 1);
		}
		
		// reinitialize the plan
		this.bestPlan.setLine(this.routeProvider.createTransitLine(this.id, this.bestPlan.getStartTime(), this.bestPlan.getEndTime(), this.bestPlan.getNVehciles(), this.bestPlan.getStartStop(), this.bestPlan.getEndStop(), this.bestPlan.getId()));
		
		this.currentTransitLine = this.routeProvider.createEmptyLine(id);
		for (PPlan plan : this.getAllPlans()) {
			for (TransitRoute route : plan.getLine().getRoutes().values()) {
				this.currentTransitLine.addRoute(route);
			}
		}
	}
	
	public Id getId() {
		return this.id;
	}
	
	public PFranchise getFranchise(){
		return this.franchise;
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